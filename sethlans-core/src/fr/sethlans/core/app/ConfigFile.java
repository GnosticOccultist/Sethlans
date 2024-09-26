package fr.sethlans.core.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import fr.alchemy.utilities.file.json.AlchemyJSON;
import fr.alchemy.utilities.file.json.JSONObject;
import fr.alchemy.utilities.file.json.JSONValue;

public class ConfigFile {

    private JSONObject root;

    public ConfigFile() {
        this.root = AlchemyJSON.object();
    }

    public Boolean getBoolean(String name, Boolean defValue) {
        return root.getOptional(name).filter(JSONValue::isTrue).map(JSONValue::isTrue).orElse(defValue);
    }

    public Integer getInteger(String name, Integer defValue) {
        return root.getOptional(name).filter(JSONValue::isNumber).map(JSONValue::asInt).orElse(defValue);
    }

    public ConfigFile addInteger(String name, Integer value) {
        root.add(name, AlchemyJSON.value(value));
        return this;
    }

    public Float getFloat(String name, Float defValue) {
        return root.getOptional(name).filter(JSONValue::isNumber).map(JSONValue::asFloat).orElse(defValue);
    }

    public String getString(String name, String defValue) {
        return root.getOptional(name).filter(JSONValue::isString).map(JSONValue::asString).orElse(defValue);
    }

    public ConfigFile addString(String name, String value) {
        root.add(name, AlchemyJSON.value(value));
        return this;
    }

    public void load(Path configPath) {
        try (var reader = Files.newBufferedReader(configPath)) {
            this.root = AlchemyJSON.parse(reader).asObject();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
