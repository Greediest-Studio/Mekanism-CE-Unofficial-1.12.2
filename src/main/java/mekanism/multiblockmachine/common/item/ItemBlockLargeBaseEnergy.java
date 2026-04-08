package mekanism.multiblockmachine.common.item;

import cofh.redstoneflux.api.IEnergyContainerItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;
import mekanism.api.EnumColor;
import mekanism.api.energy.IEnergizedItem;
import mekanism.common.Upgrade;
import mekanism.common.capabilities.ItemCapabilityWrapper;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.forgeenergy.ForgeEnergyItemWrapper;
import mekanism.common.integration.ic2.IC2ItemManager;
import mekanism.common.integration.redstoneflux.RFIntegration;
import mekanism.common.integration.tesla.TeslaItemWrapper;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

@InterfaceList({
        @Interface(iface = "cofh.redstoneflux.api.IEnergyContainerItem", modid = MekanismHooks.REDSTONEFLUX_MOD_ID),
        @Interface(iface = "ic2.api.item.ISpecialElectricItem", modid = MekanismHooks.IC2_MOD_ID)
})
public abstract class ItemBlockLargeBaseEnergy extends ItemBlockLargeBase implements IEnergizedItem, ISpecialElectricItem, IEnergyContainerItem {

    public String name;

    public ItemBlockLargeBaseEnergy(Block block, String machine) {
        super(block, machine);
    }


    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack itemstack, World world, @Nonnull List<String> list, @Nonnull ITooltipFlag flag) {
        super.addInformation(itemstack, world, list, flag);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemstack, List<String> list, World world, ITooltipFlag flag) {
        if (itemstack.getItem() instanceof IEnergizedItem energizedItem && itemstack.getCount() <= 1) {
            list.add(EnumColor.BRIGHT_GREEN + LangUtils.localize("tooltip.storedEnergy") + ": " + EnumColor.GREY + MekanismUtils.getEnergyDisplay(energizedItem.getEnergy(itemstack), energizedItem.getMaxEnergy(itemstack)));
        }
    }

    @Override
    public double getEnergy(ItemStack itemStack) {
        if (itemStack.getCount() > 1) {
            return 0;
        }
        if (!itemStack.hasTagCompound()) {
            return 0;
        }
        return ItemDataUtils.getDouble(itemStack, "energyStored");
    }

    @Override
    public void setEnergy(ItemStack itemStack, double amount) {
        if (itemStack.getCount() > 1) {
            return;
        }
        if (amount == 0) {
            NBTTagCompound dataMap = ItemDataUtils.getDataMap(itemStack);
            dataMap.removeTag("energyStored");
            if (dataMap.isEmpty() && itemStack.getTagCompound() != null) {
                itemStack.getTagCompound().removeTag(ItemDataUtils.DATA_ID);
            }
        } else {
            ItemDataUtils.setDouble(itemStack, "energyStored", Math.max(Math.min(amount, getMaxEnergy(itemStack)), 0));
        }
    }

    @Override
    public double getMaxEnergy(ItemStack itemStack) {
        if (itemStack.getCount() > 1) {
            return 0;
        }
        double storage = getMachineStorage() * getThread(itemStack);
        return ItemDataUtils.hasData(itemStack, "upgrades") ? MekanismUtils.getMaxEnergy(itemStack, storage) : storage;
    }

    abstract double getMachineStorage();

    private int getThread(ItemStack stack) {
        int thread = 1;
        Map<Upgrade, Integer> upgrades = Upgrade.buildMap(ItemDataUtils.getDataMap(stack));
        if (upgrades.get(Upgrade.THREAD) != null) {
            thread += upgrades.get(Upgrade.THREAD);
        }
        NBTTagCompound dataMap = ItemDataUtils.getDataMap(stack);
        if (dataMap.isEmpty() && stack.getTagCompound() != null) {
            stack.getTagCompound().removeTag(ItemDataUtils.DATA_ID);
        }
        if (stack.getTagCompound() != null && stack.getTagCompound().isEmpty()) {
            stack.setTagCompound(null);
        }
        return thread;
    }

    @Override
    public double getMaxTransfer(ItemStack itemStack) {
        if (itemStack.getCount() > 1) {
            return 0;
        }
        return getMaxEnergy(itemStack) * 0.005;
    }

    @Override
    public boolean canReceive(ItemStack itemStack) {
        return itemStack.getCount() <= 1;
    }

    @Override
    public boolean canSend(ItemStack itemStack) {
        return false;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int receiveEnergy(ItemStack theItem, int energy, boolean simulate) {
        if (theItem.getCount() > 1) {
            return 0;
        }
        if (canReceive(theItem)) {
            double energyNeeded = getMaxEnergy(theItem) - getEnergy(theItem);
            double toReceive = Math.min(RFIntegration.fromRF(energy), energyNeeded);
            if (!simulate) {
                setEnergy(theItem, getEnergy(theItem) + toReceive);
            }
            return RFIntegration.toRF(toReceive);
        }
        return 0;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int extractEnergy(ItemStack theItem, int energy, boolean simulate) {
        if (theItem.getCount() > 1) {
            return 0;
        }
        if (canSend(theItem)) {
            double energyRemaining = getEnergy(theItem);
            double toSend = Math.min(RFIntegration.fromRF(energy), energyRemaining);
            if (!simulate) {
                setEnergy(theItem, getEnergy(theItem) - toSend);
            }
            return RFIntegration.toRF(toSend);
        }
        return 0;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int getEnergyStored(ItemStack theItem) {
        if (theItem.getCount() > 1) {
            return 0;
        }
        return RFIntegration.toRF(getEnergy(theItem));
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int getMaxEnergyStored(ItemStack theItem) {
        if (theItem.getCount() > 1) {
            return 0;
        }
        return RFIntegration.toRF(getMaxEnergy(theItem));
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getEnergy(stack) > 0;
    }


    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1D - (getEnergy(stack) / getMaxEnergy(stack));
    }

    @Override
    @Optional.Method(modid = MekanismHooks.IC2_MOD_ID)
    public IElectricItemManager getManager(ItemStack itemStack) {
        return IC2ItemManager.getManager(this);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
        return new ItemCapabilityWrapper(stack, new TeslaItemWrapper(), new ForgeEnergyItemWrapper());
    }

}
