package mekanism.multiblockmachine.common.inventory.container;

import mekanism.common.inventory.slot.SlotEnergy;
import mekanism.generators.common.inventory.container.ContainerPassiveGenerator;
import mekanism.multiblockmachine.common.tile.generator.TileEntityLargeWindGenerator;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerLargeWindGenerator extends ContainerPassiveGenerator<TileEntityLargeWindGenerator> {

    public ContainerLargeWindGenerator(InventoryPlayer inventory, TileEntityLargeWindGenerator generator) {
        super(inventory, generator);
    }

    @Override
    protected int getInventorYOffset() {
        return 93;
    }

    @Override
    protected void addSlots() {
        addSlotToContainer(new SlotEnergy.SlotCharge(tileEntity, 0, 143, 37));
    }
}
