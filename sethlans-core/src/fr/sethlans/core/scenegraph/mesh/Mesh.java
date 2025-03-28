package fr.sethlans.core.scenegraph.mesh;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.List;

import fr.alchemy.utilities.Validator;
import fr.sethlans.core.render.backend.BackendObject;
import fr.sethlans.core.util.BufferUtils;

public class Mesh extends BackendObject {

    private Topology topology;

    private int vertexCount;

    private FloatBuffer vertexData;

    private Buffer indices;

    private int fpv;

    public Mesh(Topology topology, Collection<Integer> indices, List<Vertex> vertices) {
        Validator.nonNull(topology, "The mesh topology can't be null!");
        this.vertexCount = vertices.size();

        this.indices = BufferUtils.create(indices);

        var vertex = vertices.get(0);
        this.fpv = vertex.numFloats();
        this.vertexData = BufferUtils.createVertex(vertices, vertex);
    }

    public Mesh(Topology topology, int[] indices, float[] vertices, int fpv) {
        Validator.nonNull(topology, "The mesh topology can't be null!");
        this.vertexCount = vertices.length / fpv;

        this.indices = BufferUtils.create(indices);
        this.fpv = fpv;
        this.vertexData = BufferUtils.create(vertices);
    }

    public int vertexCount() {
        return vertexCount;
    }
    
    public int fpv() {
        return fpv;
    }

    public Topology topology() {
        return topology;
    }

    public FloatBuffer getVertexData() {
        return vertexData;
    }

    public Buffer getIndices() {
        return indices;
    }
}
