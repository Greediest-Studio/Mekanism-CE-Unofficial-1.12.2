package mekanism.common.capabilities.radiation.item;

import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.ItemCapabilityWrapper.ItemCapability;
import nc.capability.radiation.resistance.IRadiationResistance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Optional.Interface;

import java.util.Objects;
import java.util.function.ToDoubleFunction;

@Interface(iface = "nc.capability.radiation.resistance.IRadiationResistance", modid = "nuclearcraft")
public class NCRadiationShieldingHandler extends ItemCapability implements IRadiationResistance {

    private static final String NBT_RADIATION_RESISTANCE = "radiationResistance";
    private static final String NBT_SHIELDING_RESISTANCE = "shieldingRadResistance";
    private static final String NBT_LEGACY_ARMOR_RESISTANCE = "ncRadiationResistance";

    public static NCRadiationShieldingHandler create(ToDoubleFunction<ItemStack> baseRadResistance, ToDoubleFunction<ItemStack> shieldingRadResistance) {
        return create(baseRadResistance, shieldingRadResistance, true);
    }

    public static NCRadiationShieldingHandler create(ToDoubleFunction<ItemStack> baseRadResistance, ToDoubleFunction<ItemStack> shieldingRadResistance, boolean allowExternalShielding) {
        Objects.requireNonNull(baseRadResistance, "radiationResistance function cannot be null");
        Objects.requireNonNull(shieldingRadResistance, "shieldingRadResistance function cannot be null");
        return new NCRadiationShieldingHandler(baseRadResistance, shieldingRadResistance, allowExternalShielding);
    }

    private final ToDoubleFunction<ItemStack> baseRadResistance, shieldingRadResistance;
    private final boolean allowExternalShielding;
    private double baseOverride = Double.NaN;
    private double shieldingOverride = Double.NaN;

    private NCRadiationShieldingHandler(ToDoubleFunction<ItemStack> baseRadResistance, ToDoubleFunction<ItemStack> shieldingRadResistance, boolean allowExternalShielding) {
        this.baseRadResistance = baseRadResistance;
        this.shieldingRadResistance = shieldingRadResistance;
        this.allowExternalShielding = allowExternalShielding;
    }

    public static boolean clearLegacyArmorShieldingTag(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey(NBT_LEGACY_ARMOR_RESISTANCE)) {
            return false;
        }
        tag.removeTag(NBT_LEGACY_ARMOR_RESISTANCE);
        if (tag.isEmpty()) {
            stack.setTagCompound(null);
        }
        return true;
    }


    @Override
    public boolean canProcess(Capability<?> capability) {
        return capability == Capabilities.NC_CAPABILITY_RADIATION_RESISTANCE;
    }

    public double getBaseRadResistance() {
        return Double.isNaN(baseOverride) ? Math.max(baseRadResistance.applyAsDouble(getStack()), 0D) : baseOverride;
    }


    public void setBaseRadResistance(double v) {
        if (!allowExternalShielding) {
            return;
        }
        baseOverride = Math.max(v, 0D);
    }

    public double getShieldingRadResistance() {
        return Double.isNaN(shieldingOverride) ? Math.max(shieldingRadResistance.applyAsDouble(getStack()), 0D) : shieldingOverride;
    }

    public void setShieldingRadResistance(double v) {
        if (!allowExternalShielding) {
            return;
        }
        shieldingOverride = Math.max(v, 0D);
    }

    public NBTTagCompound writeNBT(IRadiationResistance iRadiationResistance, EnumFacing enumFacing, NBTTagCompound nbtTagCompound) {
        if (nbtTagCompound == null) {
            nbtTagCompound = new NBTTagCompound();
        }
        nbtTagCompound.setDouble(NBT_RADIATION_RESISTANCE, getBaseRadResistance());
        nbtTagCompound.setDouble(NBT_SHIELDING_RESISTANCE, getShieldingRadResistance());
        return nbtTagCompound;
    }

    public void readNBT(IRadiationResistance iRadiationResistance, EnumFacing enumFacing, NBTTagCompound nbtTagCompound) {
        if (nbtTagCompound == null) {
            return;
        }
        if (nbtTagCompound.hasKey(NBT_SHIELDING_RESISTANCE)) {
            setBaseRadResistance(nbtTagCompound.getDouble(NBT_RADIATION_RESISTANCE));
            setShieldingRadResistance(nbtTagCompound.getDouble(NBT_SHIELDING_RESISTANCE));
        } else if (nbtTagCompound.hasKey(NBT_RADIATION_RESISTANCE)) {
            // Old NC format stores a single resistance value.
            setRadiationResistance(nbtTagCompound.getDouble(NBT_RADIATION_RESISTANCE));
        }
    }


    public double getRadiationResistance() {
        return getBaseRadResistance() + getShieldingRadResistance();
    }

    public void setRadiationResistance(double newResistance) {
        if (!allowExternalShielding) {
            return;
        }
        setBaseRadResistance(0);
        setShieldingRadResistance(newResistance);
    }
}
