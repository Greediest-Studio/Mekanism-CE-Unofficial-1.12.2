package mekanism.multiblockmachine.common.item;

import mekanism.api.EnumColor;
import mekanism.common.Mekanism;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.LangUtils;
import mekanism.multiblockmachine.common.tile.generator.TileEntityLargeWindGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;
import java.util.Map;

public class ItemBlockLargeWindGenerator extends ItemBlockLargeBaseEnergy {

    public ItemBlockLargeWindGenerator(Block block) {
        super(block, "LargeWindGenerator");
    }

    @Override
    public double getMachineStorage() {
        return MekanismConfig.current().generators.windGeneratorStorage.val() * MekanismConfig.current().multiblock.LargeWindGeneratorProcesses.val();
    }

    @Override
    public boolean canPlace(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState state) {
        boolean isCanPlace = false;
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();

        outer:
        for (int y = 0; y <= 1; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    testPos.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    Block b = world.getBlockState(testPos).getBlock();
                    if (!world.isValid(testPos) || !world.isBlockLoaded(testPos, false) || !b.isReplaceable(world, testPos)) {
                        isCanPlace = true;
                        if (player instanceof EntityPlayerMP mp) {
                            mp.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.canPlace.pos") + ": " + "X " + testPos.getX() + " " + "Y " + testPos.getY() + " " + "Z " + testPos.getZ()));
                        }
                        break outer;
                    }
                }
            }
        }

        outer:
        for (int y = 2; y <= 43; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    testPos.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    Block b = world.getBlockState(testPos).getBlock();
                    if (!world.isValid(testPos) || !world.isBlockLoaded(testPos, false) || !b.isReplaceable(world, testPos)) {
                        isCanPlace = true;
                        if (player instanceof EntityPlayerMP mp) {
                            mp.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.canPlace.pos") + ": " + "X " + testPos.getX() + " " + "Y " + testPos.getY() + " " + "Z " + testPos.getZ()));
                        }
                        break outer;
                    }
                }
            }
        }

        outer:
        for (int y = 43; y <= 47; y++) {
            for (int z = -5; z <= 5; z++) {
                for (int x = -5; x <= 5; x++) {
                    testPos.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    Block b = world.getBlockState(testPos).getBlock();
                    if (!world.isValid(testPos) || !world.isBlockLoaded(testPos, false) || !b.isReplaceable(world, testPos)) {
                        isCanPlace = true;
                        if (player instanceof EntityPlayerMP mp) {
                            mp.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.canPlace.pos") + ": " + "X " + testPos.getX() + " " + "Y " + testPos.getY() + " " + "Z " + testPos.getZ()));
                        }
                        break outer;
                    }
                }
            }
        }

        if (MekanismConfig.current().multiblock.LargeWindGenerationRangeStops.val()) {
            ChunkPos currentChunk = new ChunkPos(pos);
            int rangeCheck = MekanismConfig.current().multiblock.LargeWindGeneratorRangeCheck.val();
            int range;
            if (rangeCheck % 16 != 0) {
                range = rangeCheck / 16 + 1;
            } else {
                range = rangeCheck / 16;
            }
            outer:
            for (int chunkX = currentChunk.x - range; chunkX <= currentChunk.x + range; chunkX++) {
                for (int chunkZ = currentChunk.z - range; chunkZ <= currentChunk.z + range; chunkZ++) {
                    Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                    if (chunk == null) {
                        continue;
                    }
                    Map<BlockPos, TileEntity> tileEntityMap = chunk.getTileEntityMap();
                    for (TileEntity tileEntity : tileEntityMap.values()) {
                        if (tileEntity instanceof TileEntityLargeWindGenerator) {
                            BlockPos tilePos = tileEntity.getPos();
                            double distanceSquared = pos.distanceSq(tilePos);
                            if (distanceSquared <= rangeCheck * rangeCheck) {
                                isCanPlace = true;
                                if (player instanceof EntityPlayerMP mp) {
                                    mp.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.tileEntity.pos") + ": " + "X " + testPos.getX() + " " + "Y " + testPos.getY() + " " + "Z " + testPos.getZ()));
                                }
                                break outer;
                            }
                        }
                    }
                }
            }
        }
        return isCanPlace;
    }

}
