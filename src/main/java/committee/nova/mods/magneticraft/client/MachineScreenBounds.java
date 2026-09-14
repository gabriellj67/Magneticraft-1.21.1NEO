package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout.Rect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable geometry contract used by tests and one-shot development screen validation. */
final class MachineScreenBounds {
    private MachineScreenBounds() {
    }

    static Builder builder(String screenName, int width, int height) {
        return new Builder(screenName, new Rect(0, 0, width, height));
    }

    record Element(String name, Rect bounds, String parent, String collisionGroup, int minimumGap) {
    }

    record Layout(String screenName, Rect screen, List<Element> elements) {
        Layout {
            elements = List.copyOf(elements);
        }

        void validate() {
            List<String> problems = problems();
            if (!problems.isEmpty()) {
                throw new IllegalStateException(String.join("; ", problems));
            }
        }

        List<String> problems() {
            List<String> problems = new ArrayList<>();
            Map<String, Element> byName = new LinkedHashMap<>();
            for (Element element : elements) {
                if (byName.putIfAbsent(element.name(), element) != null) {
                    problems.add(screenName + ": duplicate element '" + element.name() + "'");
                }
                if (element.bounds().width() <= 0 || element.bounds().height() <= 0) {
                    problems.add(screenName + ": non-positive bounds for '" + element.name() + "' " + element.bounds());
                } else if (!screen.contains(element.bounds())) {
                    problems.add(screenName + ": '" + element.name() + "' exceeds screen " + element.bounds());
                }
            }
            for (Element element : elements) {
                if (element.parent() == null) {
                    continue;
                }
                Element parent = byName.get(element.parent());
                if (parent == null) {
                    problems.add(screenName + ": missing parent '" + element.parent() + "' for '" + element.name() + "'");
                } else if (!parent.bounds().contains(element.bounds())) {
                    problems.add(screenName + ": '" + element.name() + "' " + element.bounds()
                            + " exceeds parent '" + parent.name() + "' " + parent.bounds());
                }
            }
            for (int first = 0; first < elements.size(); first++) {
                Element left = elements.get(first);
                if (left.collisionGroup() == null) {
                    continue;
                }
                for (int second = first + 1; second < elements.size(); second++) {
                    Element right = elements.get(second);
                    if (!left.collisionGroup().equals(right.collisionGroup())) {
                        continue;
                    }
                    if (left.bounds().overlaps(right.bounds())) {
                        problems.add(screenName + ": '" + left.name() + "' " + left.bounds()
                                + " overlaps '" + right.name() + "' " + right.bounds());
                        continue;
                    }
                    int minimumGap = Math.max(left.minimumGap(), right.minimumGap());
                    if (minimumGap > 0 && gap(left.bounds(), right.bounds()) < minimumGap) {
                        problems.add(screenName + ": '" + left.name() + "' and '" + right.name()
                                + " have less than " + minimumGap + " px clearance");
                    }
                }
            }
            return List.copyOf(problems);
        }
    }

    static final class Builder {
        private final String screenName;
        private final Rect screen;
        private final List<Element> elements = new ArrayList<>();

        private Builder(String screenName, Rect screen) {
            this.screenName = screenName;
            this.screen = screen;
        }

        Builder element(String name, Rect bounds) {
            return element(name, bounds, null, null, 0);
        }

        Builder element(String name, Rect bounds, String collisionGroup, int minimumGap) {
            return element(name, bounds, null, collisionGroup, minimumGap);
        }

        Builder child(String name, Rect bounds, String parent, String collisionGroup, int minimumGap) {
            return element(name, bounds, parent, collisionGroup, minimumGap);
        }

        Layout build() {
            return new Layout(screenName, screen, elements);
        }

        private Builder element(
                String name,
                Rect bounds,
                String parent,
                String collisionGroup,
                int minimumGap
        ) {
            elements.add(new Element(name, bounds, parent, collisionGroup, Math.max(0, minimumGap)));
            return this;
        }
    }

    private static int gap(Rect left, Rect right) {
        int horizontal = Math.max(0, Math.max(left.x(), right.x()) - Math.min(left.right(), right.right()));
        int vertical = Math.max(0, Math.max(left.y(), right.y()) - Math.min(left.bottom(), right.bottom()));
        if (horizontal == 0) {
            return vertical;
        }
        if (vertical == 0) {
            return horizontal;
        }
        return Math.min(horizontal, vertical);
    }
}
