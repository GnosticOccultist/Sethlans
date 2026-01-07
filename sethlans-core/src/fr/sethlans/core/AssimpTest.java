package fr.sethlans.core;

import java.util.ArrayList;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.assimp.Assimp;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.asset.AssimpLoader;
import fr.sethlans.core.asset.TextureLoader;
import fr.sethlans.core.material.Texture;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.scenegraph.Geometry;
import fr.sethlans.core.scenegraph.mesh.Mesh;
import fr.sethlans.core.scenegraph.mesh.Topology;
import fr.sethlans.core.scenegraph.mesh.Vertex;

public class AssimpTest extends SethlansApplication {

    private Texture texture;

    private Geometry vikingRoom;

    private Quaternionf rotation;

    private float angle;

    public static void main(String[] args) {
        launch(AssimpTest.class, args);
    }

    @Override
    protected void prepare(ConfigFile appConfig) {
        super.prepare(appConfig);
        
        appConfig.addString(APP_NAME_PROP, "Assimp Demo")
                .addInteger(APP_MAJOR_PROP, 1)
                .addInteger(APP_MINOR_PROP, 0)
                .addInteger(APP_PATCH_PROP, 0)
                .addBoolean(GRAPHICS_DEBUG_PROP, true)
                .addBoolean(VSYNC_PROP, false)
                .addInteger(MSAA_SAMPLES_PROP, 4)
                .addString(WINDOW_TITLE_PROP, "Assimp Demo")
                .addInteger(WINDOW_WIDTH_PROP, 800)
                .addInteger(WINDOW_HEIGHT_PROP, 600);
    }

    @Override
    protected void initialize() {
        var vertices = new ArrayList<Vertex>();
        var indices = new ArrayList<Integer>();
        AssimpLoader.load("resources/models/viking_room/viking_room.obj", Assimp.aiProcess_FlipUVs, true, vertices,
                indices);

        var mesh = new Mesh(Topology.TRIANGLES, indices, vertices);
        vikingRoom = new Geometry("Viking Room", mesh);

        texture = TextureLoader.load(getConfig(), "resources/models/viking_room/viking_room.png");
        vikingRoom.setTexture(texture);

        rotation = new Quaternionf();
    }

    @Override
    public void render(VulkanFrame frame) {

        angle += 0.1f;
        angle %= 360;

        rotation.identity().rotateAxis((float) Math.toRadians(90), new Vector3f(1, 0, 0))
                .rotateAxis((float) Math.toRadians(angle), new Vector3f(0, 0, 1));
        vikingRoom.getModelMatrix().identity().translationRotateScale(new Vector3f(0, 0.35f, -3f), rotation, 1);
        
        frame.command().begin();
        frame.render(vikingRoom);
        frame.command().end();
    }

    @Override
    protected void cleanup() {

    }
}
