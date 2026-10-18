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

### 为什么需要原 mod 的 jar

本模组的代码直接引用了原 mod 的类（`mio_icif_recycler_elc`、`RecipeSlots`、`MachineItemHandler`…），
所以 **javac 在编译期必须能看到这些类**，否则连编译都过不去。

`build.gradle` 里写的是 `compileOnly files('libs/mio_icif.jar')`，含义是：

- 编译时：从该 jar 读取类型信息
- 打包时：**不**把原 mod 的类打进本模组的 jar
- 运行时：由玩家 `mods/` 目录里的原 mod 提供

所以需要的是一个**已编译好的 jar**，不是源码工程，也不需要你重新构建别人的模组。

### 步骤

1. 拿到原 mod 的 jar，两种方式任选：

   **A. 直接用已发布的 jar（推荐）**

   如果你已经装了包含该 mod 的整合包，直接从 `mods/` 目录复制即可：

   ```bash
   cp "[SI] SingularityIteration-<版本>-beta.jar" libs/mio_icif.jar
   ```

   也可以从整合包或 mod 发布页下载。

   **B. 从源码构建**（仅当需要针对未发布的新版本时）

   ```bash
   git clone https://github.com/2132937983/Singularity-Iteration.git
   cd Singularity-Iteration && ./gradlew build
   cp build/libs/*.jar /path/to/Singularity-RecyclerPlus/libs/mio_icif.jar
   ```

2. 文件名必须为 `libs/mio_icif.jar`（或自行修改 `build.gradle` 里的路径）。
   该文件已被 `.gitignore` 忽略，不会提交到版本库。

3. 构建本模组：

   ```bash
   ./gradlew build
   # 或直接用 IntelliJ IDEA 打开，Gradle 会自动配置
   ```

   产物在 `build/libs/recyclerplus-1.0.0.jar`。

> 需要 **JDK 21**（MC 1.21.1 的编译目标）。

### 用 VSCode 构建

不需要 IntelliJ IDEA，VSCode 完全可以。

1. 装扩展 **Extension Pack for Java**（`vscjava.vscode-java-pack`），它会带上 Gradle for Java。
2. 用 VSCode 打开本工程目录，等右下角 Gradle 导入完成（首次会下载依赖，较慢）。
3. 直接按 `Ctrl+Shift+B`（或终端里跑 `./gradlew build`）。命令行和 IDE 走的是同一套 Gradle，结果完全一致。

如果 VSCode 报 `No matching toolchains found for Java 21`，说明它没找到 JDK 21。两种解法：

- 在 `settings.json` 里指定：`"java.import.gradle.java.home": "C:/Program Files/Java/<你的 JDK21>"`；
- 或设置环境变量 `JAVA_HOME` 指向 JDK 21 后重启 VSCode。

### 关于 Gradle Wrapper

仓库已自带 wrapper（`gradlew`、`gradlew.bat`、`gradle/wrapper/`），无需本机安装 Gradle。

`gradle-wrapper.properties` 里默认用腾讯云镜像：

```
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.10.2-bin.zip
```

原因是官方 `services.gradle.org` 会 307 跳转到 GitHub 的 `release-assets.githubusercontent.com`，
该域名可能不在 JDK 的 `cacerts` 里，会抛 `PKIX path building failed`。网络可达时也可以改回官方地址。

### 构建卡在 maven.neoforged.net（常见）

如果报错形如：

```
Could not resolve net.neoforged:neoform-runtime:...
  > Got socket exception during request ... Connection reset
```

说明连不上 NeoForge 的 Maven 仓库。这个域名在国内经常不通，和 IDE 无关。

注意 **Java 不读 `HTTP_PROXY` / `HTTPS_PROXY` 环境变量**，代理必须单独告诉 Gradle。
在 `%USERPROFILE%\.gradle\gradle.properties`（Linux/macOS 为 `~/.gradle/gradle.properties`）里加：

```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

端口换成你自己代理软件的端口。若代理软件开了 TUN/全局模式，则不需要这段配置。

### 已校验的版本

本模组的注入点已对照 `[SI] SingularityIteration-0.1.7.35.1-beta.jar` 用 `javap` 逐个核对，
以下成员在字节码层面确认存在且签名一致：

| 成员 | 实际签名 |
|---|---|
| `mio_icif_recycler_elc#hasOutputSpace` | `private boolean` |
| `mio_icif_recycler_elc#finishRecycling` | `private void` |
| `mio_icif_recycler_elc#scexRecycleOutcome` | `private int` |
| `AbstractProcessingMachineBlockEntity#progress` / `#itemHandler` | `protected` |
| `AbstractProcessingMachineBlockEntity#stopWork` / `#finishWork` | `protected void` |
| `RecipeSlots#prepare` | `(MachineItemHandler,int,int,int[],List<ItemStack>) → Optional<Prepared>` |
| `RecipeSlots.Prepared#commit` | `public boolean` |

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
