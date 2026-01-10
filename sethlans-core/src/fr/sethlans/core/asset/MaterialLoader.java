package fr.sethlans.core.asset;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import fr.alchemy.utilities.file.FileUtils;
import fr.alchemy.utilities.file.json.AlchemyJSON;
import fr.alchemy.utilities.file.json.JSONObject;
import fr.alchemy.utilities.file.json.JSONValue;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.material.Material;
import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.material.MaterialPass.ShaderType;

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
            
            // Load the specified shaders in the file into the material pass.
            loadShaders(pass, passObj);
            
            material.addPass(pass);
        }
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
