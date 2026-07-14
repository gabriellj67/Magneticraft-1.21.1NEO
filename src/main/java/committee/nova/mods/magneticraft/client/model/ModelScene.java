package committee.nova.mods.magneticraft.client.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Immutable render-neutral representation shared by MCX and glTF. */
public record ModelScene(
        Format format,
        boolean ambientOcclusion,
        boolean gui3d,
        String particleTexture,
        List<Integer> rootNodes,
        List<Node> nodes,
        List<Animation> animations
) {
    public ModelScene {
        rootNodes = List.copyOf(rootNodes);
        nodes = List.copyOf(nodes);
        animations = List.copyOf(animations);
    }

    public Node node(int index) {
        if (index < 0 || index >= nodes.size()) {
            throw new IndexOutOfBoundsException("Unknown model node " + index);
        }
        return nodes.get(index);
    }

    public Optional<Animation> animation(String name) {
        return animations.stream().filter(animation -> animation.name().equals(name)).findFirst();
    }

    public enum Format {
        MCX,
        GLTF
    }

    public record Node(
            int index,
            String name,
            ModelTransform transform,
            List<Integer> children,
            List<Primitive> primitives
    ) {
        public Node {
            children = List.copyOf(children);
            primitives = List.copyOf(primitives);
        }

        public boolean branch() {
            return primitives.isEmpty();
        }
    }

    /**
     * Quad-oriented primitive. Each face occupies four vertices; glTF triangles repeat the final vertex.
     */
    public record Primitive(
            String texture,
            String cullFace,
            float[] positions,
            float[] textureCoordinates,
            float[] normals
    ) {
        public Primitive {
            positions = Arrays.copyOf(positions, positions.length);
            textureCoordinates = Arrays.copyOf(textureCoordinates, textureCoordinates.length);
            normals = Arrays.copyOf(normals, normals.length);
            if (positions.length % 12 != 0) {
                throw new IllegalArgumentException("Primitive positions must contain four XYZ vertices per face");
            }
            int vertexCount = positions.length / 3;
            if (textureCoordinates.length != vertexCount * 2 || normals.length != vertexCount * 3) {
                throw new IllegalArgumentException("Primitive attribute lengths do not match");
            }
        }

        @Override
        public float[] positions() {
            return Arrays.copyOf(positions, positions.length);
        }

        @Override
        public float[] textureCoordinates() {
            return Arrays.copyOf(textureCoordinates, textureCoordinates.length);
        }

        @Override
        public float[] normals() {
            return Arrays.copyOf(normals, normals.length);
        }

        float[] positionsView() {
            return positions;
        }

        float[] textureCoordinatesView() {
            return textureCoordinates;
        }

        float[] normalsView() {
            return normals;
        }

        public int faceCount() {
            return positions.length / 12;
        }
    }

    public record Animation(String name, float durationSeconds, List<Channel> channels) {
        public Animation {
            channels = List.copyOf(channels);
        }
    }

    public record Channel(
            int node,
            Path path,
            Interpolation interpolation,
            float[] times,
            float[] values,
            int valueWidth
    ) {
        public Channel {
            times = Arrays.copyOf(times, times.length);
            values = Arrays.copyOf(values, values.length);
            if (times.length == 0 || values.length != times.length * valueWidth) {
                throw new IllegalArgumentException("Animation channel keyframe data does not match");
            }
        }

        @Override
        public float[] times() {
            return Arrays.copyOf(times, times.length);
        }

        @Override
        public float[] values() {
            return Arrays.copyOf(values, values.length);
        }

        float[] timesView() {
            return times;
        }

        float[] valuesView() {
            return values;
        }
    }

    public enum Path {
        TRANSLATION(3),
        ROTATION(4),
        SCALE(3);

        private final int width;

        Path(int width) {
            this.width = width;
        }

        public int width() {
            return width;
        }
    }

    public enum Interpolation {
        LINEAR,
        STEP
    }
}
