package committee.nova.mods.magneticraft.client.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Explicit node/part selection stored in generated legacy-scene model JSON. */
public record ModelSceneSelection(
        Set<String> includeNodes,
        Set<String> includeSubtrees,
        Set<String> excludeNodes,
        Set<String> excludeSubtrees
) {
    public static final ModelSceneSelection ALL = new ModelSceneSelection(Set.of(), Set.of(), Set.of(), Set.of());

    public ModelSceneSelection {
        includeNodes = Set.copyOf(includeNodes);
        includeSubtrees = Set.copyOf(includeSubtrees);
        excludeNodes = Set.copyOf(excludeNodes);
        excludeSubtrees = Set.copyOf(excludeSubtrees);
    }

    public Set<Integer> select(ModelScene scene) {
        LinkedHashSet<Integer> selected = new LinkedHashSet<>();
        boolean hasIncludes = !includeNodes.isEmpty() || !includeSubtrees.isEmpty();
        if (!hasIncludes) {
            for (ModelScene.Node node : scene.nodes()) {
                selected.add(node.index());
            }
        } else {
            for (ModelScene.Node node : scene.nodes()) {
                if (node.name() != null && includeNodes.contains(node.name())) {
                    selected.add(node.index());
                }
                if (node.name() != null && includeSubtrees.contains(node.name())) {
                    addSubtree(scene, node.index(), selected);
                }
            }
        }

        for (ModelScene.Node node : scene.nodes()) {
            if (node.name() != null && excludeNodes.contains(node.name())) {
                selected.remove(node.index());
            }
            if (node.name() != null && excludeSubtrees.contains(node.name())) {
                removeSubtree(scene, node.index(), selected);
            }
        }
        return Set.copyOf(selected);
    }

    public static ModelSceneSelection of(
            List<String> includeNodes,
            List<String> includeSubtrees,
            List<String> excludeNodes,
            List<String> excludeSubtrees
    ) {
        return new ModelSceneSelection(
                Set.copyOf(includeNodes),
                Set.copyOf(includeSubtrees),
                Set.copyOf(excludeNodes),
                Set.copyOf(excludeSubtrees)
        );
    }

    private static void addSubtree(ModelScene scene, int index, Set<Integer> selected) {
        if (!selected.add(index)) {
            return;
        }
        for (int child : scene.node(index).children()) {
            addSubtree(scene, child, selected);
        }
    }

    private static void removeSubtree(ModelScene scene, int index, Set<Integer> selected) {
        selected.remove(index);
        for (int child : scene.node(index).children()) {
            removeSubtree(scene, child, selected);
        }
    }
}
