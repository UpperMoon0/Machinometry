package com.nstut.machinometry.fabric;

import com.nstut.machinometry.core.GaugeReading;
import com.nstut.machinometry.core.InstrumentKind;
import com.nstut.machinometry.core.MeasurementFormatter;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;

final class GaugeBlockEntity extends BlockEntity {
    private GaugeReading reading = GaugeReading.unavailable();
    private int sampleDelay;

    GaugeBlockEntity(BlockPos pos, BlockState state) {
        super(FabricContent.GAUGE_BLOCK_ENTITY, pos, state);
    }

    GaugeReading reading() { return reading; }

    private InstrumentKind kind() { return FabricContent.kindOf(getBlockState().getBlock()); }
    private Direction targetDirection() { return getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite(); }
    private Direction targetSide() { return targetDirection().getOpposite(); }

    static void serverTick(Level level, BlockPos pos, BlockState state, GaugeBlockEntity blockEntity) {
        if (blockEntity.sampleDelay++ % 5 != 0) return;
        GaugeReading next = blockEntity.sample();
        if (!next.equals(blockEntity.reading)) {
            blockEntity.reading = next;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    String readout() {
        return MeasurementFormatter.gauge(kind(), reading);
    }

    private GaugeReading sample() {
        if (level == null) return GaugeReading.unavailable();
        BlockPos targetPos = worldPosition.relative(targetDirection());
        return switch (kind()) {
            case INVENTORY_GAUGE -> reading(ItemStorage.SIDED.find(level, targetPos, targetSide()));
            case TANK_GAUGE -> reading(FluidStorage.SIDED.find(level, targetPos, targetSide()));
            case ENERGY_GAUGE -> {
                EnergyStorage storage = EnergyStorage.SIDED.find(level, targetPos, targetSide());
                yield storage == null ? GaugeReading.unavailable()
                        : new GaugeReading(storage.getAmount(), storage.getCapacity(), true);
            }
            default -> GaugeReading.unavailable();
        };
    }

    private static <T> GaugeReading reading(Storage<T> storage) {
        if (storage == null) return GaugeReading.unavailable();
        long amount = 0;
        long capacity = 0;
        for (StorageView<T> view : storage) {
            amount = safeAdd(amount, view.getAmount());
            capacity = safeAdd(capacity, view.getCapacity());
        }
        return new GaugeReading(amount, capacity, true);
    }

    private static long safeAdd(long a, long b) {
        if (b <= 0) return a;
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }
}
