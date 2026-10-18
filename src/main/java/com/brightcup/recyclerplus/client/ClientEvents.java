package com.brightcup.recyclerplus.client;

import com.brightcup.recyclerplus.RecyclerPlus;
import com.brightcup.recyclerplus.config.RecyclerRules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * 客户端侧的配置加载。
 *
 * <p>单机时集成服务端会触发 {@code ServerStartedEvent}，同一 JVM 内配置已经就绪；
 * 但连别人的服务器时客户端不会收到该事件，JEI 就会读到空规则表。
 * 因此这里在玩家登录客户端世界时再读一次本地配置。
 */
@EventBusSubscriber(modid = RecyclerPlus.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        RecyclerRules.load();
    }
}
