package com.neoloxal.sable_heavy_blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class SableHeavyBlocksTags {
    public static final TagKey<Block> ILLEGAL_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(SableHeavyBlocks.MODID, "illegal_blocks")
    );

    public static final TagKey<Block> ADDITIONAL_HEAVY_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(SableHeavyBlocks.MODID, "additional_heavy_blocks")
    );
}
