package com.brightcup.recyclerplus;

import com.brightcup.recyclerplus.config.RecyclerRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Recycler Plus —— 给「回收机」添加专属回收产物的附属模组。
 *
 * <p>本模组不改动原 mod 的任何文件，只通过 Mixin 在运行时接管
 * {@code mio_icif_recycler_elc} 的输出判定。
 */
@Mod(RecyclerPlus.MOD_ID)
public final class RecyclerPlus {

    public static final String MOD_ID = "recyclerplus";

    public RecyclerPlus(IEventBus modBus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onServerStarted(ServerStartedEvent event) {
        // 服务器启动后物品注册表与标签已就绪，此时解析配置最安全
        RecyclerRules.load();
    }
}
