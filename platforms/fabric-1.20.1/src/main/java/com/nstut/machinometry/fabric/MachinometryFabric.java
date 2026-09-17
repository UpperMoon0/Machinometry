package com.nstut.machinometry.fabric;

import net.fabricmc.api.ModInitializer;

public final class MachinometryFabric implements ModInitializer {
    public static final String MOD_ID = "machinometry";

    @Override
    public void onInitialize() {
        FabricContent.register();
    }
}
