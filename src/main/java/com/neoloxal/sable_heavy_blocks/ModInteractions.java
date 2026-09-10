package com.neoloxal.sable_heavy_blocks;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;
import oshi.util.tuples.Pair;

import java.util.*;

@EventBusSubscriber
public class ModInteractions {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        updateBlock(
                event.getLevel(),
                event.getPos(),
                event.getPlacedBlock()
        );
    }

    /** {@link #blockUpdated(BlockEvent.NeighborNotifyEvent)} does the function of this. */
    /*@SubscribeEvent
    public static void breakBlock(BlockDropsEvent.BreakEvent event) {
        LevelAccessor level = event.getLevel();
        List<BlockPos> blocks = getUnstableBlocks(level, event.getPos(), 1, 5).getA();
        blocks.forEach(blockPos -> updateBlock(
                level,
                blockPos,
                level.getBlockState(blockPos)
        ));
    }*/

    @SubscribeEvent
    public static void blockUpdated(BlockEvent.NeighborNotifyEvent event) {
        if (PROCESSING.get()) {
            return;
        }

        LevelAccessor level = event.getLevel();
        List<BlockPos> blocks = getUnstableBlocks(level, event.getPos(), 1, FilterType.HEAVY_BLOCKS).getA();
        blocks.forEach(blockPos -> updateBlock(
                level,
                blockPos,
                level.getBlockState(blockPos)
        ));
    }

    private static void updateBlock(LevelAccessor level, BlockPos blockPos, BlockState state) {
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;

            try {
                PROCESSING.set(true);
                double mass = PhysicsBlockPropertyHelper.getMass(level, blockPos, state);
                if (mass >= SHBServerConfig.CONFIG.minMassToBreak.getAsDouble() && !state.is(SableHeavyBlocksTags.ILLEGAL_BLOCKS) || state.is(SableHeavyBlocksTags.ADDITIONAL_HEAVY_BLOCKS)) {
                    Pair<List<BlockPos>, Boolean> unstableBlocks = getUnstableBlocks(level, blockPos, -1, FilterType.LIGHT_BLOCKS);
                    if (!unstableBlocks.getB()) {
                        SubLevelAccess subLevelAccess = SableCompanion.INSTANCE.getContaining((Level) level, blockPos);
                        Collection<BlockPos> blocks = getBlocks(level, blockPos);
                        blocks.addAll(unstableBlocks.getA());

                        if (subLevelAccess != null) {
                            double totalMass = 0;
                            for (BlockPos pos : blocks) {
                                totalMass += PhysicsBlockPropertyHelper.getMass(level, pos, level.getBlockState(pos));
                            }

                            ServerSubLevel serverSubLevel = (ServerSubLevel) subLevelAccess;
                            if (Math.abs(serverSubLevel.getMassTracker().getMass() - totalMass) < 0.001) {
                                return;
                            }
                        }

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
                    }
                }
            } finally {
                PROCESSING.set(false);
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
        validatePositions.forEach(pos -> validateAndAddBlockPos(list, level, pos));
        return list;
    }

    private static Pair<List<BlockPos>, Boolean> getUnstableBlocks(LevelAccessor level, BlockPos blockPos, int direction, FilterType filterType) {
        Pair<List<BlockPos>, Boolean> unstableBlocks = getUnstableBlocks(level, blockPos, direction, 1, new HashSet<>());
        List<BlockPos> cleanUnstableBlocks = unstableBlocks.getA().stream().distinct().filter(pos -> filterType.test(level, pos, level.getBlockState(pos))).toList();
        /*if (unstableBlocks.getB()) {
            LOGGER.debug("STABLE {}", cleanUnstableBlocks);
        } else {
            LOGGER.debug("UNSTABLE {}", cleanUnstableBlocks);
        }*/
        return new Pair<>(cleanUnstableBlocks, unstableBlocks.getB());
    }

    private static Pair<List<BlockPos>, Boolean> getUnstableBlocks(LevelAccessor level, BlockPos blockPos, int direction, int depth, Set<BlockPos> visited) {
        List<BlockPos> structurePositionsToCheck = List.of(
                moveBlockPos(blockPos, 1, direction, 0),
                moveBlockPos(blockPos, 0, direction, 0),
                moveBlockPos(blockPos, 1, direction, 1),
                moveBlockPos(blockPos, 1, direction, -1),
                moveBlockPos(blockPos, -1, direction, 0),
                moveBlockPos(blockPos, -1, direction, 1),
                moveBlockPos(blockPos, -1, direction, -1),
                moveBlockPos(blockPos, 0, direction, 1),
                moveBlockPos(blockPos, 0, direction, -1)
        );

        List<BlockPos> unstableBlocks = new ArrayList<>();

        if (depth > SHBServerConfig.CONFIG.minDistanceForSupport.get()) {
            return new Pair<>(unstableBlocks, true);
        }

        boolean stable = false;
        for (BlockPos pos : structurePositionsToCheck) {
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos).canSurvive(level, blockPos)) {
                if (!visited.add(pos)) {
                    continue;
                }
                Pair<List<BlockPos>, Boolean> foundUnstableBlocks = getUnstableBlocks(level, pos, direction, depth + 1, visited);
                unstableBlocks.addAll(foundUnstableBlocks.getA().stream().filter(unstableBlock -> !level.getBlockState(unstableBlock).isAir()).toList());
                unstableBlocks.add(pos);
                if (foundUnstableBlocks.getB()) {
                    stable = true;
                }
            }
        }

        //LOGGER.debug("Block Found? {} at depth of {} before max depth of {}.", stable, depth, SHBServerConfig.CONFIG.minDistanceForSupport.get());
        return new Pair<>(unstableBlocks, stable);
    }

    private static void validateAndAddBlockPos(List<BlockPos> list, LevelAccessor level, BlockPos blockPos) {
        List<BlockPos> connected = List.of(
                moveBlockPos(blockPos, 0, 1, 0), // 1 above
                moveBlockPos(blockPos, 0, -1, 0), // 1 down
                moveBlockPos(blockPos, 1, 0, 0), // 1 east
                moveBlockPos(blockPos, -1, 0, 0), // 1 west
                moveBlockPos(blockPos, 0, 0, 1), // 1 south
                moveBlockPos(blockPos, 0, 0, -1) // 1 north
        );

        connected.forEach(pos -> {
            //LOGGER.debug("Checking if block at {}", pos);
            if (list.contains(pos) && !list.contains(blockPos)) {
                BlockState block = level.getBlockState(blockPos);
                if (!block.isAir() && block.canSurvive(level, blockPos)) {
                    list.add(blockPos);
                    //LOGGER.debug("Block found at {}, adding {} to list.", pos, blockPos);
                }
            }
        });
    }
}
