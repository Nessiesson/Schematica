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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
        List<MBlockPos> blocksToPlace = new ArrayList<>();
        for (MBlockPos pos : BlockPosHelper.getAllInBox(BlockPos.ORIGIN, new BlockPos(world.getWidth() - 1, world.getHeight() - 1, world.getLength() - 1))) {
            blocksToPlace.add(new MBlockPos(pos));
        }
        Map<MBlockPos, Integer> failedBlocks = new HashMap<>();
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule sendCommandFeedback false"));
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule doTileDrops false"));
        System.out.println(blocksToPlace);
        while (blocksToPlace.size() > 0) {
            MBlockPos pos = blocksToPlace.get(0);
            blocksToPlace.remove(0);
            final IBlockState blockState = world.getBlockState(pos);
            final Block block = blockState.getBlock();
            final BlockPos mcPos = new BlockPos(world.position.add(pos));
            if (!BlockStateHelper.areBlockStatesEqual(blockState, mcWorld.getBlockState(mcPos))) {
                if (pasteAir || !(block.isAir(blockState, mcWorld, mcPos))) {
                    System.out.println("Blocks to place: " + blocksToPlace.size());
                    if (setBlockAt(blockState, mcPos, mcWorld)) {
                        count++;
                    } else {
                        if (failedBlocks.containsKey(pos)) {
                            if (failedBlocks.get(pos) < 5 ) {
                                System.out.println("Failed placing " + block + " at " + pos + " " + failedBlocks.get(pos) + " times trying again...");
                                blocksToPlace.add(blocksToPlace.size() - 1, new MBlockPos(pos));
                                failedBlocks.put(pos, failedBlocks.get(pos) + 1);
                            } else {
                                System.out.println("Failed placing " + block + " at " + pos + " " + failedBlocks.get(pos) + " times");
                            }
                        } else {
                            blocksToPlace.add(blocksToPlace.size() - 1, new MBlockPos(pos));
                            failedBlocks.put(pos, 1);
                        }
                    }
                }
            }
        }
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule doTileDrops true"));
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule sendCommandFeedback true"));
        player.sendMessage(new TextComponentTranslation(Names.Messages.FINISHED_PASTING, count));
    }
}
