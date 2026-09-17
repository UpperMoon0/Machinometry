package com.nstut.machinometry.forge;

import com.nstut.machinometry.core.GaugeReading;
import com.nstut.machinometry.core.InstrumentKind;
import com.nstut.machinometry.core.MeasurementFormatter;
import com.nstut.machinometry.core.RateTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod(MachinometryForge.MOD_ID)
public final class MachinometryForge {
    public static final String MOD_ID = "machinometry";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<Block> ITEM_FLOW_METER = registerBlock(InstrumentKind.ITEM_FLOW);
    public static final RegistryObject<Block> FLUID_FLOW_METER = registerBlock(InstrumentKind.FLUID_FLOW);
    public static final RegistryObject<Block> ENERGY_FLOW_METER = registerBlock(InstrumentKind.ENERGY_FLOW);
    public static final RegistryObject<Block> INVENTORY_GAUGE = registerBlock(InstrumentKind.INVENTORY_GAUGE);
    public static final RegistryObject<Block> TANK_GAUGE = registerBlock(InstrumentKind.TANK_GAUGE);
    public static final RegistryObject<Block> ENERGY_GAUGE = registerBlock(InstrumentKind.ENERGY_GAUGE);

    public static final RegistryObject<BlockEntityType<FlowMeterBlockEntity>> FLOW_METER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "flow_meter",
            () -> BlockEntityType.Builder.of(FlowMeterBlockEntity::new,
                    ITEM_FLOW_METER.get(), FLUID_FLOW_METER.get(), ENERGY_FLOW_METER.get()).build(null));
    public static final RegistryObject<BlockEntityType<GaugeBlockEntity>> GAUGE_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "gauge",
            () -> BlockEntityType.Builder.of(GaugeBlockEntity::new,
                    INVENTORY_GAUGE.get(), TANK_GAUGE.get(), ENERGY_GAUGE.get()).build(null));

    public MachinometryForge() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    private static RegistryObject<Block> registerBlock(InstrumentKind kind) {
        RegistryObject<Block> block = BLOCKS.register(kind.id(), () -> new InstrumentBlock(kind,
                BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f, 6.0f)));
        ITEMS.register(kind.id(), () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static InstrumentKind kindOf(Block block) {
        if (block == ITEM_FLOW_METER.get()) return InstrumentKind.ITEM_FLOW;
        if (block == FLUID_FLOW_METER.get()) return InstrumentKind.FLUID_FLOW;
        if (block == ENERGY_FLOW_METER.get()) return InstrumentKind.ENERGY_FLOW;
        if (block == INVENTORY_GAUGE.get()) return InstrumentKind.INVENTORY_GAUGE;
        if (block == TANK_GAUGE.get()) return InstrumentKind.TANK_GAUGE;
        if (block == ENERGY_GAUGE.get()) return InstrumentKind.ENERGY_GAUGE;
        throw new IllegalStateException("Unknown Machinometry block: " + block);
    }

    public static final class InstrumentBlock extends BaseEntityBlock {
        private final InstrumentKind kind;

        private InstrumentBlock(InstrumentKind kind, Properties properties) {
            super(properties);
            this.kind = kind;
            registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HorizontalDirectionalBlock.FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return kind.isFlowMeter() ? new FlowMeterBlockEntity(pos, state) : new GaugeBlockEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide) return null;
            if (kind.isFlowMeter()) {
                return createTickerHelper(type, FLOW_METER_BLOCK_ENTITY.get(), FlowMeterBlockEntity::serverTick);
            }
            return createTickerHelper(type, GAUGE_BLOCK_ENTITY.get(), GaugeBlockEntity::serverTick);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof FlowMeterBlockEntity meter) {
                    player.displayClientMessage(Component.literal(meter.readout()), false);
                } else if (blockEntity instanceof GaugeBlockEntity gauge) {
                    player.displayClientMessage(Component.literal(gauge.readout()), false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public boolean hasAnalogOutputSignal(BlockState state) {
            return kind.isGauge();
        }

        @Override
        public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            return blockEntity instanceof GaugeBlockEntity gauge ? gauge.reading.comparatorSignal() : 0;
        }
    }

    public static final class FlowMeterBlockEntity extends BlockEntity {
        private final RateTracker rateTracker = new RateTracker();
        private final IItemHandler itemInput = new ItemProxy(true);
        private final IItemHandler itemOutput = new ItemProxy(false);
        private final IFluidHandler fluidInput = new FluidProxy(true);
        private final IFluidHandler fluidOutput = new FluidProxy(false);
        private final IEnergyStorage energyInput = new EnergyProxy(true);
        private final IEnergyStorage energyOutput = new EnergyProxy(false);
        private LazyOptional<IItemHandler> itemInputCap = LazyOptional.of(() -> itemInput);
        private LazyOptional<IItemHandler> itemOutputCap = LazyOptional.of(() -> itemOutput);
        private LazyOptional<IFluidHandler> fluidInputCap = LazyOptional.of(() -> fluidInput);
        private LazyOptional<IFluidHandler> fluidOutputCap = LazyOptional.of(() -> fluidOutput);
        private LazyOptional<IEnergyStorage> energyInputCap = LazyOptional.of(() -> energyInput);
        private LazyOptional<IEnergyStorage> energyOutputCap = LazyOptional.of(() -> energyOutput);

        public FlowMeterBlockEntity(BlockPos pos, BlockState state) {
            super(FLOW_METER_BLOCK_ENTITY.get(), pos, state);
        }

        private InstrumentKind kind() { return kindOf(getBlockState().getBlock()); }
        private Direction outputDirection() { return getBlockState().getValue(HorizontalDirectionalBlock.FACING); }
        private Direction inputDirection() { return outputDirection().getOpposite(); }

        private void record(long amount) {
            if (amount <= 0) return;
            rateTracker.recordCommitted(amount);
            setChanged();
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, FlowMeterBlockEntity blockEntity) {
            blockEntity.rateTracker.tick();
        }

        public String readout() {
            return MeasurementFormatter.flow(kind(), rateTracker);
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

        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            if (side != null) {
                boolean input = side == inputDirection();
                boolean output = side == outputDirection();
                InstrumentKind kind = kind();
                if (kind == InstrumentKind.ITEM_FLOW && capability == ForgeCapabilities.ITEM_HANDLER) {
                    if (input) return itemInputCap.cast();
                    if (output) return itemOutputCap.cast();
                }
                if (kind == InstrumentKind.FLUID_FLOW && capability == ForgeCapabilities.FLUID_HANDLER) {
                    if (input) return fluidInputCap.cast();
                    if (output) return fluidOutputCap.cast();
                }
                if (kind == InstrumentKind.ENERGY_FLOW && capability == ForgeCapabilities.ENERGY) {
                    if (input) return energyInputCap.cast();
                    if (output) return energyOutputCap.cast();
                }
            }
            return super.getCapability(capability, side);
        }

        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            itemInputCap.invalidate(); itemOutputCap.invalidate();
            fluidInputCap.invalidate(); fluidOutputCap.invalidate();
            energyInputCap.invalidate(); energyOutputCap.invalidate();
        }

        private <T> T neighbor(Capability<T> capability, Direction direction) {
            if (level == null) return null;
            BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (blockEntity == null || blockEntity == this) return null;
            return blockEntity.getCapability(capability, direction.getOpposite()).orElse(null);
        }

        private final class ItemProxy implements IItemHandler {
            private final boolean inputSide;
            private ItemProxy(boolean inputSide) { this.inputSide = inputSide; }
            private IItemHandler target() { return neighbor(ForgeCapabilities.ITEM_HANDLER, inputSide ? outputDirection() : inputDirection()); }
            @Override public int getSlots() { IItemHandler h = target(); return h == null ? 0 : h.getSlots(); }
            @Override public net.minecraft.world.item.ItemStack getStackInSlot(int slot) { IItemHandler h = target(); return h == null || slot < 0 || slot >= h.getSlots() ? net.minecraft.world.item.ItemStack.EMPTY : h.getStackInSlot(slot); }
            @Override public net.minecraft.world.item.ItemStack insertItem(int slot, net.minecraft.world.item.ItemStack stack, boolean simulate) {
                if (!inputSide) return stack;
                IItemHandler h = target();
                if (h == null || slot < 0 || slot >= h.getSlots()) return stack;
                net.minecraft.world.item.ItemStack remainder = h.insertItem(slot, stack, simulate);
                if (!simulate) record(stack.getCount() - remainder.getCount());
                return remainder;
            }
            @Override public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (inputSide) return net.minecraft.world.item.ItemStack.EMPTY;
                IItemHandler h = target();
                if (h == null || slot < 0 || slot >= h.getSlots()) return net.minecraft.world.item.ItemStack.EMPTY;
                net.minecraft.world.item.ItemStack extracted = h.extractItem(slot, amount, simulate);
                if (!simulate) record(extracted.getCount());
                return extracted;
            }
            @Override public int getSlotLimit(int slot) { IItemHandler h = target(); return h == null || slot < 0 || slot >= h.getSlots() ? 0 : h.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) { IItemHandler h = target(); return inputSide && h != null && slot >= 0 && slot < h.getSlots() && h.isItemValid(slot, stack); }
        }

        private final class FluidProxy implements IFluidHandler {
            private final boolean inputSide;
            private FluidProxy(boolean inputSide) { this.inputSide = inputSide; }
            private IFluidHandler target() { return neighbor(ForgeCapabilities.FLUID_HANDLER, inputSide ? outputDirection() : inputDirection()); }
            @Override public int getTanks() { IFluidHandler h = target(); return h == null ? 0 : h.getTanks(); }
            @Override public @NotNull FluidStack getFluidInTank(int tank) { IFluidHandler h = target(); return h == null || tank < 0 || tank >= h.getTanks() ? FluidStack.EMPTY : h.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { IFluidHandler h = target(); return h == null || tank < 0 || tank >= h.getTanks() ? 0 : h.getTankCapacity(tank); }
            @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { IFluidHandler h = target(); return inputSide && h != null && tank >= 0 && tank < h.getTanks() && h.isFluidValid(tank, stack); }
            @Override public int fill(FluidStack resource, FluidAction action) {
                if (!inputSide) return 0;
                IFluidHandler h = target(); if (h == null) return 0;
                int moved = h.fill(resource, action); if (action.execute()) record(moved); return moved;
            }
            @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                if (inputSide) return FluidStack.EMPTY;
                IFluidHandler h = target(); if (h == null) return FluidStack.EMPTY;
                FluidStack moved = h.drain(resource, action); if (action.execute()) record(moved.getAmount()); return moved;
            }
            @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                if (inputSide) return FluidStack.EMPTY;
                IFluidHandler h = target(); if (h == null) return FluidStack.EMPTY;
                FluidStack moved = h.drain(maxDrain, action); if (action.execute()) record(moved.getAmount()); return moved;
            }
        }

        private final class EnergyProxy implements IEnergyStorage {
            private final boolean inputSide;
            private EnergyProxy(boolean inputSide) { this.inputSide = inputSide; }
            private IEnergyStorage target() { return neighbor(ForgeCapabilities.ENERGY, inputSide ? outputDirection() : inputDirection()); }
            @Override public int receiveEnergy(int maxReceive, boolean simulate) {
                if (!inputSide) return 0;
                IEnergyStorage h = target(); if (h == null) return 0;
                int moved = h.receiveEnergy(maxReceive, simulate); if (!simulate) record(moved); return moved;
            }
            @Override public int extractEnergy(int maxExtract, boolean simulate) {
                if (inputSide) return 0;
                IEnergyStorage h = target(); if (h == null) return 0;
                int moved = h.extractEnergy(maxExtract, simulate); if (!simulate) record(moved); return moved;
            }
            @Override public int getEnergyStored() { IEnergyStorage h = target(); return h == null ? 0 : h.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { IEnergyStorage h = target(); return h == null ? 0 : h.getMaxEnergyStored(); }
            @Override public boolean canExtract() { IEnergyStorage h = target(); return !inputSide && h != null && h.canExtract(); }
            @Override public boolean canReceive() { IEnergyStorage h = target(); return inputSide && h != null && h.canReceive(); }
        }
    }

    public static final class GaugeBlockEntity extends BlockEntity {
        private GaugeReading reading = GaugeReading.unavailable();
        private int sampleDelay;

        public GaugeBlockEntity(BlockPos pos, BlockState state) {
            super(GAUGE_BLOCK_ENTITY.get(), pos, state);
        }

        private InstrumentKind kind() { return kindOf(getBlockState().getBlock()); }
        private Direction targetDirection() { return getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite(); }
        private Direction targetSide() { return targetDirection().getOpposite(); }

        public static void serverTick(Level level, BlockPos pos, BlockState state, GaugeBlockEntity blockEntity) {
            if (blockEntity.sampleDelay++ % 5 != 0) return;
            GaugeReading next = blockEntity.sample();
            if (!next.equals(blockEntity.reading)) {
                blockEntity.reading = next;
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
        }

        public String readout() { return MeasurementFormatter.gauge(kind(), reading); }

        private GaugeReading sample() {
            if (level == null) return GaugeReading.unavailable();
            BlockEntity target = level.getBlockEntity(worldPosition.relative(targetDirection()));
            if (target == null) return GaugeReading.unavailable();
            return switch (kind()) {
                case INVENTORY_GAUGE -> target.getCapability(ForgeCapabilities.ITEM_HANDLER, targetSide()).map(handler -> {
                    long amount = 0, capacity = 0;
                    for (int slot = 0; slot < handler.getSlots(); slot++) { amount += handler.getStackInSlot(slot).getCount(); capacity += handler.getSlotLimit(slot); }
                    return new GaugeReading(amount, capacity, true);
                }).orElse(GaugeReading.unavailable());
                case TANK_GAUGE -> target.getCapability(ForgeCapabilities.FLUID_HANDLER, targetSide()).map(handler -> {
                    long amount = 0, capacity = 0;
                    for (int tank = 0; tank < handler.getTanks(); tank++) { amount += handler.getFluidInTank(tank).getAmount(); capacity += handler.getTankCapacity(tank); }
                    return new GaugeReading(amount, capacity, true);
                }).orElse(GaugeReading.unavailable());
                case ENERGY_GAUGE -> target.getCapability(ForgeCapabilities.ENERGY, targetSide()).map(handler ->
                        new GaugeReading(handler.getEnergyStored(), handler.getMaxEnergyStored(), true)).orElse(GaugeReading.unavailable());
                default -> GaugeReading.unavailable();
            };
        }
    }
}
