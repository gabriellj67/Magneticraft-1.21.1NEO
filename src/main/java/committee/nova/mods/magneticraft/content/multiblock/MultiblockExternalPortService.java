package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Runtime index for released ports intentionally located outside the hidden structure volume. */
public final class MultiblockExternalPortService {
    private static final Map<ServerLevel, Map<Key, ExternalNode>> NODES = new WeakHashMap<>();

    private MultiblockExternalPortService() {
    }

    public static List<ExternalNode> register(
            ServerLevel level,
            AdvancedMultiblockBlockEntity controller
    ) {
        Set<BlockPos> members = Set.copyOf(controller.members());
        Map<Key, EnumSet<Direction>> grouped = new HashMap<>();
        for (MultiblockPortLayout.Port port : MultiblockPortLayout.ports(controller.definition())) {
            NetworkDomain domain = domain(port.kind());
            if (domain == null) {
                continue;
            }
            BlockPos position = port.worldPosition(controller);
            if (position.equals(controller.getBlockPos()) || members.contains(position)) {
                continue;
            }
            grouped.computeIfAbsent(new Key(domain, position), ignored -> EnumSet.noneOf(Direction.class))
                    .add(port.worldSide(controller.facing()));
        }
        List<ExternalNode> registered = new ArrayList<>();
        Map<Key, ExternalNode> levelNodes = NODES.computeIfAbsent(level, ignored -> new HashMap<>());
        for (Map.Entry<Key, EnumSet<Direction>> entry : grouped.entrySet()) {
            ExternalNode node = new ExternalNode(controller, entry.getKey(), entry.getValue());
            ExternalNode replaced = levelNodes.put(entry.getKey(), node);
            if (replaced != null) {
                PhysicalNetworkService.manager(level).unregister(replaced);
            }
            PhysicalNetworkService.manager(level).register(node);
            ConduitBlock.refreshAround(level, node.position());
            registered.add(node);
        }
        return List.copyOf(registered);
    }

    public static void unregister(ServerLevel level, List<ExternalNode> nodes) {
        Map<Key, ExternalNode> levelNodes = NODES.get(level);
        for (ExternalNode node : nodes) {
            PhysicalNetworkService.manager(level).unregister(node);
            if (levelNodes != null) {
                levelNodes.remove(node.key(), node);
            }
            ConduitBlock.refreshAround(level, node.position());
        }
        if (levelNodes != null && levelNodes.isEmpty()) {
            NODES.remove(level);
        }
    }

    public static boolean supports(
            LevelAccessor level,
            BlockPos position,
            NetworkDomain domain,
            Direction side
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        Map<Key, ExternalNode> levelNodes = NODES.get(serverLevel);
        ExternalNode node = levelNodes == null ? null : levelNodes.get(new Key(domain, position));
        return node != null && node.connectionSides().contains(side);
    }

    private static NetworkDomain domain(MultiblockPortLayout.Kind kind) {
        return switch (kind) {
            case ELECTRICITY -> NetworkDomain.ELECTRICITY;
            case HEAT -> NetworkDomain.HEAT;
            default -> null;
        };
    }

    private record Key(NetworkDomain domain, BlockPos position) {
        private Key {
            position = position.immutable();
        }
    }

    public static final class ExternalNode implements PhysicalNetworkNode {
        private final AdvancedMultiblockBlockEntity controller;
        private final Key key;
        private final Set<Direction> sides;

        private ExternalNode(
                AdvancedMultiblockBlockEntity controller,
                Key key,
                Set<Direction> sides
        ) {
            this.controller = controller;
            this.key = key;
            this.sides = Set.copyOf(sides);
        }

        private Key key() {
            return key;
        }

        @Override
        public NetworkDomain domain() {
            return key.domain();
        }

        @Override
        public BlockPos position() {
            return key.position();
        }

        @Override
        public Set<Direction> connectionSides() {
            return controller.operational() ? sides : Set.of();
        }

        @Override
        public PhysicalNetworkNode transferNode() {
            if (!controller.operational()) {
                return this;
            }
            return switch (domain()) {
                case ELECTRICITY -> controller.electricity() == null ? this : controller.electricity();
                case HEAT -> controller.heat() == null ? this : controller.heat();
                default -> this;
            };
        }

        @Override
        public void exchangeWith(PhysicalNetworkNode other) {
            PhysicalNetworkNode delegate = transferNode();
            if (delegate != this) {
                delegate.exchangeWith(other.transferNode());
            }
        }
    }
}
