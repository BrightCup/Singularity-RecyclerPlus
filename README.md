# Recycler Plus

给 [Singularity Iteration](https://github.com/2132937983/Singularity-Iteration) 的「回收机」添加**专属回收产物**的附属模组。

原 mod 的回收机是照 IC2 原版实现的：任意物品进去，12.5% 概率产出废料，其余情况直接消耗掉，**完全不读取配方系统**。本模组不改动原 mod 的任何文件，只通过 Mixin 在运行时接管它的输出判定：

- 命中 `config/recyclerplus.json` 的物品 → 按配置产出（可设概率）
- 未命中的物品 → 完全保持原版行为（12.5% 出废料）

## 环境

| 项目 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.218+ |
| 原 mod | Singularity Iteration（modid `mio_icif`） |
| Java | 21 |

## 构建

1. 先构建原 mod，拿到它的 jar：

   ```bash
   git clone https://github.com/2132937983/Singularity-Iteration.git
   cd Singularity-Iteration
   ./gradlew build
   ```

2. 把产物复制到本工程的 `libs/mio_icif.jar`（文件名必须一致，或改 `build.gradle` 里的路径）：

   ```bash
   cp build/libs/mio_icif-*.jar /path/to/Singularity-RecyclerPlus/libs/mio_icif.jar
   ```

3. 构建本模组：

   ```bash
   ./gradlew build
   # 或直接用 IntelliJ IDEA 打开，Gradle 会自动配置
   ```

   产物在 `build/libs/recyclerplus-1.0.0.jar`。

> 仓库里没有附带 Gradle Wrapper 的 jar，用 IDEA 打开会自动配置 Gradle；若要用命令行，先执行一次 `gradle wrapper --gradle-version 8.10.2`。

## 使用

把 `recyclerplus-1.0.0.jar` 和原 mod 一起丢进 `mods/`。首次启动会在 `config/recyclerplus.json` 生成模板：

```json
{
  "_comment": "input 支持物品ID 或 #标签；output 为物品ID；count 1-64；chance 0~1。未命中的物品仍走原版废料逻辑。",
  "_example": { "input": "minecraft:diamond_pickaxe", "output": "minecraft:diamond", "count": 1, "chance": 1.0 },
  "rules": []
}
```

填 `rules` 即可，例如：

```json
{
  "rules": [
    { "input": "minecraft:diamond_pickaxe", "output": "minecraft:diamond", "count": 2, "chance": 1.0 },
    { "input": "minecraft:iron_pickaxe",    "output": "minecraft:iron_ingot",  "count": 1, "chance": 0.5 },
    { "input": "#minecraft:wool",           "output": "minecraft:string",      "count": 4, "chance": 1.0 }
  ]
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| `input` | `命名空间:物品ID`，或以 `#` 开头的物品标签 |
| `output` | 产出物品 ID |
| `count` | 产出数量，1–64，缺省 1 |
| `chance` | 产出概率 0–1，缺省 1.0。未通过概率时只消耗输入；设为 0 即「只销毁、不产出」 |
| 匹配顺序 | 按数组顺序，**第一条命中即生效** |

改完配置需要**重启服务器**（配置在 `ServerStartedEvent` 阶段解析）。

## 实现说明

| 文件 | 作用 |
|---|---|
| `config/RecyclerRules.java` | 读取并解析 `config/recyclerplus.json`，提供 `preview` / `roll` 两个查询入口 |
| `mixin/MachineInternals.java` | 取父类 `AbstractProcessingMachineBlockEntity` 的 `progress` / `itemHandler` / `finishWork()` / `stopWork()` |
| `mixin/RecyclerState.java` | 取回收机私有字段 `scexRecycleOutcome`，用于清缓存 |
| `mixin/RecyclerElcMixin.java` | 注入 `hasOutputSpace()` 与 `finishRecycling()` |

几个必须注意的点：

- **`hasOutputSpace()` 必须一起接管。** 原实现只判断废料能否进输出槽；不接管的话，输出槽里放着专属产物时 `canWork()` 会恒为 false，机器根本不启动。
- **`@Shadow` 拿不到这些成员。** `progress`、`itemHandler`、`finishWork()`、`stopWork()` 都声明在父类上，只能用 accessor / invoker 接口打在父类。
- **`chance` 不能走原 mod 的 `registerRecyclerRecipe()`。** 它的 `RecyclerRecipe` 构造器会把第 6 个参数直接丢弃，概率不生效，因此本模组自己读配置文件。

## 兼容性风险

Mixin 绑定的是原 mod 的两个**私有方法名**（`hasOutputSpace`、`finishRecycling`）和两个字段名（`progress`、`itemHandler`、`scexRecycleOutcome`）。原 mod 一旦重构这些成员，本模组会启动报错（`injectors.defaultRequire = 1`，不会静默失效）。**升级原 mod 后请先跑一次客户端/服务端启动验证。**

## License

MIT
