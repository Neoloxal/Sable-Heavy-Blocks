package com.neoloxal.sable_heavy_blocks;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class SHBServerConfig {
    public static final SHBServerConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public final ModConfigSpec.IntValue minDistanceForSupport;
    public final ModConfigSpec.DoubleValue minMassToBreak;

    public SHBServerConfig(ModConfigSpec.Builder builder) {
        minDistanceForSupport = builder
                .comment("Minimum distance for support.")
                .translation("config.sable_heavy_blocks.min_distance_for_support")
                .defineInRange("min_distance_for_support", 20, 0, 200);
        minMassToBreak = builder
                .comment("Minimum mass to break.")
                .translation("config.sable_heavy_blocks.min_mass_to_break")
                .defineInRange("min_mass_to_break", 4.0, 0.1, 1000);
    }

    static {
        Pair<SHBServerConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(SHBServerConfig::new);

        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }
}
