package mekanism.client.render;

import mekanism.common.base.ISpecialSelectionWireframeTile;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.*;

@SideOnly(Side.CLIENT)
public final class SpecialSelectionWireframeRegistry {

    private static final JsonModelSelectionBoxCache.OutlineBox[] EMPTY_OUTLINES = new JsonModelSelectionBoxCache.OutlineBox[0];
    private static final Map<String, ISpecialSelectionWireframeProvider> PROVIDERS = new HashMap<>();
    private static final ISpecialSelectionWireframeTile.SelectionTransform[] ROTATE_Y_180 = {
            ISpecialSelectionWireframeTile.SelectionTransform.rotateY(180, 0.5D, 0.5D, 0.5D)
    };
    private static final ISpecialSelectionWireframeTile.SelectionTransform[] ROTATE_Y_90 = {
            ISpecialSelectionWireframeTile.SelectionTransform.rotateY(90, 0.5D, 0.5D, 0.5D)
    };
    private static final ISpecialSelectionWireframeTile.SelectionTransform[] ROTATE_Y_270 = {
            ISpecialSelectionWireframeTile.SelectionTransform.rotateY(270, 0.5D, 0.5D, 0.5D)
    };
    private static final ISpecialSelectionWireframeTile.SelectionTransform[] ROTATE_X_NEG_90 = {
            ISpecialSelectionWireframeTile.SelectionTransform.rotateX(-90, 0.5D, 0.5D, 0.5D)
    };
    private static final ISpecialSelectionWireframeTile.SelectionTransform[] ROTATE_X_90 = {
            ISpecialSelectionWireframeTile.SelectionTransform.rotateX(90, 0.5D, 0.5D, 0.5D)
    };

    private SpecialSelectionWireframeRegistry() {
    }

    public static JsonModelSelectionBoxCache.OutlineBox[] getWireframes(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (state == null || world == null || pos == null) {
            return EMPTY_OUTLINES;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof ISpecialSelectionWireframeTile specialTile)) {
            return EMPTY_OUTLINES;
        }
        ISpecialSelectionWireframeProvider provider = getOrCreateProvider(specialTile);
        if (provider == null) {
            return EMPTY_OUTLINES;
        }
        JsonModelSelectionBoxCache.OutlineBox[] wireframes = provider.getWireframes(state, world, pos);
        if (wireframes != null && wireframes.length > 0) {
            return wireframes;
        }
        return EMPTY_OUTLINES;
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private static ISpecialSelectionWireframeProvider getOrCreateProvider(ISpecialSelectionWireframeTile specialTile) {
        String normalizedKey = normalizeKey(specialTile.getSelectionWireframeKey());
        if (normalizedKey == null) {
            return null;
        }
        ISpecialSelectionWireframeProvider cached = PROVIDERS.get(normalizedKey);
        if (cached != null) {
            return cached;
        }
        ISpecialSelectionWireframeProvider created = createProvider(specialTile);
        if (created != null) {
            PROVIDERS.put(normalizedKey, created);
        }
        return created;
    }

    private static ISpecialSelectionWireframeProvider createProvider(ISpecialSelectionWireframeTile specialTile) {
        Class<?> modelClass = specialTile.getSelectionWireframeModelClass();
        if (modelClass == null) {
            return null;
        }
        if (!ModelBase.class.isAssignableFrom(modelClass)) {
            return null;
        }
        try {
            Object instance = modelClass.getDeclaredConstructor().newInstance();
            String[] sideArrayFieldNames = specialTile.getSelectionWireframeSideArrayFieldNames();
            String[] ignoredRendererFieldNames = specialTile.getSelectionWireframeIgnoredRendererFieldNames();
            return new ModelBoxWireframeProvider((ModelBase) instance, sideArrayFieldNames, ignoredRendererFieldNames);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static EnumFacing getFacing(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (world != null && pos != null) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityBasicBlock basicBlock && basicBlock.facing != null) {
                return basicBlock.facing;
            }
        }
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
            IProperty<?> property = entry.getKey();
            if (!"facing".equals(property.getName())) {
                continue;
            }
            Comparable<?> value = entry.getValue();
            if (value instanceof EnumFacing) {
                return (EnumFacing) value;
            }
        }
        return EnumFacing.NORTH;
    }

    private static final double MODEL_SCALE = 16D;
    private static final double MIN_AXIS_THICKNESS = 1.0E-6D;

    private static JsonModelSelectionBoxCache.OutlineBox[] extractModelOutlines(List<ModelRenderer> renderers) {
        if (renderers == null || renderers.isEmpty()) {
            return EMPTY_OUTLINES;
        }
        List<Vec3d[]> cornerSets = new ArrayList<>();
        Set<ModelRenderer> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ModelRenderer renderer : renderers) {
            addRendererOutlines(renderer, RendererTransform.IDENTITY, cornerSets, visited);
        }
        return JsonModelSelectionBoxCache.toOutlineBoxesFromCorners(cornerSets);
    }

    private static Set<String> createIgnoredFieldSet(String[] ignoredRendererFieldNames) {
        if (ignoredRendererFieldNames == null || ignoredRendererFieldNames.length == 0) {
            return Collections.emptySet();
        }
        Set<String> ignored = new HashSet<>();
        for (String fieldName : ignoredRendererFieldNames) {
            if (fieldName == null) {
                continue;
            }
            String normalized = fieldName.trim();
            if (!normalized.isEmpty()) {
                ignored.add(normalized);
            }
        }
        return ignored.isEmpty() ? Collections.emptySet() : ignored;
    }

    private static List<ModelRenderer> getModelRenderers(ModelBase model, Set<String> ignoredFieldNames) {
        List<ModelRenderer> renderers = new ArrayList<>();
        for (Field field : getModelRendererFields(model.getClass())) {
            try {
                if (ignoredFieldNames != null && ignoredFieldNames.contains(field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(model);
                if (value instanceof ModelRenderer) {
                    renderers.add((ModelRenderer) value);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return renderers;
    }

    private static List<Field> getModelRendererFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (ModelRenderer.class.isAssignableFrom(field.getType())) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static Field findField(Class<?> type, String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Class<?> current = type;
        String target = name.trim();
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(target);
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static List<ModelRenderer[]> getSideRendererArrays(ModelBase model, String[] sideArrayFieldNames) {
        if (model == null || sideArrayFieldNames == null || sideArrayFieldNames.length == 0) {
            return Collections.emptyList();
        }
        List<ModelRenderer[]> arrays = new ArrayList<>();
        for (String fieldName : sideArrayFieldNames) {
            Field field = findField(model.getClass(), fieldName);
            if (field == null) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(model);
                if (value instanceof ModelRenderer[]) {
                    arrays.add(((ModelRenderer[]) value).clone());
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return arrays;
    }

    private static void addRendererOutlines(ModelRenderer renderer, RendererTransform parentTransform, List<Vec3d[]> cornerSets, Set<ModelRenderer> visited) {
        if (renderer == null || !visited.add(renderer)) {
            return;
        }
        RendererTransform currentTransform = parentTransform.combine(RendererTransform.fromRenderer(renderer));

        if (renderer.cubeList != null) {
            for (Object cubeObject : renderer.cubeList) {
                if (!(cubeObject instanceof ModelBox cube)) {
                    continue;
                }
                Vec3d[] corners = convertModelBoxToCorners(cube, currentTransform);
                if (corners != null) {
                    cornerSets.add(corners);
                }
            }
        }

        if (renderer.childModels != null) {
            for (Object childObject : renderer.childModels) {
                if (childObject instanceof ModelRenderer childRenderer) {
                    addRendererOutlines(childRenderer, currentTransform, cornerSets, visited);
                }
            }
        }
    }

    private static Vec3d[] convertModelBoxToCorners(ModelBox cube, RendererTransform transform) {
        double cubeMinX = Math.min(cube.posX1, cube.posX2);
        double cubeMaxX = Math.max(cube.posX1, cube.posX2);
        double cubeMinY = Math.min(cube.posY1, cube.posY2);
        double cubeMaxY = Math.max(cube.posY1, cube.posY2);
        double cubeMinZ = Math.min(cube.posZ1, cube.posZ2);
        double cubeMaxZ = Math.max(cube.posZ1, cube.posZ2);
        if ((cubeMaxX - cubeMinX) < MIN_AXIS_THICKNESS
                || (cubeMaxY - cubeMinY) < MIN_AXIS_THICKNESS
                || (cubeMaxZ - cubeMinZ) < MIN_AXIS_THICKNESS) {
            return null;
        }

        Vec3d[] modelCorners = new Vec3d[]{
                transform.apply(cubeMinX, cubeMinY, cubeMinZ),
                transform.apply(cubeMinX, cubeMinY, cubeMaxZ),
                transform.apply(cubeMinX, cubeMaxY, cubeMinZ),
                transform.apply(cubeMinX, cubeMaxY, cubeMaxZ),
                transform.apply(cubeMaxX, cubeMinY, cubeMinZ),
                transform.apply(cubeMaxX, cubeMinY, cubeMaxZ),
                transform.apply(cubeMaxX, cubeMaxY, cubeMinZ),
                transform.apply(cubeMaxX, cubeMaxY, cubeMaxZ)
        };
        Vec3d[] worldCorners = new Vec3d[modelCorners.length];
        for (int i = 0; i < modelCorners.length; i++) {
            Vec3d corner = modelCorners[i];
            // TESR model coordinates are converted by translate(0.5, 1.5, 0.5) and rotate Z 180.
            worldCorners[i] = new Vec3d(
                    0.5D - corner.x / MODEL_SCALE,
                    1.5D - corner.y / MODEL_SCALE,
                    0.5D + corner.z / MODEL_SCALE
            );
        }
        return worldCorners;
    }

    private static final class RendererTransform {

        private static final RendererTransform IDENTITY = new RendererTransform(
                1, 0, 0,
                0, 1, 0,
                0, 0, 1,
                0, 0, 0
        );

        private final double m00;
        private final double m01;
        private final double m02;
        private final double m10;
        private final double m11;
        private final double m12;
        private final double m20;
        private final double m21;
        private final double m22;
        private final double tx;
        private final double ty;
        private final double tz;

        private RendererTransform(double m00, double m01, double m02,
                                  double m10, double m11, double m12,
                                  double m20, double m21, double m22,
                                  double tx, double ty, double tz) {
            this.m00 = m00;
            this.m01 = m01;
            this.m02 = m02;
            this.m10 = m10;
            this.m11 = m11;
            this.m12 = m12;
            this.m20 = m20;
            this.m21 = m21;
            this.m22 = m22;
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
        }

        private static RendererTransform fromRenderer(ModelRenderer renderer) {
            double rx = renderer.rotateAngleX;
            double ry = renderer.rotateAngleY;
            double rz = renderer.rotateAngleZ;

            double cx = Math.cos(rx);
            double sx = Math.sin(rx);
            double cy = Math.cos(ry);
            double sy = Math.sin(ry);
            double cz = Math.cos(rz);
            double sz = Math.sin(rz);

            // ModelRenderer applies rotations in X -> Y -> Z order (post-translate).
            // Combined matrix is Rz * Ry * Rx.
            double r00 = cz * cy;
            double r01 = cz * sy * sx - sz * cx;
            double r02 = cz * sy * cx + sz * sx;
            double r10 = sz * cy;
            double r11 = sz * sy * sx + cz * cx;
            double r12 = sz * sy * cx - cz * sx;
            double r20 = -sy;
            double r21 = cy * sx;
            double r22 = cy * cx;

            double offsetX = renderer.offsetX * MODEL_SCALE;
            double offsetY = renderer.offsetY * MODEL_SCALE;
            double offsetZ = renderer.offsetZ * MODEL_SCALE;

            double tx = offsetX + renderer.rotationPointX;
            double ty = offsetY + renderer.rotationPointY;
            double tz = offsetZ + renderer.rotationPointZ;

            return new RendererTransform(r00, r01, r02, r10, r11, r12, r20, r21, r22, tx, ty, tz);
        }

        private RendererTransform combine(RendererTransform local) {
            return new RendererTransform(
                    m00 * local.m00 + m01 * local.m10 + m02 * local.m20,
                    m00 * local.m01 + m01 * local.m11 + m02 * local.m21,
                    m00 * local.m02 + m01 * local.m12 + m02 * local.m22,
                    m10 * local.m00 + m11 * local.m10 + m12 * local.m20,
                    m10 * local.m01 + m11 * local.m11 + m12 * local.m21,
                    m10 * local.m02 + m11 * local.m12 + m12 * local.m22,
                    m20 * local.m00 + m21 * local.m10 + m22 * local.m20,
                    m20 * local.m01 + m21 * local.m11 + m22 * local.m21,
                    m20 * local.m02 + m21 * local.m12 + m22 * local.m22,
                    m00 * local.tx + m01 * local.ty + m02 * local.tz + tx,
                    m10 * local.tx + m11 * local.ty + m12 * local.tz + ty,
                    m20 * local.tx + m21 * local.ty + m22 * local.tz + tz
            );
        }

        private Vec3d apply(double x, double y, double z) {
            return new Vec3d(
                    m00 * x + m01 * y + m02 * z + tx,
                    m10 * x + m11 * y + m12 * z + ty,
                    m20 * x + m21 * y + m22 * z + tz
            );
        }
    }

    private static final class ModelBoxWireframeProvider implements ISpecialSelectionWireframeProvider {

        private final ModelBase model;
        private final List<ModelRenderer> alwaysRenderers;
        private final List<ModelRenderer[]> sideRendererArrays;
        private final Map<String, JsonModelSelectionBoxCache.OutlineBox[]> cache = new HashMap<>();

        private ModelBoxWireframeProvider(ModelBase model, String[] sideArrayFieldNames, String[] ignoredRendererFieldNames) {
            this.model = model;
            sideRendererArrays = getSideRendererArrays(model, sideArrayFieldNames);
            Set<String> ignoredFields = createIgnoredFieldSet(ignoredRendererFieldNames);

            Set<ModelRenderer> conditional = Collections.newSetFromMap(new IdentityHashMap<>());
            for (ModelRenderer[] sideArray : sideRendererArrays) {
                if (sideArray == null) {
                    continue;
                }
                for (ModelRenderer renderer : sideArray) {
                    if (renderer != null) {
                        conditional.add(renderer);
                    }
                }
            }

            List<ModelRenderer> allRenderers = getModelRenderers(model, ignoredFields);
            alwaysRenderers = new ArrayList<>(allRenderers.size());
            for (ModelRenderer renderer : allRenderers) {
                if (renderer != null && !conditional.contains(renderer)) {
                    alwaysRenderers.add(renderer);
                }
            }
        }

        @Override
        public JsonModelSelectionBoxCache.OutlineBox[] getWireframes(IBlockState state, IBlockAccess world, BlockPos pos) {
            ISpecialSelectionWireframeTile specialTile = getSpecialTile(world, pos);
            int sideMask = getSideMask(specialTile, state, world, pos);
            EnumFacing facing = getFacing(state, world, pos);
            boolean applyDefaultFacingRotation = shouldApplyDefaultFacingRotation(specialTile, state, world, pos);
            ISpecialSelectionWireframeTile.SelectionTransform[] transforms = getTransforms(specialTile, state, world, pos);
            int animationKey = getAnimationCacheKey(specialTile, state, world, pos);
            String cacheKey = facing.ordinal() + "|" + sideMask + "|" + (applyDefaultFacingRotation ? "1" : "0") + "|" + Arrays.hashCode(transforms) + "|" + animationKey;
            JsonModelSelectionBoxCache.OutlineBox[] cached = cache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            applyModelState(specialTile, model, state, world, pos);

            List<ModelRenderer> renderers = new ArrayList<>(alwaysRenderers.size() + sideRendererArrays.size() * 6);
            renderers.addAll(alwaysRenderers);
            if (!sideRendererArrays.isEmpty() && sideMask != 0) {
                for (EnumFacing side : EnumFacing.VALUES) {
                    if ((sideMask & (1 << side.ordinal())) == 0) {
                        continue;
                    }
                    int sideIndex = side.ordinal();
                    for (ModelRenderer[] sideArray : sideRendererArrays) {
                        if (sideArray != null && sideIndex < sideArray.length) {
                            ModelRenderer renderer = sideArray[sideIndex];
                            if (renderer != null) {
                                renderers.add(renderer);
                            }
                        }
                    }
                }
            }

            JsonModelSelectionBoxCache.OutlineBox[] outlines = extractModelOutlines(renderers);
            if (outlines.length == 0) {
                cache.put(cacheKey, EMPTY_OUTLINES);
                return EMPTY_OUTLINES;
            }

            if (applyDefaultFacingRotation) {
                ISpecialSelectionWireframeTile.SelectionTransform[] facingTransforms = getDefaultFacingTransforms(facing);
                if (facingTransforms.length > 0) {
                    outlines = applyTransforms(outlines, facingTransforms);
                }
            }

            if (transforms.length > 0) {
                outlines = applyTransforms(outlines, transforms);
            }
            cache.put(cacheKey, outlines.length == 0 ? EMPTY_OUTLINES : outlines);
            return outlines.length == 0 ? EMPTY_OUTLINES : outlines;
        }

        private static ISpecialSelectionWireframeTile getSpecialTile(IBlockAccess world, BlockPos pos) {
            TileEntity tile = world == null || pos == null ? null : world.getTileEntity(pos);
            if (tile instanceof ISpecialSelectionWireframeTile specialTile) {
                return specialTile;
            }
            return null;
        }

        private static int getSideMask(ISpecialSelectionWireframeTile specialTile, IBlockState state, IBlockAccess world, BlockPos pos) {
            if (specialTile == null) {
                return 0x3F;
            }
            int mask = 0;
            for (EnumFacing side : EnumFacing.VALUES) {
                if (specialTile.shouldRenderSelectionWireframeSide(side, state, world, pos)) {
                    mask |= 1 << side.ordinal();
                }
            }
            return mask;
        }

        private static ISpecialSelectionWireframeTile.SelectionTransform[] getTransforms(ISpecialSelectionWireframeTile specialTile, IBlockState state, IBlockAccess world, BlockPos pos) {
            if (specialTile == null) {
                return ISpecialSelectionWireframeTile.SelectionTransform.EMPTY;
            }
            ISpecialSelectionWireframeTile.SelectionTransform[] transforms = specialTile.getSelectionWireframeTransforms(state, world, pos);
            return transforms == null ? ISpecialSelectionWireframeTile.SelectionTransform.EMPTY : transforms;
        }

        private static int getAnimationCacheKey(ISpecialSelectionWireframeTile specialTile, IBlockState state, IBlockAccess world, BlockPos pos) {
            if (specialTile == null) {
                return 0;
            }
            return specialTile.getSelectionWireframeAnimationCacheKey(state, world, pos);
        }

        private static void applyModelState(ISpecialSelectionWireframeTile specialTile, ModelBase model, IBlockState state, IBlockAccess world, BlockPos pos) {
            if (specialTile == null || model == null) {
                return;
            }
            specialTile.applySelectionWireframeModelState(model, state, world, pos);
        }

        private static boolean shouldApplyDefaultFacingRotation(ISpecialSelectionWireframeTile specialTile, IBlockState state, IBlockAccess world, BlockPos pos) {
            return specialTile == null || specialTile.shouldApplyDefaultSelectionWireframeFacingRotation(state, world, pos);
        }

        private static ISpecialSelectionWireframeTile.SelectionTransform[] getDefaultFacingTransforms(EnumFacing facing) {
            if (facing == null) {
                return ISpecialSelectionWireframeTile.SelectionTransform.EMPTY;
            }
            return switch (facing) {
                case SOUTH -> ROTATE_Y_180;
                case WEST -> ROTATE_Y_90;
                case EAST -> ROTATE_Y_270;
                case UP -> ROTATE_X_NEG_90;
                case DOWN -> ROTATE_X_90;
                default -> ISpecialSelectionWireframeTile.SelectionTransform.EMPTY;
            };
        }

        private static JsonModelSelectionBoxCache.OutlineBox[] applyTransforms(JsonModelSelectionBoxCache.OutlineBox[] outlines, ISpecialSelectionWireframeTile.SelectionTransform[] transforms) {
            if (outlines == null || outlines.length == 0) {
                return EMPTY_OUTLINES;
            }
            if (transforms == null || transforms.length == 0) {
                return outlines;
            }
            List<Vec3d[]> transformedCornerSets = new ArrayList<>(outlines.length);
            for (int i = 0; i < outlines.length; i++) {
                JsonModelSelectionBoxCache.OutlineBox transformed = applyTransforms(outlines[i], transforms);
                if (transformed != null) {
                    transformedCornerSets.add(transformed.getCorners());
                }
            }
            return JsonModelSelectionBoxCache.toOutlineBoxesFromCorners(transformedCornerSets);
        }

        private static JsonModelSelectionBoxCache.OutlineBox applyTransforms(JsonModelSelectionBoxCache.OutlineBox outline, ISpecialSelectionWireframeTile.SelectionTransform[] transforms) {
            if (outline == null || transforms == null || transforms.length == 0) {
                return outline;
            }
            Vec3d[] corners = copyCorners(outline.getCorners());
            for (ISpecialSelectionWireframeTile.SelectionTransform transform : transforms) {
                if (transform == null) {
                    continue;
                }
                for (int i = 0; i < corners.length; i++) {
                    corners[i] = applyTransform(corners[i], transform);
                }
            }
            JsonModelSelectionBoxCache.OutlineBox[] converted = JsonModelSelectionBoxCache.toOutlineBoxesFromCorners(Collections.singletonList(corners));
            return converted.length == 0 ? null : converted[0];
        }

        private static Vec3d applyTransform(Vec3d point, ISpecialSelectionWireframeTile.SelectionTransform transform) {
            switch (transform.getType()) {
                case TRANSLATE:
                    return new Vec3d(point.x + transform.getX(), point.y + transform.getY(), point.z + transform.getZ());
                case ROTATE_X:
                    return rotateX(point, transform.getAngle(), transform.getX(), transform.getY(), transform.getZ());
                case ROTATE_Y:
                    return rotateY(point, transform.getAngle(), transform.getX(), transform.getY(), transform.getZ());
                case ROTATE_Z:
                    return rotateZ(point, transform.getAngle(), transform.getX(), transform.getY(), transform.getZ());
                default:
                    return point;
            }
        }

        private static Vec3d rotateX(Vec3d point, double angle, double ox, double oy, double oz) {
            if (Math.abs(angle) < 1.0E-7D) {
                return point;
            }
            double radians = Math.toRadians(angle);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            double x = point.x - ox;
            double y = point.y - oy;
            double z = point.z - oz;
            double ry = y * cos - z * sin;
            double rz = y * sin + z * cos;
            return new Vec3d(x + ox, ry + oy, rz + oz);
        }

        private static Vec3d rotateY(Vec3d point, double angle, double ox, double oy, double oz) {
            if (Math.abs(angle) < 1.0E-7D) {
                return point;
            }
            double radians = Math.toRadians(angle);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            double x = point.x - ox;
            double y = point.y - oy;
            double z = point.z - oz;
            double rx = x * cos + z * sin;
            double rz = -x * sin + z * cos;
            return new Vec3d(rx + ox, y + oy, rz + oz);
        }

        private static Vec3d rotateZ(Vec3d point, double angle, double ox, double oy, double oz) {
            if (Math.abs(angle) < 1.0E-7D) {
                return point;
            }
            double radians = Math.toRadians(angle);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            double x = point.x - ox;
            double y = point.y - oy;
            double z = point.z - oz;
            double rx = x * cos - y * sin;
            double ry = x * sin + y * cos;
            return new Vec3d(rx + ox, ry + oy, z + oz);
        }

        private static Vec3d[] copyCorners(Vec3d[] corners) {
            if (corners == null || corners.length == 0) {
                return new Vec3d[0];
            }
            Vec3d[] copy = new Vec3d[corners.length];
            for (int i = 0; i < corners.length; i++) {
                Vec3d corner = corners[i];
                copy[i] = corner == null ? Vec3d.ZERO : new Vec3d(corner.x, corner.y, corner.z);
            }
            return copy;
        }
    }
}
