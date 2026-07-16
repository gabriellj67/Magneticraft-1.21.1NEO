package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.network.pressure.PressureTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

public final class PressureTankScreen extends AbstractContainerScreen<PressureTankMenu> {
    public PressureTankScreen(PressureTankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = LegacyMachineGuiLayout.STANDARD_WIDTH;
        imageHeight = LegacyMachineGuiLayout.STANDARD_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawPlayerInventory(graphics, leftPos, topPos);
        MachineScreenLayout.drawInset(graphics, leftPos + 10, topPos + 20, 156, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, gasName(), 16, 25, 0x404040, false);
        graphics.drawString(font, Component.translatable(
                "gui.magneticraft.pressure.value",
                formatted(menu.pressureKpa()),
                formatted(menu.pressureKpa() / 100.0D),
                formatted(menu.pressureKpa() * 0.1450377377D)
        ), 16, 37, 0x404040, false);
        graphics.drawString(font, Component.translatable(
                "gui.magneticraft.pressure.amount",
                formatted(menu.gasKpaLiters()),
                formatted(menu.capacityKpaLiters())
        ), 16, 49, 0x404040, false);
        if (menu.fillRatio() >= 0.9D) {
            graphics.drawString(font, Component.translatable("gui.magneticraft.pressure.warning"),
                    16, 61, 0xD02020, false);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private Component gasName() {
        ResourceLocation id = menu.gasId().orElse(null);
        if (id == null) {
            return Component.translatable("gui.magneticraft.pressure.empty");
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
        return fluid == null
                ? Component.literal(id.toString())
                : fluid.getFluidType().getDescription();
    }

    private static String formatted(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
