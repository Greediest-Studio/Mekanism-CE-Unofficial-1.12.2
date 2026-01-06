package mekanism.generators.common.inventory.container;

import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasItem;
import mekanism.common.inventory.slot.SlotEnergy.SlotCharge;
import mekanism.common.recipe.RecipeHandler;
import mekanism.generators.common.tile.TileEntityGasGenerator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerGasGenerator extends ContainerFuelGenerator<TileEntityGasGenerator> {

    public ContainerGasGenerator(InventoryPlayer inventory, TileEntityGasGenerator generator) {
        super(inventory, generator);
    }

    @Override
    protected void addSlots() {
        addSlotToContainer(new Slot(tileEntity, 0, 17, 35));
        addSlotToContainer(new SlotCharge(tileEntity, 1, 143, 35));
    }

    @Override
    protected boolean tryFuel(ItemStack slotStack) {
        if (slotStack.getItem() instanceof IGasItem gasItem) {
            GasStack gasStack = gasItem.getGas(slotStack);
            return gasStack != null && RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.containsRecipe(gasStack.getGas());
        }
        return false;
    }
}
