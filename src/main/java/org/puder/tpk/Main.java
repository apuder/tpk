package org.puder.tpk;

import net.iharder.Base64;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

public class Main {

    public static void main(String[] args) {
        try {
            iterateApps();
        } catch (IOException | URISyntaxException | ConfigurationException e) {
            e.printStackTrace();
        }
    }

    private static void iterateApps() throws IOException, URISyntaxException, ConfigurationException {
        Enumeration<URL> apps = ClassLoader.getSystemClassLoader().getResources(Config.APPS_FOLDER);
        File appsFolder = new File(apps.nextElement().toURI());
        for (File appFolder : appsFolder.listFiles()) {
            convertToJSON(appFolder);
        }
    }

    private static void convertToJSON(File appFolder) throws ConfigurationException, IOException {
        String path = appFolder.getCanonicalPath();
        File propertyFile = new File(path + File.separator + "info.properties");
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.setDelimiterParsingDisabled(true);
        config.setFile(propertyFile);
        config.load();

        JSONObject app = new JSONObject();

        convertProperty(app, config, "app.description");
        convertProperty(app, config, "submitter.name");
        convertProperty(app, config, "submitter.email");
        convertProperty(app, config, "app.id");
        convertProperty(app, config, "app.name");
        convertProperty(app, config, "app.categories");
        convertProperty(app, config, "app.version");
        convertProperty(app, config, "app.year_published");
        convertProperty(app, config, "app.author");
        convertProperty(app, config, "app.description");
        convertProperty(app, config, "app.platform");
        convertProperty(app, config, "trs.model");
        convertFile(app, config, "app.screenshot", path);
        convertFile(app, config, "trs.image.disk", path);
        convertFile(app, config, "trs.image.cmd", path);

        saveJSON(appFolder, app);
    }

    private static void convertProperty(JSONObject app, PropertiesConfiguration config, String property) {
        String value = "";
        for (String v : config.getStringArray(property)) {
            if (value.length() != 0) {
                value += " ";
            }
            value += v;
        }

        setNestedProperty(app, property, value);
    }

    private static void convertFile(JSONObject app, PropertiesConfiguration config, String property, String path) {
        JSONArray files = new JSONArray();
        for (String v : config.getStringArray(property)) {
            JSONObject f = convertToBase64(path + File.separator + v);
            files.put(f);
        }
        setNestedProperty(app, property, files);
    }

    private static void setNestedProperty(JSONObject root, String property, Object value) {
        String[] properties = property.split("\\.");
        JSONObject prop = root;
        for (int i = 0; i < properties.length - 1; i++) {
            String p = properties[i];
            if (!prop.has(p)) {
                JSONObject nestedProp = new JSONObject();
                prop.put(p, nestedProp);
                prop = nestedProp;
            } else {
                prop = prop.getJSONObject(p);
            }
        }
        prop.put(properties[properties.length - 1], value);
    }

    private static JSONObject convertToBase64(String path) {
        JSONObject b64 = new JSONObject();
        try {
            InputStream is = new FileInputStream(new File(path));
            byte[] data = IOUtils.toByteArray(is);
            b64.put("ext", FilenameUtils.getExtension(path));
            b64.put("content", Base64.encodeBytes(data));
        } catch (IOException e) {
            // Do nothing
        }
        return b64;
    }

    private static void saveJSON(File appFolder, JSONObject app) throws IOException {
        String appName = FilenameUtils.getBaseName(appFolder.getCanonicalPath());
        File dir = new File(Config.OUTPUT_FOLDER);
        if (!dir.exists()) {
            dir.mkdir();
        }
        FileWriter out = new FileWriter(Config.OUTPUT_FOLDER + File.separator + appName + ".json");
        out.write(app.toString(2));
        out.close();
    }
}
