package com.neoloxal.sable_heavy_blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public record FilterRecord(LevelAccessor level, BlockPos pos, BlockState state) {
}
