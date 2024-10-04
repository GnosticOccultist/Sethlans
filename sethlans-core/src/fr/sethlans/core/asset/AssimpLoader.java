package fr.sethlans.core.asset;

import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AILogStream;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.Assimp;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;

public class AssimpLoader {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.asset");

    public static void load(String modelPath, int flags, boolean debug, List<Vertex> vertices, List<Integer> indices) {
        logger.debug("Loading model data from '" + modelPath + "'.");
        if (debug) {
            var logStream = AILogStream.create();
            String filename = null;
            logStream = Assimp.aiGetPredefinedLogStream(Assimp.aiDefaultLogStream_STDERR, filename, logStream);
            Assimp.aiAttachLogStream(logStream);
            Assimp.aiEnableVerboseLogging(true);
        }

        var aiScene = Assimp.aiImportFile(modelPath, flags);
        Assimp.aiDetachAllLogStreams();
        if (aiScene == null || aiScene.mRootNode() == null) {
            throw new RuntimeException(
                    "Assimp failed to import the 3D model from '" + modelPath + "': " + Assimp.aiGetErrorString());
        }

        var rootNode = aiScene.mRootNode();
        var pMeshes = aiScene.mMeshes();
        processNode(rootNode, pMeshes, vertices, indices);

        Assimp.aiReleaseImport(aiScene);
    }

    private static void processNode(AINode aiNode, PointerBuffer pMeshes, List<Vertex> vertices,
            List<Integer> indices) {
        if (aiNode.mMeshes() != null) {
            var pMeshIndices = aiNode.mMeshes();
            if (pMeshIndices == null) {
                return;
            }

            var numMeshesInNode = pMeshIndices.capacity();
            for (var i = 0; i < numMeshesInNode; ++i) {
                var meshIndex = pMeshIndices.get(i);
                var aiMeshHandle = pMeshes.get(meshIndex);
                var aiMesh = AIMesh.create(aiMeshHandle);
                processMesh(aiMesh, vertices, indices);
            }
        }

        var children = aiNode.mChildren();
        if (children != null) {
            var numChildren = aiNode.mNumChildren();
            for (var i = 0; i < numChildren; ++i) {
                var childHandle = children.get(i);
                var child = AINode.create(childHandle);
                processNode(child, pMeshes, vertices, indices);
            }
        }
    }

    private static void processMesh(AIMesh aiMesh, List<Vertex> vertices, List<Integer> indices) {
        var pPositions = aiMesh.mVertices();
        var vertexCount = pPositions.capacity();

        var pTexCoords = aiMesh.mTextureCoords(0);
        if (pTexCoords != null) {
            assert pTexCoords.capacity() == vertexCount;
        }

        var pColors = aiMesh.mColors(0);
        if (pColors != null) {
            assert pColors.capacity() == vertexCount;
        }

        var pNormals = aiMesh.mNormals();
        if (pNormals != null) {
            assert pNormals.capacity() == vertexCount;
        }

        for (var i = 0; i < vertexCount; ++i) {
            var aiPosition = pPositions.get(i);
            var position = new Vector3f(aiPosition.x(), aiPosition.y(), aiPosition.z());

            Vector3fc color = null;
            if (pColors != null) {
                var aiColor = pColors.get(i);
                color = new Vector3f(aiColor.r(), aiColor.g(), aiColor.b());
                // Note: alpha gets dropped
            }

            Vector3fc normal = null;
            if (pNormals != null) {
                var aiNormal = pNormals.get(i);
                normal = new Vector3f(aiNormal.x(), aiNormal.y(), aiNormal.z());
            }

            Vector2fc texCoords = null;
            if (pTexCoords != null) {
                var aiTexCoords = pTexCoords.get(i);
                texCoords = new Vector2f(aiTexCoords.x(), aiTexCoords.y());
            }

            var vertex = new Vertex(position, color, normal, texCoords);
            vertices.add(vertex);
        }

        if (!vertices.isEmpty()) {
            var pFaces = aiMesh.mFaces();
            var faceCount = pFaces.capacity();
            for (var i = 0; i < faceCount; ++i) {
                var face = pFaces.get(i);
                var pIndices = face.mIndices();
                var numIndices = face.mNumIndices();
                if (numIndices == 3) {
                    for (var j = 0; j < numIndices; ++j) {
                        var vertexIndex = pIndices.get(j);
                        indices.add(vertexIndex);
                    }
                } else {
                    System.out.printf("skipped a mesh face with %d indices%n", numIndices);
                }
            }
        }
    }
}
