package com.neoloxal.sable_heavy_blocks;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.impl.SableCompanionUtil;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.block_breakers.SubLevelBlockBreakingUtility;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.joml.Vector3dc;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@EventBusSubscriber
public class ModInteractions {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        updateBlock(
                event.getLevel(),
                event.getPos(),
                event.getPlacedBlock(),
                false
        );
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos blockPos = moveBlockPos(event.getPos(), 0, 1, 0);
        updateBlock(
                level,
                blockPos,
                level.getBlockState(blockPos),
                true
        );
    }

    @SubscribeEvent
    public static void blockUpdated(BlockEvent.NeighborNotifyEvent event) {
        if (PROCESSING.get()) {
            return;
        }

        LevelAccessor level = event.getLevel();
        BlockPos aboveBlock = moveBlockPos(event.getPos(), 0, 1, 0);
        updateBlock(
                level,
                aboveBlock,
                level.getBlockState(aboveBlock),
                false
        );
    }

    private static void updateBlock(LevelAccessor level, BlockPos blockPos, BlockState state, Boolean assumeAir) {
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;

            Double mass = PhysicsBlockPropertyHelper.getMass(level, blockPos, state);
            if (mass >= 4 && (level.getBlockState(moveBlockPos(blockPos, 0, -1, 0)).is(Blocks.AIR) || assumeAir)) {
                SubLevelAccess subLevelAccess = SableCompanion.INSTANCE.getContaining((Level) level, blockPos);
                try {
                PROCESSING.set(true);
                Collection<BlockPos> blocks = getBlocks(level, blockPos);
                ServerSubLevel serverSubLevel = SubLevelAssemblyHelper.assembleBlocks(
                        serverLevel,
                        blockPos,
                        blocks,
                        new BoundingBox3i(0, 0, 0, 5, 4, 5)
                );

                blocks.forEach(pos -> {
                    serverLevel.blockUpdated(pos, Blocks.AIR);
                    for (Direction direction : Direction.values()) {
                        BlockPos connectedPos = pos.relative(direction);
                        BlockState connectedState = serverLevel.getBlockState(connectedPos);
                        BlockState updatedConnectedState = connectedState.updateShape(direction.getOpposite(), Blocks.AIR.defaultBlockState(), serverLevel, connectedPos, pos);
                        Block.updateOrDestroy(connectedState, updatedConnectedState, serverLevel, connectedPos, Block.UPDATE_ALL);
                    }
                });
                } finally {
                    PROCESSING.set(false);
                }
            }
        }
    }

    private static BlockPos moveBlockPos(BlockPos blockPos, int x, int y, int z) {
        return new BlockPos(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);
    }

    private static Collection<BlockPos> getBlocks(LevelAccessor level, BlockPos blockPos) {
        List<BlockPos> list = new ArrayList<>();
        list.add(blockPos);

        List<BlockPos> validatePositions = List.of(
                moveBlockPos(blockPos, 0, 1, 0), // 1 above
                moveBlockPos(blockPos, 1, 0, 0), // 1 east
                moveBlockPos(blockPos, -1, 0, 0), // 1 west
                moveBlockPos(blockPos, 0, 0, 1), // 1 south
                moveBlockPos(blockPos, 0, 0, -1), // 1 north
                moveBlockPos(blockPos, 0, 2, 0), // 2 above
                moveBlockPos(blockPos, 1, 1, 0), // 1 east, 1 above
                moveBlockPos(blockPos, -1, 1, 0), // 1 west, 1 above
                moveBlockPos(blockPos, 0, 1, 1), // 1 south, 1 above
                moveBlockPos(blockPos, 0, 1, -1), // 1 north, 1 above
                moveBlockPos(blockPos, 2, 0, 0), // 2 east
                moveBlockPos(blockPos, -2, 0, 0), // 2 west
                moveBlockPos(blockPos, 0, 0, 2), // 2 south
                moveBlockPos(blockPos, 0, 0, -2), // 2 north
                moveBlockPos(blockPos, 1, 0, 1), // 1 south-east
                moveBlockPos(blockPos, 1, 0, -1), // 1 north-east
                moveBlockPos(blockPos, -1, 0, -1), // 1 north-west
                moveBlockPos(blockPos, -1, 0, 1), // 1 south-west
                moveBlockPos(blockPos, 1, -1, 0), // 1 east, 1 down
                moveBlockPos(blockPos, -1, -1, 0), // 1 west, 1 down
                moveBlockPos(blockPos, 0, -1, 1), // 1 south, 1 down
                moveBlockPos(blockPos, 0, -1, -1) // 1 north, 1 down
        );
        for (int i = 0; i < validatePositions.size(); i++) {
            validatePositions.forEach(pos -> validateAndAddBlockPos(list, validatePositions, level, pos));
        }
        return list;
    }

    private static void validateAndAddBlockPos(List<BlockPos> list, List<BlockPos> orginList, LevelAccessor level, BlockPos blockPos) {
        List<BlockPos> connected = List.of(
                moveBlockPos(blockPos, 0, 1, 0), // 1 above
                moveBlockPos(blockPos, 0, -1, 0), // 1 down
                moveBlockPos(blockPos, 1, 0, 0), // 1 east
                moveBlockPos(blockPos, -1, 0, 0), // 1 west
                moveBlockPos(blockPos, 0, 0, 1), // 1 south
                moveBlockPos(blockPos, 0, 0, -1) // 1 north
        );

        connected.forEach(pos -> {
            LOGGER.debug("Checking if block at {}", pos);
            if (list.contains(pos) && !list.contains(blockPos) && !level.getBlockState(blockPos).is(Blocks.AIR)) {
                list.add(blockPos);
                LOGGER.debug("Block found at {}, adding {} to list.", pos, blockPos);
            }
        });
    }
}
