package com.github.lunatrius.schematica.client.util;

import com.github.lunatrius.core.util.math.BlockPosHelper;
import com.github.lunatrius.core.util.math.MBlockPos;
import com.github.lunatrius.schematica.block.state.BlockStateHelper;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.reference.Names;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.*;

public class Paster {
    public static final Paster INSTANCE = new Paster();
    private static Map<MBlockPos, Integer> failedBlocks = new HashMap<>();
    private static Map<MBlockPos, Long> blocksToPaste = new HashMap<>();
    private static boolean pasteAir = false;
    private static int count = 0;
    private static int blocksToProcessPerTick = 5000;
    private static World mcWorld;
    private static SchematicWorld world;
    private static EntityPlayer player;

    public void togglePasteAir(SchematicWorld schematic) {
        pasteAir = !pasteAir && schematic != null;
    }

    public boolean isPastingAir () {
        return pasteAir;
    }

    public boolean isPasting() {
        return blocksToPaste.size() > 0;
    }

    public boolean needsProcessing() {
        return blocksToPaste.size() > 0;
    }

    private static boolean blockNedsSupport(Block block, BlockPos pos){
        return (block instanceof BlockBush ||
                block instanceof BlockFlowerPot ||
                block instanceof BlockFire ||
                block instanceof BlockButton ||
                block instanceof BlockDoublePlant ||
                block instanceof BlockSign ||
                block instanceof BlockChorusFlower ||
                block instanceof BlockCake ||
                block instanceof BlockCarpet ||
                block instanceof BlockRailBase ||
                block instanceof BlockEndRod ||
                block instanceof BlockMushroom ||
                block instanceof BlockLever ||
                block instanceof BlockRedstoneWire ||
                block instanceof BlockCactus ||
                block instanceof BlockVine ||
                block instanceof BlockSnow ||
                block instanceof BlockTorch ||
                block instanceof BlockLadder ||
                block instanceof BlockBanner ||
                block instanceof BlockDoor ||
                block instanceof BlockRedstoneDiode ||
                block instanceof BlockBasePressurePlate ||
                block instanceof BlockPistonMoving ||
                block instanceof BlockReed ||
                block instanceof BlockTripWireHook);
    }

    private static boolean setBlockAt(final IBlockState blockState, final BlockPos pos, final World world) {
        Block block = blockState.getBlock();
        if (pos.getY() < 0 || pos.getY() > 256 || !world.isBlockLoaded(pos)) {
            return true;
        }
        if(!blockNedsSupport(block, pos)) {
            String cmd = String.format("%d %d %d %s %d", pos.getX(), pos.getY(), pos.getZ(), block.getRegistryName(), block.getMetaFromState(blockState));
            world.sendPacketToServer(new CPacketChatMessage("/setblock " + cmd + " destroy"));
            return true;
        } else {
            if (block.canPlaceBlockAt(mcWorld, pos)) {
                String cmd = String.format("%d %d %d %s %d", pos.getX(), pos.getY(), pos.getZ(), block.getRegistryName(), block.getMetaFromState(blockState));
                world.sendPacketToServer(new CPacketChatMessage("/setblock " + cmd + " destroy"));
                return true;
            } else {
                String cmd = String.format("%d %d %d", pos.getX(), pos.getY(), pos.getZ());
                world.sendPacketToServer(new CPacketChatMessage("/setblock " + cmd + " air 0 destroy"));
                return false;
            }
        }
    }

    private static List<MBlockPos> getBlocksForTick(long tick) {
        List<MBlockPos> blocksThisTick = new ArrayList<>();
        for (MBlockPos pos: blocksToPaste.keySet()) {
            if (blocksToPaste.get(pos) <= tick) {
                blocksThisTick.add(new MBlockPos(pos));
            }
        }
        return blocksThisTick;
    }

    public void paste(final EntityPlayer player1, final SchematicWorld world1, final World world2) {
        world = world1;
        mcWorld = world2;
        player = player1;
        int i = 0;
        long delay = 1;
        for (MBlockPos pos : BlockPosHelper.getAllInBox(BlockPos.ORIGIN, new BlockPos(world.getWidth() - 1, world.getHeight() - 1, world.getLength() - 1))) {
            if (i > blocksToProcessPerTick) {
                delay++;
                i = 0;
            }
            final IBlockState blockState = world.getBlockState(pos);
            final BlockPos mcPos = new BlockPos(world.position.add(pos));
            if (!BlockStateHelper.areBlockStatesEqual(blockState, mcWorld.getBlockState(mcPos))) {
                if (pasteAir || !(blockState.getBlock().isAir(blockState, mcWorld, mcPos))) {
                    blocksToPaste.put(new MBlockPos(pos), mcWorld.getTotalWorldTime() + delay);
                    i++;
                }
            }
        }
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule sendCommandFeedback false"));
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule doTileDrops false"));
    }
    public void processQueue() {
        long nextFreeTick = Collections.max(blocksToPaste.values()) + 3;
        for (MBlockPos pos : getBlocksForTick(mcWorld.getTotalWorldTime())) {
            blocksToPaste.remove(pos);
            final IBlockState blockState = world.getBlockState(pos);
            final BlockPos mcPos = new BlockPos(world.position.add(pos));
            if (setBlockAt(blockState, mcPos, mcWorld)) {
                count++;
            } else {
                if (failedBlocks.containsKey(pos)) {
                    if (failedBlocks.get(pos) < 400) {
                        blocksToPaste.put(new MBlockPos(pos), nextFreeTick);
                        failedBlocks.put(pos, failedBlocks.get(pos) + 1);
                    } else {
                    }
                } else {
                    blocksToPaste.put(new MBlockPos(pos), nextFreeTick);
                    failedBlocks.put(pos, 1);
                }
            }
        }
        if (blocksToPaste.size() == 0) {
            mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule doTileDrops true"));
            mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule sendCommandFeedback true"));
            player.sendMessage(new TextComponentTranslation(Names.Messages.FINISHED_PASTING, count));
            count = 0;
        }
    }
}
