package com.nstut.machinometry.core;

public enum InstrumentKind {
    ITEM_FLOW("item_flow_meter", "Item Flow Meter", Mode.FLOW, Unit.ITEM),
    FLUID_FLOW("fluid_flow_meter", "Fluid Flow Meter", Mode.FLOW, Unit.FLUID),
    ENERGY_FLOW("energy_flow_meter", "Energy Flow Meter", Mode.FLOW, Unit.ENERGY),
    INVENTORY_GAUGE("inventory_gauge", "Inventory Gauge", Mode.GAUGE, Unit.ITEM),
    TANK_GAUGE("tank_gauge", "Tank Gauge", Mode.GAUGE, Unit.FLUID),
    ENERGY_GAUGE("energy_gauge", "Energy Gauge", Mode.GAUGE, Unit.ENERGY);

    private final String id;
    private final String displayName;
    private final Mode mode;
    private final Unit unit;

    InstrumentKind(String id, String displayName, Mode mode, Unit unit) {
        this.id = id;
        this.displayName = displayName;
        this.mode = mode;
        this.unit = unit;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Mode mode() { return mode; }
    public Unit unit() { return unit; }
    public boolean isFlowMeter() { return mode == Mode.FLOW; }
    public boolean isGauge() { return mode == Mode.GAUGE; }

    public enum Mode { FLOW, GAUGE }
    public enum Unit { ITEM, FLUID, ENERGY }
}
