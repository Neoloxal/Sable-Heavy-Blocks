package com.neoloxal.sable_heavy_blocks;

import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public enum FilterType {
    ALL(filterRecord -> true),
    HEAVY_BLOCKS(filterRecord -> {
        double mass = PhysicsBlockPropertyHelper.getMass(filterRecord.level(), filterRecord.pos(), filterRecord.state());
        return mass >= 4 && !filterRecord.state().is(Blocks.BEDROCK);
    }),
    LIGHT_BLOCKS(filterRecord -> PhysicsBlockPropertyHelper.getMass(filterRecord.level(), filterRecord.pos(), filterRecord.state()) < 4);

    private final Predicate<FilterRecord> predicate;

    FilterType(Predicate<FilterRecord> predicate) {
        this.predicate = predicate;
    }

    public boolean test(LevelAccessor level, BlockPos blockPos, BlockState blockState) {
        return this.predicate.test(new FilterRecord(level, blockPos, blockState));
    }
}
