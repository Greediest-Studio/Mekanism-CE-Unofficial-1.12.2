package mekanism.common.base;

import mekanism.common.Upgrade;
import mekanism.common.tile.component.TileComponentUpgrade;

public interface IUpgradeTile extends IGetBackMachine {

    default boolean supportsUpgrades() {
        return true;
    }

    default boolean supportsUpgrade(Upgrade upgradeType) {
        return supportsUpgrades() && getComponent().supports(upgradeType);
    }

    TileComponentUpgrade getComponent();


}
