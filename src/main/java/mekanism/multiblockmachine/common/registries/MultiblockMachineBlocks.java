package mekanism.multiblockmachine.common.registries;

import mekanism.multiblockmachine.common.MekanismMultiblockMachine;
import mekanism.multiblockmachine.common.block.*;
import mekanism.multiblockmachine.common.item.*;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.registries.IForgeRegistry;

@ObjectHolder(MekanismMultiblockMachine.MODID)
public class MultiblockMachineBlocks {

    public static Block LargeElectrolyticSeparator = new BlockLargeElectrolyticSeparator();
    public static Block LargeChemicalInfuser = new BlockLargeChemicalInfuser();
    public static Block LargeChemicalWasher = new BlockLargeChemicalWasher();
    public static Block LargeWindGenerator = new BlockLargeWindGenerator();
    public static Block LargeGasGenerator = new BlockLargeGasGenerator();

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        registryBlock(registry, LargeElectrolyticSeparator, "LargeElectrolyticSeparator");
        registryBlock(registry, LargeChemicalInfuser, "LargeChemicalInfuser");
        registryBlock(registry, LargeChemicalWasher, "LargeChemicalWasher");
        registryBlock(registry, LargeWindGenerator, "LargeWindGenerator");
        registryBlock(registry, LargeGasGenerator, "LargeGasGenerator");
    }

    public static void registerItemBlocks(IForgeRegistry<Item> registry) {
        registryItem(registry, new ItemBlockLargeElectrolyticSeparator(LargeElectrolyticSeparator), "LargeElectrolyticSeparator");
        registryItem(registry, new ItemBlockLargeChemicalInfuser(LargeChemicalInfuser), "LargeChemicalInfuser");
        registryItem(registry, new ItemBlockLargeChemicalWasher(LargeChemicalWasher), "LargeChemicalWasher");
        registryItem(registry, new ItemBlockLargeWindGenerator(LargeWindGenerator), "LargeWindGenerator");
        registryItem(registry, new ItemBlockLargeGasGenerator(LargeGasGenerator), "LargeGasGenerator");
    }


    public static void registryItem(IForgeRegistry<Item> registry, Item item, String name) {
        registry.register(init(item, name));
    }

    public static Item init(Item item, String name) {
        return item.setTranslationKey(name).setRegistryName(new ResourceLocation(MekanismMultiblockMachine.MODID, name));
    }

    public static void registryBlock(IForgeRegistry<Block> registry, Block block, String name) {
        registry.register(init(block, name));
    }

    public static Block init(Block block, String name) {
        return block.setTranslationKey(name).setRegistryName(new ResourceLocation(MekanismMultiblockMachine.MODID, name));
    }


}
