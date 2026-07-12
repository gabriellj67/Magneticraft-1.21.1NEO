package committee.nova.mods.magneticraft.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

/** Shared presentation shell for Magneticraft's JEI recipe categories. */
abstract class AbstractMagneticraftRecipeCategory<T> implements IRecipeCategory<T> {
    private final RecipeType<T> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final int width;
    private final int height;

    protected AbstractMagneticraftRecipeCategory(
            RecipeType<T> recipeType,
            Component title,
            ItemLike icon,
            IGuiHelper guiHelper,
            int width,
            int height
    ) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemLike(icon);
        this.arrow = guiHelper.getRecipeArrow();
        this.width = width;
        this.height = height;
    }

    @Override
    public final RecipeType<T> getRecipeType() {
        return recipeType;
    }

    @Override
    public final Component getTitle() {
        return title;
    }

    @Override
    public final IDrawable getIcon() {
        return icon;
    }

    @Override
    public final int getWidth() {
        return width;
    }

    @Override
    public final int getHeight() {
        return height;
    }

    protected final void drawArrow(GuiGraphics graphics, int x, int y) {
        arrow.draw(graphics, x, y);
    }

    protected static void drawLine(GuiGraphics graphics, Component text, int x, int y) {
        graphics.drawString(Minecraft.getInstance().font, text, x, y, 0xFF404040, false);
    }
}
