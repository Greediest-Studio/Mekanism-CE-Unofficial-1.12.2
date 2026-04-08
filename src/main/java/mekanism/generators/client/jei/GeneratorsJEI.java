package mekanism.generators.client.jei;

import mekanism.client.jei.GuiElementHandler;
import mekanism.client.jei.MekanismJEI;
import mekanism.generators.client.jei.machine.other.FissionReactorRecipeCategory;
import mekanism.generators.client.jei.machine.other.FusionCoolingRecipeCategory;
import mekanism.generators.client.jei.machine.other.GasStackFlueToEnergyRecipeCategory;
import mekanism.generators.common.GeneratorsBlocks;
import mezz.jei.api.*;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.Item;

@JEIPlugin
public class GeneratorsJEI implements IModPlugin {

    @Override
    public void registerItemSubtypes(ISubtypeRegistry registry) {
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(GeneratorsBlocks.Generator), MekanismJEI.NBT_INTERPRETER);
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(GeneratorsBlocks.Generator2), MekanismJEI.NBT_INTERPRETER);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
        registry.addRecipeCategories(new FissionReactorRecipeCategory(guiHelper));
        registry.addRecipeCategories(new FusionCoolingRecipeCategory<>(guiHelper));
        registry.addRecipeCategories(new GasStackFlueToEnergyRecipeCategory<>(guiHelper));

    }

    @Override
    public void register(IModRegistry registry) {
        registry.addAdvancedGuiHandlers(new GuiElementHandler());
        GeneratorRecipeRegistryHelper.registerFissionReactor(registry);
        GeneratorRecipeRegistryHelper.registerFusionCooling(registry);
        GeneratorRecipeRegistryHelper.registerGasStackFlueToEnergyRecipe(registry);
    }
}
