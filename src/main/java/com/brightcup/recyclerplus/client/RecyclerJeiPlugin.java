package com.brightcup.recyclerplus.client;

import com.brightcup.recyclerplus.RecyclerPlus;
import com.brightcup.recyclerplus.config.RecyclerRules;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 把 {@code config/recyclerplus.json} 中的专属回收规则注册进 JEI。
 *
 * <p>原 mod 虽然有 {@code RecyclerRecipe} 与 {@code registerRecyclerRecipe()}，
 * 但既没有任何配方实例，也没有回收机的 JEI 分类，所以 JEI 里什么都查不到。
 * 本插件直接读配置文件，自建分类展示。
 */
@JeiPlugin
public class RecyclerJeiPlugin implements IModPlugin {

    /** 原 mod 的回收机方块，注册名在 {@code mio_icif_blocks} 中写作 "producer/block_recycler_elc"。 */
    private static final ResourceLocation RECYCLER_ID =
            ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_recycler_elc");

    /** @return 回收机物品；原 mod 未加载时返回 {@code null} */
    @Nullable
    public static Item recyclerItem() {
        Item item = BuiltInRegistries.ITEM.get(RECYCLER_ID);
        return item == Items.AIR ? null : item;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(RecyclerPlus.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new RecyclerRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<RecyclerJeiRecipe> recipes = new ArrayList<>();
        int index = 0;
        for (RecyclerRules.Rule rule : RecyclerRules.all()) {
            recipes.add(new RecyclerJeiRecipe(index++, rule.input(), rule.output().copy(), rule.chance()));
        }
        registration.addRecipes(RecyclerRecipeCategory.TYPE, recipes);

        Item recycler = recyclerItem();
        if (recycler != null) {
            registration.addIngredientInfo(new ItemStack(recycler), VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.recyclerplus.info"));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        Item recycler = recyclerItem();
        if (recycler != null) {
            registration.addRecipeCatalysts(RecyclerRecipeCategory.TYPE, recycler);
        }
    }
}
