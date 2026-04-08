package mekanism.common.base;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.util.Objects;

public interface ISpecialSelectionWireframeTile {

    default String getSelectionWireframeKey() {
        Class<?> modelClass = getSelectionWireframeModelClass();
        if (modelClass != null) {
            return modelClass.getName();
        }
        return getClass().getName();
    }

    default Class<?> getSelectionWireframeModelClass() {
        return null;
    }

    default String[] getSelectionWireframeSideArrayFieldNames() {
        return new String[0];
    }

    default String[] getSelectionWireframeIgnoredRendererFieldNames() {
        return new String[0];
    }

    default int getSelectionWireframeAnimationCacheKey(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    default void applySelectionWireframeModelState(Object model, IBlockState state, IBlockAccess world, BlockPos pos) {
    }

    default SelectionTransform[] getSelectionWireframeTransforms(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SelectionTransform.EMPTY;
    }

    default boolean shouldApplyDefaultSelectionWireframeFacingRotation(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }

    default boolean shouldRenderSelectionWireframeSide(EnumFacing side, IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }

    final class SelectionTransform {

        public static final SelectionTransform[] EMPTY = new SelectionTransform[0];

        public enum Type {
            TRANSLATE,
            ROTATE_X,
            ROTATE_Y,
            ROTATE_Z
        }

        private final Type type;
        private final double x;
        private final double y;
        private final double z;
        private final double angle;

        private SelectionTransform(Type type, double x, double y, double z, double angle) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.angle = angle;
        }

        public static SelectionTransform translate(double x, double y, double z) {
            return new SelectionTransform(Type.TRANSLATE, x, y, z, 0);
        }

        public static SelectionTransform rotateX(double angle, double pivotX, double pivotY, double pivotZ) {
            return new SelectionTransform(Type.ROTATE_X, pivotX, pivotY, pivotZ, angle);
        }

        public static SelectionTransform rotateY(double angle, double pivotX, double pivotY, double pivotZ) {
            return new SelectionTransform(Type.ROTATE_Y, pivotX, pivotY, pivotZ, angle);
        }

        public static SelectionTransform rotateZ(double angle, double pivotX, double pivotY, double pivotZ) {
            return new SelectionTransform(Type.ROTATE_Z, pivotX, pivotY, pivotZ, angle);
        }

        public Type getType() {
            return type;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public double getAngle() {
            return angle;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SelectionTransform other)) {
                return false;
            }
            return type == other.type
                    && Double.compare(x, other.x) == 0
                    && Double.compare(y, other.y) == 0
                    && Double.compare(z, other.z) == 0
                    && Double.compare(angle, other.angle) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, x, y, z, angle);
        }
    }
}
