package mekanism.generators.common.content.fission;

import mekanism.api.gas.GasStack;
import mekanism.common.multiblock.MultiblockCache;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public class FissionReactorCache extends MultiblockCache<SynchronizedFissionData> {

    public GasStack fuel;
    public GasStack waste;
    public GasStack gasCoolant;
    public GasStack heatedCoolant;

    public FluidStack coolant;
    public FluidStack steam;

    public double rateLimit = SynchronizedFissionData.getDefaultRateLimit();
    public boolean active;

    public double burnRemaining;
    public double partialWaste;
    public double temperature = SynchronizedFissionData.BASE_TEMPERATURE;
    public double reactorDamage;
    public boolean forceDisable;

    @Override
    public void apply(SynchronizedFissionData data) {
        data.updateCapacities();
        data.fuelTank.setGas(fuel == null ? null : fuel.copy());
        data.wasteTank.setGas(waste == null ? null : waste.copy());
        data.gasCoolantTank.setGas(gasCoolant == null ? null : gasCoolant.copy());
        data.heatedCoolantTank.setGas(heatedCoolant == null ? null : heatedCoolant.copy());
        data.coolantTank.setFluid(coolant == null ? null : coolant.copy());
        data.steamTank.setFluid(steam == null ? null : steam.copy());

        data.rateLimit = rateLimit;
        data.active = active;
        data.burnRemaining = burnRemaining;
        data.partialWaste = partialWaste;
        data.temperature = temperature;
        data.reactorDamage = reactorDamage;
        data.forceDisable = forceDisable;
        data.updateCapacities();
    }

    @Override
    public void sync(SynchronizedFissionData data) {
        fuel = data.fuelTank.getGas() == null ? null : data.fuelTank.getGas().copy();
        waste = data.wasteTank.getGas() == null ? null : data.wasteTank.getGas().copy();
        gasCoolant = data.gasCoolantTank.getGas() == null ? null : data.gasCoolantTank.getGas().copy();
        heatedCoolant = data.heatedCoolantTank.getGas() == null ? null : data.heatedCoolantTank.getGas().copy();
        coolant = data.coolantTank.getFluid() == null ? null : data.coolantTank.getFluid().copy();
        steam = data.steamTank.getFluid() == null ? null : data.steamTank.getFluid().copy();

        rateLimit = data.rateLimit;
        active = data.active;
        burnRemaining = data.burnRemaining;
        partialWaste = data.partialWaste;
        temperature = data.temperature;
        reactorDamage = data.reactorDamage;
        forceDisable = data.forceDisable;
    }

    @Override
    public void load(NBTTagCompound nbtTags) {
        if (nbtTags.hasKey("cachedFuel")) {
            fuel = GasStack.readFromNBT(nbtTags.getCompoundTag("cachedFuel"));
        }
        if (nbtTags.hasKey("cachedWaste")) {
            waste = GasStack.readFromNBT(nbtTags.getCompoundTag("cachedWaste"));
        }
        if (nbtTags.hasKey("cachedGasCoolant")) {
            gasCoolant = GasStack.readFromNBT(nbtTags.getCompoundTag("cachedGasCoolant"));
        }
        if (nbtTags.hasKey("cachedHeatedCoolant")) {
            heatedCoolant = GasStack.readFromNBT(nbtTags.getCompoundTag("cachedHeatedCoolant"));
        }
        if (nbtTags.hasKey("cachedCoolant")) {
            coolant = FluidStack.loadFluidStackFromNBT(nbtTags.getCompoundTag("cachedCoolant"));
        }
        if (nbtTags.hasKey("cachedSteam")) {
            steam = FluidStack.loadFluidStackFromNBT(nbtTags.getCompoundTag("cachedSteam"));
        }

        rateLimit = nbtTags.hasKey("fissionRateLimit") ? nbtTags.getDouble("fissionRateLimit") : SynchronizedFissionData.getDefaultRateLimit();
        active = nbtTags.getBoolean("fissionActive");
        burnRemaining = nbtTags.getDouble("fissionBurnRemaining");
        partialWaste = nbtTags.getDouble("fissionPartialWaste");
        temperature = nbtTags.hasKey("fissionTemperature") ? nbtTags.getDouble("fissionTemperature") : SynchronizedFissionData.BASE_TEMPERATURE;
        reactorDamage = nbtTags.getDouble("fissionReactorDamage");
        forceDisable = nbtTags.getBoolean("fissionForceDisable");
    }

    @Override
    public void save(NBTTagCompound nbtTags) {
        if (fuel != null) {
            nbtTags.setTag("cachedFuel", fuel.write(new NBTTagCompound()));
        }
        if (waste != null) {
            nbtTags.setTag("cachedWaste", waste.write(new NBTTagCompound()));
        }
        if (gasCoolant != null) {
            nbtTags.setTag("cachedGasCoolant", gasCoolant.write(new NBTTagCompound()));
        }
        if (heatedCoolant != null) {
            nbtTags.setTag("cachedHeatedCoolant", heatedCoolant.write(new NBTTagCompound()));
        }
        if (coolant != null) {
            nbtTags.setTag("cachedCoolant", coolant.writeToNBT(new NBTTagCompound()));
        }
        if (steam != null) {
            nbtTags.setTag("cachedSteam", steam.writeToNBT(new NBTTagCompound()));
        }

        nbtTags.setDouble("fissionRateLimit", rateLimit);
        nbtTags.setBoolean("fissionActive", active);
        nbtTags.setDouble("fissionBurnRemaining", burnRemaining);
        nbtTags.setDouble("fissionPartialWaste", partialWaste);
        nbtTags.setDouble("fissionTemperature", temperature);
        nbtTags.setDouble("fissionReactorDamage", reactorDamage);
        nbtTags.setBoolean("fissionForceDisable", forceDisable);
    }
}
