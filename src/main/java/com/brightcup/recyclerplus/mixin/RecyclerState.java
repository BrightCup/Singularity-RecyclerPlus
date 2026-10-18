package com.brightcup.recyclerplus.mixin;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_recycler_elc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 取用回收机自身的私有字段 {@code scexRecycleOutcome}。
 *
 * <p>该字段缓存「本次回收是否命中产出」的掷骰结果。我们在 {@code finishRecycling}
 * 头部接管后不再走原逻辑，需要主动清回 -1，避免残留值影响后续走原逻辑的物品。
 */
@Mixin(mio_icif_recycler_elc.class)
public interface RecyclerState {

    @Accessor("scexRecycleOutcome")
    void rp$setOutcome(int value);
}
