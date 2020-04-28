package com.github.lunatrius.schematica.client.util;

import com.github.lunatrius.core.util.math.BlockPosHelper;
import com.github.lunatrius.core.util.math.MBlockPos;
import com.github.lunatrius.schematica.block.state.BlockStateHelper;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.reference.Names;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;


public class Paster {
    public static final Paster INSTANCE = new Paster();
    private static boolean pasteAir = false;

    public void togglePasteAir(SchematicWorld schematic) {
        pasteAir = !pasteAir && schematic != null;
    }

    public boolean isPastingAir () {
        return pasteAir;
    }

    private static boolean setBlockAt(final IBlockState blockState, final BlockPos pos, final World world) {
        Block block = blockState.getBlock();
        boolean successfull = false;
        if(block.canPlaceBlockAt(world, pos)) {
            if (pos.getY() > 0 && pos.getY() < 256 && world.isBlockLoaded(pos)) {
                String cmd = String.format("%d %d %d %s %d", pos.getX(), pos.getY(), pos.getZ(), block.getRegistryName(), block.getMetaFromState(blockState));
                world.sendPacketToServer(new CPacketChatMessage("/setblock " + cmd + " destroy"));
            }
            successfull = true;
        }
        return successfull;
    }

    public static void paste(final EntityPlayer player, final SchematicWorld world, final World mcWorld) {
        int count = 0;
        int errorCount = 0;
        List<MBlockPos> blocksToPlace = Lists.newArrayList(BlockPosHelper.getAllInBox(BlockPos.ORIGIN, new BlockPos(world.getWidth() - 1, world.getHeight() - 1, world.getLength() - 1)));
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule sendCommandFeedback false"));
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule doTileDrops false"));
        while (blocksToPlace.size() > 0) {
            MBlockPos pos = blocksToPlace.get(0);
            blocksToPlace.remove(0);
            final IBlockState blockState = world.getBlockState(pos);
            final Block block = blockState.getBlock();
            final BlockPos mcPos = new BlockPos(world.position.add(pos));
            if (!BlockStateHelper.areBlockStatesEqual(blockState, mcWorld.getBlockState(mcPos))) {
                if (pasteAir || !(block.isAir(blockState, mcWorld, mcPos))) {
                    if (setBlockAt(blockState, mcPos, mcWorld)) {
                        count++;
                    } else {
                        errorCount++;
                        blocksToPlace.add(blocksToPlace.size() -1, new MBlockPos(pos));
                    }
                }
            }
        }
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule doTileDrops true"));
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule sendCommandFeedback true"));
        player.sendMessage(new TextComponentTranslation(Names.Messages.FINISHED_PASTING, count));
    }
}
