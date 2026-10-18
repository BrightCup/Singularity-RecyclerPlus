package com.brightcup.recyclerplus.mixin;

import com.brightcup.recyclerplus.config.RecyclerRules;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_recycler_elc;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 接管回收机的两个私有方法。
 *
 * <ul>
 *   <li>{@code hasOutputSpace()}：原实现写死判断「废料能否塞进输出槽」。
 *       专属产物不是废料，不接管会导致 {@code canWork()} 恒为 false，机器干脆不启动。</li>
 *   <li>{@code finishRecycling()}：原实现写死 12.5% 产出废料。命中配置规则的物品在这里
 *       走自定义产出，并把 {@code scexRecycleOutcome} 一并清回 -1。</li>
 * </ul>
 *
 * <p>两个注入点都只在「命中配置规则」时取消原逻辑，未命中的物品完全保持原版行为。
 */
@Mixin(mio_icif_recycler_elc.class)
public abstract class RecyclerElcMixin {

    @Inject(method = "hasOutputSpace", at = @At("HEAD"), cancellable = true)
    private void rp$hasOutputSpaceForCustomResult(CallbackInfoReturnable<Boolean> cir) {
        ItemStack result = rp$previewResult();
        if (result == null) {
            return;
        }
        if (result.isEmpty()) {
            // 概率为 0 的规则只消耗输入，不需要输出空间
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(RecipeSlots.prepare(
                rp$internals().rp$getItemHandler(),
                mio_icif_recycler_elc.INPUT_SLOT, 1,
                new int[]{mio_icif_recycler_elc.OUTPUT_SLOT},
                List.of(result)
        ).isPresent());
    }

    @Inject(method = "finishRecycling", at = @At("HEAD"), cancellable = true)
    private void rp$finishWithCustomResult(CallbackInfo ci) {
        ItemStack rolled = rp$rollResult();
        if (rolled == null) {
            // 没有命中专属规则，交回原逻辑处理（12.5% 废料）
            return;
        }

        MachineInternals internals = rp$internals();
        var operation = rolled.isEmpty()
                ? RecipeSlots.consumeOnly(internals.rp$getItemHandler(), mio_icif_recycler_elc.INPUT_SLOT, 1)
                : RecipeSlots.prepare(
                        internals.rp$getItemHandler(),
                        mio_icif_recycler_elc.INPUT_SLOT, 1,
                        new int[]{mio_icif_recycler_elc.OUTPUT_SLOT},
                        List.of(rolled));

        if (operation.isEmpty()) {
            internals.rp$stopWork();
            ci.cancel();
            return;
        }

        int completed = internals.rp$getProgress();
        internals.rp$setProgress(0);
        ((RecyclerState) (Object) this).rp$setOutcome(-1);

        if (!operation.get().commit()) {
            internals.rp$setProgress(completed);
            internals.rp$stopWork();
            ci.cancel();
            return;
        }

        internals.rp$finishWork();
        rp$self().setChanged();
        ci.cancel();
    }

    private MachineInternals rp$internals() {
        return (MachineInternals) (Object) this;
    }

    private mio_icif_recycler_elc rp$self() {
        return (mio_icif_recycler_elc) (Object) this;
    }

    @Nullable
    private ItemStack rp$previewResult() {
        if (rp$self().getLevel() == null) {
            return null;
        }
        ItemStack input = rp$internals().rp$getItemHandler()
                .getStackInSlot(mio_icif_recycler_elc.INPUT_SLOT);
        return input.isEmpty() ? null : RecyclerRules.preview(input);
    }

    @Nullable
    private ItemStack rp$rollResult() {
        if (rp$self().getLevel() == null) {
            return null;
        }
        ItemStack input = rp$internals().rp$getItemHandler()
                .getStackInSlot(mio_icif_recycler_elc.INPUT_SLOT);
        return input.isEmpty() ? null : RecyclerRules.roll(input, rp$self().getLevel().random);
    }
}
