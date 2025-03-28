package fr.sethlans.core.scenegraph.primitive;

import fr.sethlans.core.scenegraph.Geometry;
import fr.sethlans.core.scenegraph.mesh.Mesh;
import fr.sethlans.core.scenegraph.mesh.Topology;

public class Box extends Geometry {
    
    private static final float[] VERTEX_DATA = new float[] { 
            -0.5f, 0.5f, 0.5f, 
            0.0f, 1.0f, 
            -0.5f, -0.5f, 0.5f, 
            0.0f, 0.0f, 
            0.5f, -0.5f, 0.5f, 
            1.0f, 0.0f, 
            0.5f, 0.5f, 0.5f, 
            1.0f, 1.0f, 
            -0.5f, 0.5f, -0.5f, 
            0.0f, 1.0f, 
            0.5f, 0.5f, -0.5f, 
            1.0f, 1.0f, 
            -0.5f, -0.5f, -0.5f, 
            0.0f, 0.0f, 
            0.5f, -0.5f, -0.5f, 
            1.0f, 0.0f 
    };

    private static final int[] INDICES = new int[] { 
            0, 1, 3, 
            3, 1, 2, 
            4, 0, 3, 
            5, 4, 3, 
            3, 2, 7, 
            5, 3, 7, 
            6, 1, 0, 
            6, 0, 4, 
            2, 1, 6, 
            2, 6, 7, 
            7, 6, 4, 
            7, 4, 5 
    };

    public Box(String name) {
        super(name, new Mesh(Topology.TRIANGLES, INDICES, VERTEX_DATA, 5));
    }
}
