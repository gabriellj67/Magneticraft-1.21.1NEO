package committee.nova.mods.magneticraft.client.model;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

/** World-space bounds of a selected model scene. */
public record ModelSceneBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    public static ModelSceneBounds calculate(ModelScene scene, Set<Integer> selectedNodes) {
        return calculate(scene, selectedNodes, ModelTransform.IDENTITY);
    }

    public static ModelSceneBounds calculate(
            ModelScene scene,
            Set<Integer> selectedNodes,
            ModelTransform sourceTransform
    ) {
        MutableBounds bounds = new MutableBounds();
        for (int root : scene.rootNodes()) {
            visit(scene, root, sourceTransform.matrix(), selectedNodes, bounds);
        }
        if (!bounds.hasVertex) {
            throw new IllegalArgumentException("Selected model scene contains no vertices");
        }
        return new ModelSceneBounds(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
    }

    public float largestSpan() {
        return Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
    }

    private static void visit(
            ModelScene scene,
            int nodeIndex,
            Matrix4f parent,
            Set<Integer> selectedNodes,
            MutableBounds bounds
    ) {
        ModelScene.Node node = scene.node(nodeIndex);
        Matrix4f world = new Matrix4f(parent).mul(node.transform().matrix());
        if (selectedNodes.contains(nodeIndex)) {
            for (ModelScene.Primitive primitive : node.primitives()) {
                float[] positions = primitive.positions();
                for (int offset = 0; offset < positions.length; offset += 3) {
                    Vector3f position = world.transformPosition(
                            new Vector3f(positions[offset], positions[offset + 1], positions[offset + 2])
                    );
                    bounds.include(position);
                }
            }
        }
        for (int child : node.children()) {
            visit(scene, child, world, selectedNodes, bounds);
        }
    }

    private static final class MutableBounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;
        private boolean hasVertex;

        private void include(Vector3f position) {
            minX = Math.min(minX, position.x);
            minY = Math.min(minY, position.y);
            minZ = Math.min(minZ, position.z);
            maxX = Math.max(maxX, position.x);
            maxY = Math.max(maxY, position.y);
            maxZ = Math.max(maxZ, position.z);
            hasVertex = true;
        }
    }
}
