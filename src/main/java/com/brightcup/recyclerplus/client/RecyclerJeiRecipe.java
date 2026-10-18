package com.brightcup.recyclerplus.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI 展示用的回收规则快照。
 *
 * <p>直接由 {@code config/recyclerplus.json} 解析结果构造，与实际运行时行为的唯一依据是同一份配置，
 * 因此 JEI 显示的内容与机器真实产出永远一致。
 *
 * @param index  规则在配置文件中的序号，用于生成稳定的注册名
 * @param input  输入匹配器（支持物品与标签）
 * @param output 产出物品
 * @param chance 产出概率，1.0 表示必定产出
 */
public record RecyclerJeiRecipe(int index, Ingredient input, ItemStack output, float chance) {
}
