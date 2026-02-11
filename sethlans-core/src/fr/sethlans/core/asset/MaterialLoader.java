package fr.sethlans.core.asset;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import fr.alchemy.utilities.file.FileUtils;
import fr.alchemy.utilities.file.json.AlchemyJSON;
import fr.alchemy.utilities.file.json.JSONArray;
import fr.alchemy.utilities.file.json.JSONObject;
import fr.alchemy.utilities.file.json.JSONValue;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.material.Material;
import fr.sethlans.core.material.MaterialLayout;
import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.material.MaterialPass.ShaderType;
import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.material.layout.BindingType;
import fr.sethlans.core.material.layout.PushConstantLayout;

public class MaterialLoader {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.asset");

    public static Material load(ConfigFile config, String path) {
        var extension = FileUtils.getExtension(path);

        if ("smat".equals(extension)) {
            try (var isr = FileUtils.readStream(Files.newInputStream(Paths.get(path)))) {
                return loadJSON(isr);
            } catch (IOException ex) {
                logger.error("An error has occured while reading material file '" + path + "' !", ex);
            }
        }

        throw new RuntimeException("Unable to load material from file with extension: " + extension);
    }

    private static Material loadJSON(Reader reader) throws IOException {
        var object = AlchemyJSON.parse(reader).asObject();
        var name = object.getOptional("name").map(JSONValue::asString).orElse(null);
        // Retrieve the potential description of the material.
        var description = object.getOptional("description").map(JSONValue::asString).orElse(null);

        // Prevent having a null name for material.
        if (name == null || name.isEmpty()) {
            name = "Undefined";
        }

        // Create the material with the data.
        var material = new Material(name, description);
        // Load the specified material passes in the file into the material.
        loadPasses(material, object);

        logger.info("Successfully loaded material '" + name + "' !");
        return material;
    }

    private static void loadPasses(Material material, JSONObject object) throws IOException {
        var passes = object.getOptional("passes").orElseThrow(IOException::new).asObject();
        // Perform the action for all possible material passes described in the file.
        for (int i = 0; i < passes.size(); i++) {
            var name = passes.names().get(i);
            var passObj = passes.get(name).asObject();

            var pass = new MaterialPass(material, name);

            // Load the specified material layout in the file into the material pass.
            loadLayout(pass, passObj);

            // Load the specified shaders in the file into the material pass.
            loadShaders(pass, passObj);

            material.addPass(pass);
        }
    }

    private static void loadLayout(MaterialPass pass, JSONObject passObj) throws IOException {
        var layoutObj = passObj.getOptional("layout").orElseThrow(IOException::new).asObject();
        var sets = layoutObj.getOptional("sets").orElseThrow(IOException::new).asArray();

        var layout = new MaterialLayout();

        // Look for bindings set layout.
        for (var i = 0; i < sets.size(); i++) {
            var setObj = sets.get(i).asObject();
            var set = setObj.getOptional("set").orElseThrow(IOException::new).asInt();
            var bindingsObj = setObj.getOptional("bindings").orElseThrow(IOException::new).asArray();
            var bindings = loadBindings(bindingsObj);

            layout.putBindingsSet(set, bindings);
        }
        
        var pushConstants = layoutObj.getOptional("push_constants").map(JSONValue::asArray).orElse(null);
        if (pushConstants != null) {
            for (var i = 0; i < pushConstants.size(); i++) {
                var pushConstantObj = pushConstants.get(i).asObject();
                var pushConstant = loadPushConstant(pushConstantObj);
                
                layout.addPushConstant(pushConstant);
            }
        }
        
        pass.setLayout(layout);
    }

    private static List<BindingLayout> loadBindings(JSONArray bindingsArray) throws IOException {
        var size = bindingsArray.size();
        List<BindingLayout> bindings = new ArrayList<>(size);
        for (var i = 0; i < size; i++) {
            var bindingObj = bindingsArray.get(i).asObject();
            var binding = bindingObj.getOptional("binding").map(JSONValue::asInt).orElse(0);
            var name = bindingObj.getOptional("name").map(JSONValue::asString).orElse("Undefined");
            var builtin = bindingObj.getOptional("builtin").map(JSONValue::asString).orElse(null);
            var type = bindingObj.getOptional("type").orElseThrow(IOException::new).asString();
            var bindingType = getBindingType(type);
            var count = bindingObj.getOptional("count").map(JSONValue::asInt).orElse(1);
            var shadersValue = bindingObj.getOptional("shaders").orElseThrow(IOException::new);
            var shaderTypes = getShaderTypes(shadersValue);

            var bindingLayout = new BindingLayout((builtin != null) ? builtin : name, builtin != null, binding, bindingType, shaderTypes, count);
            bindings.add(bindingLayout);
        }

        return bindings;
    }
    
    private static PushConstantLayout loadPushConstant(JSONObject pushConstantObj) throws IOException {
        var name = pushConstantObj.getOptional("name").map(JSONValue::asString).orElse("Undefined");
        var offset = pushConstantObj.getOptional("offset").map(JSONValue::asInt).orElse(0);
        var size = pushConstantObj.getOptional("size").orElseThrow(IOException::new).asInt();
        var shadersValue = pushConstantObj.getOptional("shaders").orElseThrow(IOException::new);
        var shaderTypes = getShaderTypes(shadersValue);
        
        var pushConstant = new PushConstantLayout(name, offset, size, shaderTypes);
        return pushConstant;
    }

    private static BindingType getBindingType(String type) throws IOException {
        for (var bindingType : BindingType.values()) {
            if (bindingType.name().toLowerCase().equals(type.toLowerCase())) {
                return bindingType;
            }
        }

        throw new IOException("Invalid binding type '" + type + "'!");
    }

    private static EnumSet<ShaderType> getShaderTypes(JSONValue value) throws IOException {
        EnumSet<ShaderType> shaderTypes = null;
        if (value.isString()) {
            shaderTypes = EnumSet.of(getShaderType(value.asString()));

        } else {
            var array = value.asArray();
            shaderTypes = EnumSet.noneOf(ShaderType.class);
            for (var i = 0; i < array.size(); i++) {
                var type = array.get(i).asString();
                var shaderType = getShaderType(type);
                shaderTypes.add(shaderType);
            }
        }

        return shaderTypes;
    }

    private static ShaderType getShaderType(String type) throws IOException {
        for (var shaderType : ShaderType.values()) {
            if (shaderType.name().toLowerCase().equals(type.toLowerCase())) {
                return shaderType;
            }
        }

        throw new IOException("Invalid shader type '" + type + "'!");
    }

    private static void loadShaders(MaterialPass pass, JSONObject passObj) throws IOException {
        var shaders = passObj.getOptional("shaders").orElseThrow(IOException::new).asArray();

        // Look for shader type.
        for (var i = 0; i < shaders.size(); i++) {
            var shaderObj = shaders.get(i).asObject();
            var name = shaderObj.names().get(0);

            for (var type : ShaderType.values()) {
                if (type.name().toLowerCase().equals(name)) {

                    var shaderPath = shaderObj.get(name).asString();
                    // Check that the retrieved type and the extension of the source path
                    // correspond.
                    var extension = FileUtils.getExtension(shaderPath);
                    if (ShaderType.fromExtension(extension) != type) {
                        throw new IllegalStateException("The specified shader type '" + type + "' doesn't "
                                + "correspond to the source path extension '" + extension + "' !");
                    }

                    // Look for an optional entry point.
                    var entry = shaderObj.getOptional("entry").map(JSONValue::asString).orElse("main");
                    pass.addShaderSource(type, "resources/shaders/" + shaderPath, entry);
                }
            }
        }
    }
}
