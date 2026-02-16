package fr.sethlans.core.scenegraph.mesh;

import java.util.Collection;
import java.util.List;

import fr.alchemy.utilities.Validator;
import fr.sethlans.core.render.backend.BackendObject;
import fr.sethlans.core.render.buffer.NativeBuffer;
import fr.sethlans.core.render.buffer.StageableBuffer;
import fr.sethlans.core.util.BufferUtils;

public class Mesh extends BackendObject {

    private Topology topology = Topology.TRIANGLES;

    private int vertexCount;

    private StageableBuffer<NativeBuffer> vertexData;

    private StageableBuffer<NativeBuffer> indices;

    private int fpv;

    public Mesh(Topology topology, Collection<Integer> indices, List<Vertex> vertices) {
        Validator.nonNull(topology, "The mesh topology can't be null!");
        this.topology = topology;
        this.vertexCount = vertices.size();

        this.indices = new StageableBuffer<>(BufferUtils.create(indices));

        var vertex = vertices.get(0);
        this.fpv = vertex.numFloats();
        this.vertexData = new StageableBuffer<>(BufferUtils.createVertex(vertices, vertex));
    }

    public Mesh(Topology topology, int[] indices, float[] vertices, int fpv) {
        Validator.nonNull(topology, "The mesh topology can't be null!");
        this.topology = topology;
        this.vertexCount = vertices.length / fpv;

        this.indices = new StageableBuffer<>(BufferUtils.create(indices));
        this.fpv = fpv;
        this.vertexData = new StageableBuffer<>(BufferUtils.create(vertices));
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

    public StageableBuffer<NativeBuffer> getVertexData() {
        return vertexData;
    }

    public StageableBuffer<NativeBuffer> getIndices() {
        return indices;
    }
}
