package mekanism.generators.client.jei.machine.other;

import mekanism.api.gas.GasStack;
import mekanism.client.SpecialColors;
import mekanism.client.jei.MekanismJEI;
import mekanism.common.MekanismFluids;
import mekanism.common.util.LangUtils;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class FissionReactorRecipeWrapper implements IRecipeWrapper {

    private final boolean sodiumCooled;
    private final GasStack fuelInput;
    @Nullable
    private final GasStack coolantInputGas;
    @Nullable
    private final FluidStack coolantInputFluid;
    @Nullable
    private final GasStack heatedCoolantOutputGas;
    @Nullable
    private final FluidStack heatedCoolantOutputFluid;
    private final GasStack wasteOutput;

    private FissionReactorRecipeWrapper(boolean sodiumCooled, GasStack fuelInput, @Nullable GasStack coolantInputGas,
                                        @Nullable FluidStack coolantInputFluid, @Nullable GasStack heatedCoolantOutputGas,
                                        @Nullable FluidStack heatedCoolantOutputFluid, GasStack wasteOutput) {
        this.sodiumCooled = sodiumCooled;
        this.fuelInput = fuelInput.copy();
        this.coolantInputGas = coolantInputGas == null ? null : coolantInputGas.copy();
        this.coolantInputFluid = coolantInputFluid == null ? null : coolantInputFluid.copy();
        this.heatedCoolantOutputGas = heatedCoolantOutputGas == null ? null : heatedCoolantOutputGas.copy();
        this.heatedCoolantOutputFluid = heatedCoolantOutputFluid == null ? null : heatedCoolantOutputFluid.copy();
        this.wasteOutput = wasteOutput.copy();
    }

    public static FissionReactorRecipeWrapper waterCooled() {
        return new FissionReactorRecipeWrapper(false,
                new GasStack(MekanismFluids.FissileFuel, 1),
                null,
                new FluidStack(FluidRegistry.WATER, 1),
                null,
                new FluidStack(MekanismFluids.Steam, 1),
                new GasStack(MekanismFluids.NuclearWaste, 1));
    }

    public static FissionReactorRecipeWrapper sodiumCooled() {
        return new FissionReactorRecipeWrapper(true,
                new GasStack(MekanismFluids.FissileFuel, 1),
                new GasStack(MekanismFluids.Sodium, 1),
                null,
                new GasStack(MekanismFluids.SuperheatedSodium, 1),
                null,
                new GasStack(MekanismFluids.NuclearWaste, 1));
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<GasStack> gasInputs = new ArrayList<>();
        if (coolantInputGas != null) {
            gasInputs.add(coolantInputGas);
        }
        gasInputs.add(fuelInput);
        ingredients.setInputs(MekanismJEI.TYPE_GAS, gasInputs);

        List<GasStack> gasOutputs = new ArrayList<>();
        if (heatedCoolantOutputGas != null) {
            gasOutputs.add(heatedCoolantOutputGas);
        }
        gasOutputs.add(wasteOutput);
        ingredients.setOutputs(MekanismJEI.TYPE_GAS, gasOutputs);

        if (coolantInputFluid != null) {
            ingredients.setInput(VanillaTypes.FLUID, coolantInputFluid);
        }
        if (heatedCoolantOutputFluid != null) {
            ingredients.setOutput(VanillaTypes.FLUID, heatedCoolantOutputFluid);
        }
    }

    @Override
    public void drawInfo(@Nonnull Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        lines.add(LangUtils.localize("gui.status") + ": " + LangUtils.localize("gui.on"));
        lines.add(LangUtils.localize("gui.mode") + ": " + LangUtils.localize(sodiumCooled ? "fission.cooling.sodium" : "fission.cooling.water"));
        lines.add(LangUtils.localize("gui.coolant") + ": " + getInputCoolantName() + " -> " + getOutputCoolantName());
        lines.add(LangUtils.localize("gui.output") + ": " + wasteOutput.getGas().getLocalizedName());
        int screenX = 45 - 6;
        int screenY = 17 - 13;
        int screenWidth = 105;
        int screenHeight = 56;
        int lineSpacing = 2;
        int totalHeight = lines.size() * 8 + lineSpacing * (lines.size() - 1);
        int startY = screenY + Math.max(0, (screenHeight - totalHeight) / 2);
        for (String line : lines) {
            minecraft.fontRenderer.drawString(minecraft.fontRenderer.trimStringToWidth(line, screenWidth - 6), screenX + 3, startY, SpecialColors.TEXT_SCREEN.argb());
            startY += 8 + lineSpacing;
        }
    }

    public GasStack getFuelInput() {
        return fuelInput.copy();
    }

    @Nullable
    public GasStack getCoolantInputGas() {
        return coolantInputGas == null ? null : coolantInputGas.copy();
    }

    @Nullable
    public FluidStack getCoolantInputFluid() {
        return coolantInputFluid == null ? null : coolantInputFluid.copy();
    }

    @Nullable
    public GasStack getHeatedCoolantOutputGas() {
        return heatedCoolantOutputGas == null ? null : heatedCoolantOutputGas.copy();
    }

    @Nullable
    public FluidStack getHeatedCoolantOutputFluid() {
        return heatedCoolantOutputFluid == null ? null : heatedCoolantOutputFluid.copy();
    }

    public GasStack getWasteOutput() {
        return wasteOutput.copy();
    }

    private String getInputCoolantName() {
        if (coolantInputGas != null) {
            return coolantInputGas.getGas().getLocalizedName();
        }
        return coolantInputFluid == null ? "-" : LangUtils.localizeFluidStack(coolantInputFluid);
    }

    private String getOutputCoolantName() {
        if (heatedCoolantOutputGas != null) {
            return heatedCoolantOutputGas.getGas().getLocalizedName();
        }
        return heatedCoolantOutputFluid == null ? "-" : LangUtils.localizeFluidStack(heatedCoolantOutputFluid);
    }
}
