package com.neoloxal.sable_heavy_blocks.datagen;

import com.neoloxal.sable_heavy_blocks.SableHeavyBlocks;
import com.neoloxal.sable_heavy_blocks.SableHeavyBlocksTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, SableHeavyBlocks.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(SableHeavyBlocksTags.ILLEGAL_BLOCKS)
                .add(Blocks.BEDROCK);
    }
}
