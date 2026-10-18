package com.brightcup.recyclerplus.client;

import com.brightcup.recyclerplus.RecyclerPlus;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 「回收机」的 JEI 分类。
 *
 * <p>原 mod 自带 18 个机器的 JEI 分类，但没有回收机——因为原实现根本没有配方概念。
 * 本分类展示的是 {@code config/recyclerplus.json} 中定义的专属回收规则。
 */
public class RecyclerRecipeCategory implements IRecipeCategory<RecyclerJeiRecipe> {

    public static final RecipeType<RecyclerJeiRecipe> TYPE =
            RecipeType.create(RecyclerPlus.MOD_ID, "recycling", RecyclerJeiRecipe.class);

    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RecyclerPlus.MOD_ID, "textures/gui/arrow.png");

    private static final int WIDTH = 66;
    private static final int HEIGHT = 26;
    private static final int SLOT_Y = 4;
    private static final int INPUT_X = 0;
    private static final int OUTPUT_X = 46;
    private static final int ARROW_X = 22;
    private static final int ARROW_Y = 5;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public RecyclerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.arrow = helper.createDrawable(ARROW_TEXTURE, 0, 0, 24, 16);
        Item recycler = RecyclerJeiPlugin.recyclerItem();
        this.icon = recycler == null
                ? helper.createBlankDrawable(16, 16)
                : helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(recycler));
    }

    @Override
    public RecipeType<RecyclerJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.recyclerplus.recycling");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecyclerJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(INPUT_X, SLOT_Y)
                .setStandardSlotBackground()
                .addIngredients(recipe.input());

        IRecipeSlotBuilder output = builder.addOutputSlot(OUTPUT_X, SLOT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.output());

        if (recipe.chance() < 1.0F) {
            output.addRichTooltipCallback((view, tooltip) -> tooltip.add(
                    Component.translatable("jei.recyclerplus.chance", Math.round(recipe.chance() * 100.0F))
                            .withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public void draw(RecyclerJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        arrow.draw(graphics, ARROW_X, ARROW_Y);
    }

    @Override
    public ResourceLocation getRegistryName(RecyclerJeiRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath(RecyclerPlus.MOD_ID, "recycling/" + recipe.index());
    }
}
