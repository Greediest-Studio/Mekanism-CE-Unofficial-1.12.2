package mekanism.common.util;

import mekanism.common.base.IFactory;
import mekanism.common.block.states.BlockStateMachine;
import net.minecraft.util.EnumFacing;

import java.util.Objects;

public class ModelKey {
    final BlockStateMachine.MachineType type;
    final Boolean active;
    final EnumFacing facing;
    final IFactory.RecipeType recipe;

    public ModelKey(BlockStateMachine.MachineType type, Boolean active, EnumFacing facing, IFactory.RecipeType recipe) {
        this.type = type;
        this.active = active;
        this.facing = facing;
        this.recipe = recipe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelKey modelKey = (ModelKey) o;
        return type == modelKey.type &&
                Objects.equals(active, modelKey.active) &&
                facing == modelKey.facing &&
                recipe == modelKey.recipe;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, active, facing, recipe);
    }
}
