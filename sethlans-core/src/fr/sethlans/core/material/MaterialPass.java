package fr.sethlans.core.material;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map.Entry;

public class MaterialPass {
    
    private final Material material;

    private String name;
    
    private long sortId = -1;
    
    private MaterialLayout layout;
    
    private final EnumMap<ShaderType, ShaderModuleInfo> sources = new EnumMap<>(ShaderType.class);
    
    public MaterialPass(Material material, String name) {
        this.material = material;
        this.name = name;
    }

    public void addShaderSource(ShaderType type, String shaderName, String entryPoint) {
        this.sources.put(type, new ShaderModuleInfo(shaderName, type, entryPoint));
    }
    
    public Collection<Entry<ShaderType, ShaderModuleInfo>> getShaderSources() {
        return Collections.unmodifiableCollection(sources.entrySet());
    }
    
    public String getFullName() {
        return material.getName() + "#" + getName();
    }

    public String getName() {
        return name;
    }

    public MaterialLayout getLayout() {
        return layout;
    }

    public void setLayout(MaterialLayout layout) {
        this.layout = layout;
    }

    public long sortId() {
        if (sortId == -1) {
            
        }
        
        return sortId;
    }
    
    public boolean isComputePass() {
        return sources.containsKey(ShaderType.COMPUTE);
    }

    @Override
    public String toString() {
        return "MaterialPass [name=" + name + "]";
    }
    
    public record ShaderModuleInfo(String shaderName, ShaderType type, String entryPoint) {
        
    }
    
    public enum ShaderType {
        
        /**
         * Vertex processing shader. Use it for computing gl_Position, the vertex
         * position.
         */
        VERTEX("vert"),
        /**
         * Geometry assembly shader. For example it can compile a polygon mesh from
         * data.
         */
        GEOMETRY("geom"),
        /**
         * Fragment rasterization shader. It can be used to determine the color of the
         * pixel.
         */
        FRAGMENT("frag"),
        /**
         * Tesselation control shader. For example it can determine how often an input
         * VBO Patch should be subdivided.
         */
        TESS_CONTROL("tessctrl"),
        /**
         * Tesselation evaluation shader. It is similar to the vertex shader, as it
         * computes the interpolated positions and some other per-vertex data.
         */
        TESS_EVAL("tesseval"),
        /**
         * Compute shader. Should be used entirely for computing arbitrary information.
         * While it can do rendering, it is generally used for tasks not directly
         * related to drawing triangles and pixels.
         */
        COMPUTE("comp");

        private String extension;

        private ShaderType(String extension) {
            this.extension = extension;
        }
        
        public static ShaderType fromExtension(String extension) {
            for (int i = 0; i < ShaderType.values().length; i++) {
                if (ShaderType.values()[i].extension.equals(extension)) {
                    return ShaderType.values()[i];
                }
            }
            return null;
        }

        public String extension() {
            return extension;
        }
    }
}
