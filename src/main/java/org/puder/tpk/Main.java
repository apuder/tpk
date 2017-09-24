package org.puder.tpk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

public class Main {

    public static void main(String[] args) throws ConfigurationException, IOException, URISyntaxException {
        Enumeration<URL> apps = ClassLoader.getSystemClassLoader().getResources(Config.APPS_FOLDER);
        File appsFolder = new File(apps.nextElement().toURI());
        File[] files = appsFolder.listFiles();
        if (files != null) {
            for (File appFolder : files) {
                convertToJSON(appFolder);
            }
        }
    }

    private static void convertToJSON(File appFolder) throws ConfigurationException, IOException {
        PropertyWrapper info = PropertyWrapper.fromFile(appFolder);

        RpkData rpkData = new RpkData();
        rpkData.app.id = info.getProperty("app.id");
        rpkData.app.version = info.getProperty("app.version");
        rpkData.app.name = info.getProperty("app.name");
        rpkData.app.description = info.getProperty("app.description");
        rpkData.app.author = info.getProperty("app.author");
        rpkData.app.year_published = info.getProperty("app.year_published");
        rpkData.app.categories = info.getProperty("app.categories");
        rpkData.app.platform = info.getProperty("app.platform");
        rpkData.app.screenshot = info.getMediaImages("app.screenshot");
        rpkData.publisher.first_name = info.getProperty("publisher.first_name");
        rpkData.publisher.last_name = info.getProperty("publisher.last_name");
        rpkData.publisher.email = info.getProperty("publisher.email");
        rpkData.trs.model = info.getProperty("trs.model");
        rpkData.trs.image.disk = info.getMediaImages("trs.image.disk");
        rpkData.trs.image.cmd = info.getMediaImages("trs.image.cmd")[0];
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        saveJSON(appFolder, gson.toJson(rpkData));
    }

    private static void saveJSON(File appFolder, String json) throws IOException {
        File dir = new File(Config.OUTPUT_FOLDER);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Cannot create output directory: " + dir);
        }
        String appName = FilenameUtils.getBaseName(appFolder.getCanonicalPath());
        try (FileWriter out = new FileWriter(new File(dir, appName + ".json"))) {
            out.write(json);
        }
    }

    private static final class PropertyWrapper {
        private final PropertiesConfiguration config;
        private final File baseDir;

        static PropertyWrapper fromFile(File baseDir) throws ConfigurationException {
            if (!baseDir.isDirectory()) {
                throw new IllegalArgumentException("'baseDir' must be a directory.");
            }
            File propertyFile = new File(baseDir, "info.properties");
            PropertiesConfiguration config = new PropertiesConfiguration();
            config.setDelimiterParsingDisabled(true);
            config.setFile(propertyFile);
            config.load();
            return new PropertyWrapper(config, baseDir);
        }

        private PropertyWrapper(PropertiesConfiguration config, File baseDir) {
            this.config = config;
            this.baseDir = baseDir;
        }

        String getProperty(String property) {
            return String.join(" ", this.config.getStringArray(property));
        }

        private RpkData.MediaImage[] getMediaImages(String property) {
            List<RpkData.MediaImage> images = new ArrayList<>();
            for (String filename : config.getStringArray(property)) {
                images.add(createMediaImage(new File(baseDir, filename)));
            }
            return images.toArray(new RpkData.MediaImage[0]);
        }

        private static RpkData.MediaImage createMediaImage(File path) {
            try (InputStream is = new FileInputStream(path)){
                RpkData.MediaImage mediaImage = new RpkData.MediaImage();
                mediaImage.content = Base64.getEncoder().encodeToString(IOUtils.toByteArray(is));
                mediaImage.ext = FilenameUtils.getExtension(path.getName());
                return mediaImage;
            } catch (IOException e) {
                throw new RuntimeException("Cannot create MediaImage!", e);
            }
        }
    }
}