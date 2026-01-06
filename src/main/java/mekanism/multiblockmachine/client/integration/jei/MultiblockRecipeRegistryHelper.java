package mekanism.multiblockmachine.client.integration.jei;

import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.multiblockmachine.client.gui.generator.GuiLargeGasGenerator;
import mekanism.multiblockmachine.client.gui.machine.GuiLargeChemicalInfuser;
import mekanism.multiblockmachine.client.gui.machine.GuiLargeChemicalWasher;
import mekanism.multiblockmachine.client.gui.machine.GuiLargeElectrolyticSeparator;
import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mezz.jei.api.IModRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class MultiblockRecipeRegistryHelper {

    public static void registerLargeSeparator(IModRegistry registry) {
        registry.addRecipeClickArea(GuiLargeElectrolyticSeparator.class, 80, 30, 16, 6, Recipe.ELECTROLYTIC_SEPARATOR.getJEICategory());
        registerRecipeItem(registry, MultiblockMachineBlocks.LargeElectrolyticSeparator, Recipe.ELECTROLYTIC_SEPARATOR.getJEICategory());
    }

    public static void registerLargeChemicalInfuser(IModRegistry registry) {
        registry.addRecipeClickArea(GuiLargeChemicalInfuser.class, 47, 50, 28, 8, Recipe.CHEMICAL_INFUSER.getJEICategory());
        registry.addRecipeClickArea(GuiLargeChemicalInfuser.class, 101, 50, 28, 8, Recipe.CHEMICAL_INFUSER.getJEICategory());
        registerRecipeItem(registry, MultiblockMachineBlocks.LargeChemicalInfuser, Recipe.CHEMICAL_INFUSER.getJEICategory());
    }

    public static void registerLargeChemicalWasher(IModRegistry registry) {
        registry.addRecipeClickArea(GuiLargeChemicalWasher.class, 61, 39, 55, 8, Recipe.CHEMICAL_WASHER.getJEICategory());
        registerRecipeItem(registry, MultiblockMachineBlocks.LargeChemicalWasher, Recipe.CHEMICAL_WASHER.getJEICategory());
    }

    public static void registerGasStackFlueToEnergyRecipe(IModRegistry registry) {
        registry.addRecipeClickArea(GuiLargeGasGenerator.class, 55, 18, 66, 50, RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.getJEICategory());
        registerRecipeItem(registry, MultiblockMachineBlocks.LargeGasGenerator, RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.getJEICategory());
    }

    private static void registerRecipeItem(IModRegistry registry, Block block, String... recipe) {
        ItemStack add = new ItemStack(block);
        registry.addRecipeCatalyst(add, recipe);
    }
}
