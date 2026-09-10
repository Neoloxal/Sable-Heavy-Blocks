package com.neoloxal.sable_heavy_blocks;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class SHBServerConfig {
    public static final SHBServerConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public final ModConfigSpec.ConfigValue<Integer> minDistanceForSupport;

    public SHBServerConfig(ModConfigSpec.Builder builder) {
        minDistanceForSupport = builder
                .comment("Minimum distance for support.")
                .translation("config.sable_heavy_blocks.min_distance_for_support")
                .define("min_distance_for_support", 20);
    }

    static {
        Pair<SHBServerConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(SHBServerConfig::new);

        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }
}
