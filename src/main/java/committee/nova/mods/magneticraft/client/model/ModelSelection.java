package committee.nova.mods.magneticraft.client.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** ModelLoader-compatible named component and animation selection. */
public final class ModelSelection {
    private ModelSelection() {
    }

    public enum Target {
        BRANCH,
        LEAF,
        ANIMATION
    }

    @FunctionalInterface
    public interface Filter {
        boolean test(String name, Target target);
    }

    public record Selector(String name, Filter componentFilter, Filter animationFilter) {
        public Selector(String name, Filter componentFilter) {
            this(name, componentFilter, ignoreAnimation());
        }
    }

    public static Set<Integer> nodes(ModelScene scene, Filter filter) {
        LinkedHashSet<Integer> selected = new LinkedHashSet<>();
        if (scene.format() == ModelScene.Format.MCX) {
            for (ModelScene.Node node : scene.nodes()) {
                if (node.name() != null && filter.test(node.name(), Target.LEAF)) {
                    selected.add(node.index());
                }
            }
            return Set.copyOf(selected);
        }
        for (int root : scene.rootNodes()) {
            selected.addAll(recursive(scene, root, filter));
        }
        return Set.copyOf(selected);
    }

    public static ModelScene.Animation animation(ModelScene scene, Filter filter) {
        return scene.animations().stream()
                .filter(animation -> filter.test(animation.name(), Target.ANIMATION))
                .findFirst()
                .orElse(null);
    }

    private static Set<Integer> recursive(ModelScene scene, int nodeIndex, Filter filter) {
        ModelScene.Node node = scene.node(nodeIndex);
        LinkedHashSet<Integer> all = new LinkedHashSet<>();
        for (int child : node.children()) {
            all.addAll(recursive(scene, child, filter));
        }
        all.add(nodeIndex);
        if (node.name() == null) {
            return all;
        }
        Target target = node.branch() ? Target.BRANCH : Target.LEAF;
        return filter.test(node.name(), target) ? all : Set.of();
    }

    public static Filter always() {
        return (name, target) -> true;
    }

    public static Filter ignoreAnimation() {
        return not(always());
    }

    public static Filter exact(String value, Target... targets) {
        Set<Target> targetSet = targetSet(targets);
        return targeted(targetSet, name -> value.equals(name));
    }

    public static Filter notExact(String value, Target... targets) {
        Set<Target> targetSet = targetSet(targets);
        return targeted(targetSet, name -> !value.equals(name));
    }

    public static Filter regex(String expression, Target target) {
        Pattern pattern = Pattern.compile(expression);
        return targeted(Set.of(target), name -> pattern.matcher(name).matches());
    }

    public static Filter regex(String expression) {
        return regex(expression, Target.LEAF);
    }

    public static Filter notRegex(String expression, Target target) {
        Pattern pattern = Pattern.compile(expression);
        return targeted(Set.of(target), name -> !pattern.matcher(name).matches());
    }

    public static Filter notRegex(String expression) {
        return notRegex(expression, Target.LEAF);
    }

    public static Filter and(Filter... filters) {
        List<Filter> children = List.copyOf(Arrays.asList(filters));
        return (name, target) -> children.stream().allMatch(filter -> filter.test(name, target));
    }

    public static Filter or(Filter... filters) {
        List<Filter> children = List.copyOf(Arrays.asList(filters));
        return (name, target) -> children.stream().anyMatch(filter -> filter.test(name, target));
    }

    public static Filter not(Filter filter) {
        return (name, target) -> !filter.test(name, target);
    }

    private static Filter targeted(Set<Target> targets, Predicate<String> predicate) {
        return (name, target) -> !targets.contains(target) || predicate.test(name);
    }

    private static Set<Target> targetSet(Target[] targets) {
        return targets.length == 0 ? Set.of(Target.LEAF) : Set.copyOf(Arrays.asList(targets));
    }
}
