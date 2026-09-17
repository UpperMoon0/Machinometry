package com.nstut.machinometry.fabric;

import com.nstut.machinometry.core.InstrumentKind;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;

final class FabricContent {
    static final InstrumentBlock ITEM_FLOW_METER = new InstrumentBlock(InstrumentKind.ITEM_FLOW);
    static final InstrumentBlock FLUID_FLOW_METER = new InstrumentBlock(InstrumentKind.FLUID_FLOW);
    static final InstrumentBlock ENERGY_FLOW_METER = new InstrumentBlock(InstrumentKind.ENERGY_FLOW);
    static final InstrumentBlock INVENTORY_GAUGE = new InstrumentBlock(InstrumentKind.INVENTORY_GAUGE);
    static final InstrumentBlock TANK_GAUGE = new InstrumentBlock(InstrumentKind.TANK_GAUGE);
    static final InstrumentBlock ENERGY_GAUGE = new InstrumentBlock(InstrumentKind.ENERGY_GAUGE);

    static BlockEntityType<FlowMeterBlockEntity> FLOW_METER_BLOCK_ENTITY;
    static BlockEntityType<GaugeBlockEntity> GAUGE_BLOCK_ENTITY;

    private FabricContent() {}

    static void register() {
        registerBlock(InstrumentKind.ITEM_FLOW, ITEM_FLOW_METER);
        registerBlock(InstrumentKind.FLUID_FLOW, FLUID_FLOW_METER);
        registerBlock(InstrumentKind.ENERGY_FLOW, ENERGY_FLOW_METER);
        registerBlock(InstrumentKind.INVENTORY_GAUGE, INVENTORY_GAUGE);
        registerBlock(InstrumentKind.TANK_GAUGE, TANK_GAUGE);
        registerBlock(InstrumentKind.ENERGY_GAUGE, ENERGY_GAUGE);

        FLOW_METER_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("flow_meter"),
                FabricBlockEntityTypeBuilder.create(FlowMeterBlockEntity::new,
                        ITEM_FLOW_METER, FLUID_FLOW_METER, ENERGY_FLOW_METER).build());
        GAUGE_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("gauge"),
                FabricBlockEntityTypeBuilder.create(GaugeBlockEntity::new,
                        INVENTORY_GAUGE, TANK_GAUGE, ENERGY_GAUGE).build());

        ItemStorage.SIDED.registerForBlockEntity((be, side) -> be.itemStorage(side), FLOW_METER_BLOCK_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity((be, side) -> be.fluidStorage(side), FLOW_METER_BLOCK_ENTITY);
        EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.energyStorage(side), FLOW_METER_BLOCK_ENTITY);
    }

    static InstrumentKind kindOf(Block block) {
        if (block == ITEM_FLOW_METER) return InstrumentKind.ITEM_FLOW;
        if (block == FLUID_FLOW_METER) return InstrumentKind.FLUID_FLOW;
        if (block == ENERGY_FLOW_METER) return InstrumentKind.ENERGY_FLOW;
        if (block == INVENTORY_GAUGE) return InstrumentKind.INVENTORY_GAUGE;
        if (block == TANK_GAUGE) return InstrumentKind.TANK_GAUGE;
        if (block == ENERGY_GAUGE) return InstrumentKind.ENERGY_GAUGE;
        throw new IllegalStateException("Unknown Machinometry block: " + block);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MachinometryFabric.MOD_ID, path);
    }

    private static void registerBlock(InstrumentKind kind, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, id(kind.id()), block);
        Registry.register(BuiltInRegistries.ITEM, id(kind.id()), new BlockItem(block, new Item.Properties()));
    }
}
