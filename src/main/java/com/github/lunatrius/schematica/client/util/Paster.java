package com.github.lunatrius.schematica.client.util;

import com.github.lunatrius.core.util.math.BlockPosHelper;
import com.github.lunatrius.core.util.math.MBlockPos;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Names;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;


public class Paster {
    public static void paste(final EntityPlayer player, final SchematicWorld world, final World mcWorld) {
        int count = 0;
        List<String> placeAfterwards = new ArrayList<>();
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule sendCommandFeedback false"));
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule doTileDrops false"));
        for (final MBlockPos pos : BlockPosHelper.getAllInBox(BlockPos.ORIGIN, new BlockPos(world.getWidth() - 1, world.getHeight() - 1, world.getLength() - 1))) {
            final IBlockState blockState = world.getBlockState(pos);
            final Block block = blockState.getBlock();
            final BlockPos mcPos = new BlockPos(world.position.add(pos));
            String cmd = String.format("%d %d %d %s %d", mcPos.getX(), mcPos.getY(), mcPos.getZ(), blockState.getBlock().getRegistryName(), block.getMetaFromState(blockState));
            if (block.canPlaceBlockAt(mcWorld, mcPos)) {
                if (ConfigurationHandler.pasteAir || !(block instanceof BlockAir)) {
                    mcWorld.sendPacketToServer(new CPacketChatMessage("/setblock " + cmd));
                    count++;
                }
            } else if (ConfigurationHandler.pasteAir || !(block instanceof BlockAir)) {
                placeAfterwards.add(cmd);
            }
        }
        for (String cmd : placeAfterwards) {
            mcWorld.sendPacketToServer(new CPacketChatMessage("/setblock " + cmd));
            count++;
        }
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule doTileDrops true"));
        mcWorld.sendPacketToServer(new CPacketChatMessage("/gamerule sendCommandFeedback true"));
        player.sendMessage(new TextComponentTranslation(Names.Messages.FINISHED_PASTING, count));
    }
}
