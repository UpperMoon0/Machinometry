package com.nstut.machinometry.fabric;

import com.nstut.machinometry.core.InstrumentKind;
import com.nstut.machinometry.core.MeasurementFormatter;
import com.nstut.machinometry.core.RateTracker;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;

import java.util.Collections;
import java.util.Iterator;

final class FlowMeterBlockEntity extends BlockEntity {
    private final RateTracker rateTracker = new RateTracker();
    private final Storage<ItemVariant> itemInput = new ItemProxy(true);
    private final Storage<ItemVariant> itemOutput = new ItemProxy(false);
    private final Storage<FluidVariant> fluidInput = new FluidProxy(true);
    private final Storage<FluidVariant> fluidOutput = new FluidProxy(false);
    private final EnergyStorage energyInput = new EnergyProxy(true);
    private final EnergyStorage energyOutput = new EnergyProxy(false);

    FlowMeterBlockEntity(BlockPos pos, BlockState state) {
        super(FabricContent.FLOW_METER_BLOCK_ENTITY, pos, state);
    }

    private InstrumentKind kind() { return FabricContent.kindOf(getBlockState().getBlock()); }
    private Direction outputDirection() { return getBlockState().getValue(HorizontalDirectionalBlock.FACING); }
    private Direction inputDirection() { return outputDirection().getOpposite(); }

    static void serverTick(Level level, BlockPos pos, BlockState state, FlowMeterBlockEntity blockEntity) {
        blockEntity.rateTracker.tick();
    }

    String readout() {
        return MeasurementFormatter.flow(kind(), rateTracker);
    }

    Storage<ItemVariant> itemStorage(Direction side) {
        if (kind() != InstrumentKind.ITEM_FLOW || side == null) return null;
        if (side == inputDirection()) return itemInput;
        if (side == outputDirection()) return itemOutput;
        return null;
    }

    Storage<FluidVariant> fluidStorage(Direction side) {
        if (kind() != InstrumentKind.FLUID_FLOW || side == null) return null;
        if (side == inputDirection()) return fluidInput;
        if (side == outputDirection()) return fluidOutput;
        return null;
    }

    EnergyStorage energyStorage(Direction side) {
        if (kind() != InstrumentKind.ENERGY_FLOW || side == null) return null;
        if (side == inputDirection()) return energyInput;
        if (side == outputDirection()) return energyOutput;
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("TotalTransferred", rateTracker.total());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        rateTracker.restoreTotal(tag.getLong("TotalTransferred"));
        rateTracker.resetRates();
    }

    private void record(long amount) {
        if (amount <= 0) return;
        rateTracker.recordCommitted(amount);
        setChanged();
    }

    private void recordOnCommit(TransactionContext transaction, long amount) {
        if (amount <= 0) return;
        transaction.addOuterCloseCallback(result -> {
            if (result.wasCommitted()) record(amount);
        });
    }

    private Storage<ItemVariant> itemTarget(boolean inputSide) {
        if (level == null) return null;
        Direction direction = inputSide ? outputDirection() : inputDirection();
        return ItemStorage.SIDED.find(level, worldPosition.relative(direction), direction.getOpposite());
    }

    private Storage<FluidVariant> fluidTarget(boolean inputSide) {
        if (level == null) return null;
        Direction direction = inputSide ? outputDirection() : inputDirection();
        return FluidStorage.SIDED.find(level, worldPosition.relative(direction), direction.getOpposite());
    }

    private EnergyStorage energyTarget(boolean inputSide) {
        if (level == null) return null;
        Direction direction = inputSide ? outputDirection() : inputDirection();
        return EnergyStorage.SIDED.find(level, worldPosition.relative(direction), direction.getOpposite());
    }

    private final class ItemProxy implements Storage<ItemVariant> {
        private final boolean inputSide;
        private ItemProxy(boolean inputSide) { this.inputSide = inputSide; }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (!inputSide) return 0;
            Storage<ItemVariant> target = itemTarget(true);
            if (target == null) return 0;
            long moved = target.insert(resource, maxAmount, transaction);
            recordOnCommit(transaction, moved);
            return moved;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (inputSide) return 0;
            Storage<ItemVariant> target = itemTarget(false);
            if (target == null) return 0;
            long moved = target.extract(resource, maxAmount, transaction);
            recordOnCommit(transaction, moved);
            return moved;
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            Storage<ItemVariant> target = itemTarget(inputSide);
            return target == null ? Collections.emptyIterator() : target.iterator();
        }
    }

    private final class FluidProxy implements Storage<FluidVariant> {
        private final boolean inputSide;
        private FluidProxy(boolean inputSide) { this.inputSide = inputSide; }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (!inputSide) return 0;
            Storage<FluidVariant> target = fluidTarget(true);
            if (target == null) return 0;
            long moved = target.insert(resource, maxAmount, transaction);
            recordOnCommit(transaction, moved);
            return moved;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (inputSide) return 0;
            Storage<FluidVariant> target = fluidTarget(false);
            if (target == null) return 0;
            long moved = target.extract(resource, maxAmount, transaction);
            recordOnCommit(transaction, moved);
            return moved;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            Storage<FluidVariant> target = fluidTarget(inputSide);
            return target == null ? Collections.emptyIterator() : target.iterator();
        }
    }

    private final class EnergyProxy implements EnergyStorage {
        private final boolean inputSide;
        private EnergyProxy(boolean inputSide) { this.inputSide = inputSide; }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            if (!inputSide) return 0;
            EnergyStorage target = energyTarget(true);
            if (target == null) return 0;
            long moved = target.insert(maxAmount, transaction);
            recordOnCommit(transaction, moved);
            return moved;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            if (inputSide) return 0;
            EnergyStorage target = energyTarget(false);
            if (target == null) return 0;
            long moved = target.extract(maxAmount, transaction);
            recordOnCommit(transaction, moved);
            return moved;
        }

        @Override public long getAmount() { EnergyStorage target = energyTarget(inputSide); return target == null ? 0 : target.getAmount(); }
        @Override public long getCapacity() { EnergyStorage target = energyTarget(inputSide); return target == null ? 0 : target.getCapacity(); }
        @Override public boolean supportsInsertion() { EnergyStorage target = energyTarget(true); return inputSide && target != null && target.supportsInsertion(); }
        @Override public boolean supportsExtraction() { EnergyStorage target = energyTarget(false); return !inputSide && target != null && target.supportsExtraction(); }
    }
}
