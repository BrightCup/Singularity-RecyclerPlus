package com.brightcup.recyclerplus.mixin;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 取用父类 {@link AbstractProcessingMachineBlockEntity} 的 protected 成员。
 *
 * <p>这些成员无法用 {@code @Shadow} 取得——{@code @Shadow} 只对目标类自身声明的成员生效，
 * 而 {@code progress} / {@code itemHandler} / {@code finishWork()} / {@code stopWork()}
 * 都声明在父类上。
 */
@Mixin(AbstractProcessingMachineBlockEntity.class)
public interface MachineInternals {

    @Accessor("progress")
    int rp$getProgress();

    @Accessor("progress")
    void rp$setProgress(int value);

    @Accessor("itemHandler")
    MachineItemHandler rp$getItemHandler();

    @Invoker("finishWork")
    void rp$finishWork();

    @Invoker("stopWork")
    void rp$stopWork();
}
