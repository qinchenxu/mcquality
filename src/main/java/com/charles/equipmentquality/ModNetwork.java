package com.charles.equipmentquality;

import net.neoforged.bus.api.IEventBus;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        // The first playable loop currently relies on vanilla interaction sync and built-in particle packets.
        // Add custom payloads here once active skill state or multiplayer-only status data needs explicit replication.
    }
}