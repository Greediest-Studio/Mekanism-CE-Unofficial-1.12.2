package mekanism.common.util;

import mekanism.common.MekanismItems;
import mekanism.common.MekanismLang;
import mekanism.common.Upgrade;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.config.MekanismConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.List;

public class UpgradeUtils {

    private UpgradeUtils() {
    }

    public static ItemStack getStack(Upgrade upgrade) {
        return getStack(upgrade, 1);
    }

    public static ItemStack getStack(Upgrade upgrade, int count) {
        switch (upgrade) {
            case SPEED -> new ItemStack(MekanismItems.SpeedUpgrade, count);
            case ENERGY -> new ItemStack(MekanismItems.EnergyUpgrade, count);
            case FILTER -> new ItemStack(MekanismItems.FilterUpgrade, count);
            case MUFFLING -> new ItemStack(MekanismItems.MufflingUpgrade, count);
            case GAS -> new ItemStack(MekanismItems.GasUpgrade, count);
            case ANCHOR -> new ItemStack(MekanismItems.AnchorUpgrade, count);
            case STONE_GENERATOR -> new ItemStack(MekanismItems.StoneGeneratorUpgrade, count);
            case THREAD -> new ItemStack(MekanismItems.ThreadUpgrade, count);
        }
        return ItemStack.EMPTY;
    }

    public static List<String> getInfo(TileEntity tile, Upgrade upgrade) {
        List<String> ret = new ArrayList<>();
        if (tile instanceof IUpgradeTile) {
            if (tile instanceof Upgrade.IUpgradeInfoHandler handler) {
                return handler.getInfo(upgrade);
            } else {
                ret = getMultScaledInfo((IUpgradeTile) tile, upgrade);
            }
        }
        return ret;
    }

    public static List<String> getMultScaledInfo(IUpgradeTile tile, Upgrade upgrade) {
        List<String> ret = new ArrayList<>();
        if (tile.supportsUpgrades() && upgrade.getMaxInstalled() > 1) {
            double effect = Math.pow(MekanismConfig.current().general.maxUpgradeMultiplier.val(), (float) tile.getComponent().getUpgrades(upgrade) / (float) upgrade.getMaxInstalled());
            ret.add(MekanismLang.UPGRADES_EFFECT.translate(Math.round(effect * 100) / 100F).getFormattedText());
        }
        return ret;
    }

    public static List<String> getExpScaledInfo(IUpgradeTile tile, Upgrade upgrade) {
        List<String> ret = new ArrayList<>();
        if (tile.supportsUpgrades() && upgrade.getMaxInstalled() > 1) {
            ret.add(MekanismLang.UPGRADES_EFFECT.translate(Math.pow(2, (float) tile.getComponent().getUpgrades(upgrade))).getFormattedText());
        }
        return ret;
    }
}
