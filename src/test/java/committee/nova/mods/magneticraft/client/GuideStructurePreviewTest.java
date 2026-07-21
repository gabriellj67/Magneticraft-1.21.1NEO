package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.client.guide.GuideRepository;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideStructurePreviewTest {
    @Test
    void buildsCumulativeThreeDimensionalLayersAndSkipsIgnoredCells() {
        GuideRepository.LegendEntry controller = legend('C', "controller", "magneticraft:grinder", false);
        GuideRepository.LegendEntry ignored = legend('I', "ignore", null, true);
        GuideRepository.LegendEntry air = legend('A', "air", "minecraft:air", false);
        GuideRepository.LegendEntry casing = legend('B', "base", "magneticraft:machine_casing", false);
        GuideRepository.MultiblockGuide guide = new GuideRepository.MultiblockGuide(
                ResourceLocation.fromNamespaceAndPath("magneticraft", "test"),
                "block.magneticraft.test",
                "guide.magneticraft.multiblock.test.description",
                "",
                "processing",
                List.of(List.of("CI", " A"), List.of("BB", "B ")),
                Map.of('C', controller, 'I', ignored, 'A', air, 'B', casing),
                false,
                new GuideRepository.PortSummary(0, 0, false, false, List.of())
        );

        assertEquals(new GuideStructurePreview.Dimensions(2, 2, 2), GuideStructurePreview.dimensions(guide));
        List<GuideStructurePreview.StructureBlock> firstLayer =
                GuideStructurePreview.visibleBlocks(guide, 0);
        assertEquals(2, firstLayer.size());
        assertTrue(firstLayer.stream().allMatch(block -> block.y() == 0));
        assertTrue(firstLayer.stream().noneMatch(block -> block.legend().ignored()));

        List<GuideStructurePreview.StructureBlock> complete =
                GuideStructurePreview.visibleBlocks(guide, 1);
        assertEquals(5, complete.size());
        assertEquals(3, complete.stream().filter(block -> block.y() == 1).count());
    }

    @Test
    void automaticFitKeepsTinyAndLargeStructuresWithinUsefulScaleBounds() {
        float tiny = GuideStructurePreview.fitScale(
                500,
                300,
                new GuideStructurePreview.Dimensions(1, 1, 1)
        );
        float large = GuideStructurePreview.fitScale(
                160,
                100,
                new GuideStructurePreview.Dimensions(11, 7, 11)
        );

        assertEquals(32.0F, tiny);
        assertTrue(large >= 5.0F && large < tiny);
    }

    private static GuideRepository.LegendEntry legend(
            char symbol,
            String rule,
            String block,
            boolean ignored
    ) {
        return new GuideRepository.LegendEntry(
                symbol,
                rule,
                block == null ? null : ResourceLocation.tryParse(block),
                Map.of(),
                ignored
        );
    }
}
