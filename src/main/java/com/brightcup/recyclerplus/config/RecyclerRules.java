package com.brightcup.recyclerplus.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 专属回收规则表，来源于 {@code config/recyclerplus.json}。
 *
 * <p>匹配采用「先命中先用」：按配置文件中的书写顺序依次判定，第一条命中的规则生效。
 * 没有命中任何规则的物品，会回落原 mod 的默认行为（12.5% 产出废料）。
 */
public final class RecyclerRules {

    private static final Logger LOG = LoggerFactory.getLogger("RecyclerPlus");
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("recyclerplus.json");

    private static final String DEFAULT_JSON = """
            {
              "_comment": "input 支持物品ID 或 #标签；output 为物品ID；count 1-64；chance 0~1。未命中的物品仍走原版废料逻辑。",
              "_example": { "input": "minecraft:diamond_pickaxe", "output": "minecraft:diamond", "count": 1, "chance": 1.0 },
              "rules": []
            }
            """;

    /** 一条规则：输入匹配器 + 产出 + 产出概率。 */
    public record Rule(Ingredient input, ItemStack output, float chance) {}

    private static volatile List<Rule> rules = List.of();

    private RecyclerRules() {
    }

    public static void load() {
        try {
            if (Files.notExists(FILE)) {
                Path parent = FILE.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(FILE, DEFAULT_JSON, StandardCharsets.UTF_8);
                LOG.info("[RecyclerPlus] 已生成默认配置 {}", FILE);
            }

            JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Rule> parsed = new ArrayList<>();
            JsonElement rulesElement = root.get("rules");
            if (rulesElement != null && rulesElement.isJsonArray()) {
                JsonArray array = rulesElement.getAsJsonArray();
                for (JsonElement element : array) {
                    if (element != null && element.isJsonObject()) {
                        parseRule(element.getAsJsonObject(), parsed);
                    }
                }
            }
            rules = List.copyOf(parsed);
            LOG.info("[RecyclerPlus] 已加载 {} 条专属回收规则", rules.size());
        } catch (Exception e) {
            LOG.error("[RecyclerPlus] 配置读取失败 {}", FILE, e);
        }
    }

    private static void parseRule(JsonObject object, List<Rule> output) {
        if (!object.has("input") || !object.has("output")) {
            LOG.warn("[RecyclerPlus] 规则缺少 input/output 字段，已跳过: {}", object);
            return;
        }

        String inputText = object.get("input").getAsString().trim();
        String outputText = object.get("output").getAsString().trim();
        int count = object.has("count") ? object.get("count").getAsInt() : 1;
        float chance = object.has("chance") ? object.get("chance").getAsFloat() : 1.0F;

        Item outputItem = resolveItem(outputText);
        if (outputItem == null) {
            LOG.warn("[RecyclerPlus] 未知的产出物品 {}，已跳过", outputText);
            return;
        }

        Ingredient input;
        if (inputText.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(inputText.substring(1));
            if (tagId == null) {
                LOG.warn("[RecyclerPlus] 非法的标签 ID {}，已跳过", inputText);
                return;
            }
            input = Ingredient.of(TagKey.create(Registries.ITEM, tagId));
        } else {
            Item inputItem = resolveItem(inputText);
            if (inputItem == null) {
                LOG.warn("[RecyclerPlus] 未知的输入物品 {}，已跳过", inputText);
                return;
            }
            input = Ingredient.of(inputItem);
        }

        ItemStack stack = new ItemStack(outputItem, Math.max(1, Math.min(64, count)));
        output.add(new Rule(input, stack, Math.max(0.0F, Math.min(1.0F, chance))));
    }

    @Nullable
    private static Item resolveItem(String text) {
        ResourceLocation id = ResourceLocation.tryParse(text);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(id);
    }

    /**
     * 当前生效的规则快照（不可变），按配置文件中的书写顺序。
     *
     * <p>供 JEI 等展示层读取，保证「列表里显示的」与「机器真实执行的」是同一份数据。
     */
    public static List<Rule> all() {
        return rules;
    }

    /**
     * 只判断是否命中规则，并给出产出，不消耗随机数。
     * 用于输出槽容量判断，避免每个 tick 都掷一次骰子。
     *
     * @return 未命中返回 {@code null}；命中且概率为 0 时返回空栈（表示不需要输出空间）；
     *         其余情况返回产出副本
     */
    @Nullable
    public static ItemStack preview(ItemStack stack) {
        for (Rule rule : rules) {
            if (!rule.input().test(stack)) {
                continue;
            }
            return rule.chance() <= 0.0F ? ItemStack.EMPTY : rule.output().copy();
        }
        return null;
    }

    /**
     * 命中规则后掷骰决定是否产出。
     *
     * @return {@code null} 表示没有命中规则（调用方应回落原逻辑）；
     *         空栈表示命中但概率未通过（调用方应只消耗输入）；
     *         非空栈表示应当产出该物品
     */
    @Nullable
    public static ItemStack roll(ItemStack stack, RandomSource random) {
        for (Rule rule : rules) {
            if (!rule.input().test(stack)) {
                continue;
            }
            return random.nextFloat() < rule.chance() ? rule.output().copy() : ItemStack.EMPTY;
        }
        return null;
    }
}
