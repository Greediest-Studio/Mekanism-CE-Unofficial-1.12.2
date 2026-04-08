package mekanism.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mekanism.common.config.MekanismConfig;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SideOnly(Side.CLIENT)
public final class JsonModelSelectionBoxCache {

    private static final AxisAlignedBB[] EMPTY = new AxisAlignedBB[0];
    private static final OutlineBox[] EMPTY_OUTLINES = new OutlineBox[0];
    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final double MIN_ELEMENT_THICKNESS = 1.0E-6D;
    private static final int[][] EDGE_INDICES = new int[][]{
            {0, 1}, {0, 2}, {0, 4},
            {1, 3}, {1, 5},
            {2, 3}, {2, 6},
            {3, 7},
            {4, 5}, {4, 6},
            {5, 7},
            {6, 7}
    };

    private static final Map<ResourceLocation, OutlineBox[]> MODEL_OUTLINE_CACHE = new HashMap<>();
    private static final Map<String, OutlineBox[]> RESOLVED_OUTLINE_CACHE = new HashMap<>();
    private static final Map<String, AxisAlignedBB[]> RESOLVED_BOX_CACHE = new HashMap<>();

    private static final Map<String, ResolvedModel> STATE_MODEL_CACHE = new HashMap<>();
    private static final Set<String> MISSING_STATE_MODEL_CACHE = new HashSet<>();
    private static final Map<ResourceLocation, JsonObject> BLOCKSTATE_JSON_CACHE = new HashMap<>();
    private static final Set<ResourceLocation> MISSING_BLOCKSTATE_JSON = new HashSet<>();

    private JsonModelSelectionBoxCache() {
    }

    public static AxisAlignedBB[] getBoxes(IBlockState state) {
        return getBoxes(state, null, null);
    }

    public static AxisAlignedBB[] getBoxes(IBlockState state, IBlockAccess world, BlockPos pos) {
        IBlockState resolvedState = resolveActualState(state, world, pos);
        if (!canParseModId(resolvedState)) {
            return EMPTY;
        }

        ResolvedModel resolvedModel = resolveModel(resolvedState);
        if (resolvedModel == null) {
            return EMPTY;
        }

        String cacheKey = resolvedModel.cacheKey();
        AxisAlignedBB[] cached = RESOLVED_BOX_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        OutlineBox[] outlines = getWireframesInternal(resolvedModel);
        if (outlines.length == 0) {
            RESOLVED_BOX_CACHE.put(cacheKey, EMPTY);
            return EMPTY;
        }

        AxisAlignedBB[] boxes = new AxisAlignedBB[outlines.length];
        for (int i = 0; i < outlines.length; i++) {
            boxes[i] = outlines[i].getBounds();
        }
        RESOLVED_BOX_CACHE.put(cacheKey, boxes);
        return boxes;
    }

    public static OutlineBox[] getWireframes(IBlockState state) {
        return getWireframes(state, null, null);
    }

    public static OutlineBox[] getWireframes(IBlockState state, IBlockAccess world, BlockPos pos) {
        IBlockState resolvedState = resolveActualState(state, world, pos);
        if (!canParseModId(resolvedState)) {
            return EMPTY_OUTLINES;
        }

        ResolvedModel resolvedModel = resolveModel(resolvedState);
        if (resolvedModel == null) {
            return EMPTY_OUTLINES;
        }

        return getWireframesInternal(resolvedModel);
    }

    public static OutlineBox[] toOutlineBoxes(AxisAlignedBB[] boxes) {
        if (boxes == null || boxes.length == 0) {
            return EMPTY_OUTLINES;
        }
        List<OutlineBox> outlines = new ArrayList<>(boxes.length);
        for (AxisAlignedBB box : boxes) {
            if (box == null) {
                continue;
            }
            double minX = Math.min(box.minX, box.maxX);
            double minY = Math.min(box.minY, box.maxY);
            double minZ = Math.min(box.minZ, box.maxZ);
            double maxX = Math.max(box.minX, box.maxX);
            double maxY = Math.max(box.minY, box.maxY);
            double maxZ = Math.max(box.minZ, box.maxZ);
            if ((maxX - minX) < MIN_ELEMENT_THICKNESS
                    || (maxY - minY) < MIN_ELEMENT_THICKNESS
                    || (maxZ - minZ) < MIN_ELEMENT_THICKNESS) {
                continue;
            }
            outlines.add(new OutlineBox(createCorners(minX, minY, minZ, maxX, maxY, maxZ)));
        }
        return outlines.isEmpty() ? EMPTY_OUTLINES : outlines.toArray(new OutlineBox[0]);
    }

    public static OutlineBox[] toOutlineBoxesFromCorners(List<Vec3d[]> cornerSets) {
        if (cornerSets == null || cornerSets.isEmpty()) {
            return EMPTY_OUTLINES;
        }
        List<OutlineBox> outlines = new ArrayList<>(cornerSets.size());
        for (Vec3d[] rawCorners : cornerSets) {
            if (rawCorners == null || rawCorners.length != 8) {
                continue;
            }
            Vec3d[] corners = new Vec3d[8];
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            boolean valid = true;
            for (int i = 0; i < rawCorners.length; i++) {
                Vec3d corner = rawCorners[i];
                if (corner == null) {
                    valid = false;
                    break;
                }
                corners[i] = new Vec3d(corner.x, corner.y, corner.z);
                minX = Math.min(minX, corner.x);
                minY = Math.min(minY, corner.y);
                minZ = Math.min(minZ, corner.z);
                maxX = Math.max(maxX, corner.x);
                maxY = Math.max(maxY, corner.y);
                maxZ = Math.max(maxZ, corner.z);
            }
            if (!valid) {
                continue;
            }
            if ((maxX - minX) < MIN_ELEMENT_THICKNESS
                    || (maxY - minY) < MIN_ELEMENT_THICKNESS
                    || (maxZ - minZ) < MIN_ELEMENT_THICKNESS) {
                continue;
            }
            outlines.add(new OutlineBox(corners));
        }
        return outlines.isEmpty() ? EMPTY_OUTLINES : outlines.toArray(new OutlineBox[0]);
    }

    public static OutlineBox[] rotateOutlines(OutlineBox[] outlines, int xRotation, int yRotation) {
        if (outlines == null || outlines.length == 0) {
            return EMPTY_OUTLINES;
        }
        return applyStateRotation(outlines, xRotation, yRotation);
    }

    private static OutlineBox[] getWireframesInternal(ResolvedModel resolvedModel) {
        String cacheKey = resolvedModel.cacheKey();
        OutlineBox[] cached = RESOLVED_OUTLINE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        OutlineBox[] modelOutlines = loadModelOutlines(resolvedModel.modelLocation);
        if (modelOutlines.length == 0) {
            RESOLVED_OUTLINE_CACHE.put(cacheKey, EMPTY_OUTLINES);
            return EMPTY_OUTLINES;
        }

        OutlineBox[] rotatedOutlines = applyStateRotation(modelOutlines, resolvedModel.xRotation, resolvedModel.yRotation);
        RESOLVED_OUTLINE_CACHE.put(cacheKey, rotatedOutlines);
        return rotatedOutlines;
    }

    private static IBlockState resolveActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (world == null || pos == null) {
            return state;
        }
        try {
            return state.getActualState(world, pos);
        } catch (Exception ignored) {
            return state;
        }
    }

    private static boolean canParseModId(IBlockState state) {
        ResourceLocation registryName = state.getBlock().getRegistryName();
        if (registryName == null) {
            return false;
        }
        String namespace = registryName.getNamespace().toLowerCase(Locale.ROOT);
        String[] whitelist = MekanismConfig.current().client.jsonSelectionBoxModIdWhitelist.get();
        if (whitelist == null || whitelist.length == 0) {
            return false;
        }
        for (String entry : whitelist) {
            if (entry == null) {
                continue;
            }
            String value = entry.trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty()) {
                continue;
            }
            if ("*".equals(value) || namespace.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static ResolvedModel resolveModel(IBlockState state) {
        ResourceLocation blockRegistryName = state.getBlock().getRegistryName();
        if (blockRegistryName == null) {
            return null;
        }

        Map<String, String> stateValues = getStateValues(state);
        String stateCacheKey = buildStateCacheKey(blockRegistryName, stateValues);

        ResolvedModel cached = STATE_MODEL_CACHE.get(stateCacheKey);
        if (cached != null) {
            return cached;
        }
        if (MISSING_STATE_MODEL_CACHE.contains(stateCacheKey)) {
            return null;
        }

        ResolvedModel resolved = resolveModelFromBlockstates(blockRegistryName, stateValues);
        if (resolved == null) {
            resolved = resolveModelFromStateMapper(state);
        }

        if (resolved == null || isSkippedModel(resolved.modelLocation)) {
            MISSING_STATE_MODEL_CACHE.add(stateCacheKey);
            return null;
        }

        STATE_MODEL_CACHE.put(stateCacheKey, resolved);
        return resolved;
    }

    private static ResolvedModel resolveModelFromStateMapper(IBlockState state) {
        BlockStateMapper stateMapper = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getBlockStateMapper();
        try {
            Map<IBlockState, ModelResourceLocation> variants = stateMapper.getVariants(state.getBlock());
            ModelResourceLocation modelLocation = variants.get(state);
            if (modelLocation == null && !variants.isEmpty()) {
                modelLocation = variants.values().iterator().next();
            }
            if (modelLocation != null) {
                Map<String, String> variantValues = parseVariantString(modelLocation.getVariant());
                ResolvedModel resolvedFromModelBlockstates = resolveModelFromBlockstates(
                        new ResourceLocation(modelLocation.getNamespace(), modelLocation.getPath()),
                        variantValues
                );
                if (resolvedFromModelBlockstates != null) {
                    return resolvedFromModelBlockstates;
                }
                ResourceLocation modelResource = new ResourceLocation(modelLocation.getNamespace(), modelLocation.getPath());
                return new ResolvedModel(modelResource, 0, 0);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ResolvedModel resolveModelFromBlockstates(ResourceLocation blockRegistryName, Map<String, String> stateValues) {
        JsonObject blockstatesJson = loadBlockstateJson(blockRegistryName);
        if (blockstatesJson == null || !blockstatesJson.has("variants") || !blockstatesJson.get("variants").isJsonObject()) {
            return null;
        }

        JsonObject variants = blockstatesJson.getAsJsonObject("variants");
        VariantResolution resolution = new VariantResolution(blockRegistryName.getNamespace());

        if (blockstatesJson.has("defaults") && blockstatesJson.get("defaults").isJsonObject()) {
            resolution.merge(blockstatesJson.getAsJsonObject("defaults"));
        }

        JsonElement normalVariant = variants.get("normal");
        if (normalVariant != null) {
            resolution.merge(normalVariant);
        }

        String stateVariantKey = buildStateVariantKey(stateValues);
        boolean mergedExactVariant = false;
        if (!stateVariantKey.isEmpty()) {
            JsonElement exactVariant = variants.get(stateVariantKey);
            if (exactVariant != null) {
                resolution.merge(exactVariant);
                mergedExactVariant = true;
            }
        }

        if (!mergedExactVariant) {
            JsonElement bestMatchVariant = null;
            int bestScore = -1;
            for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
                String variantKey = entry.getKey();
                if ("inventory".equals(variantKey) || "normal".equals(variantKey) || !variantKey.contains("=")) {
                    continue;
                }
                int matchScore = matchVariantKey(variantKey, stateValues);
                if (matchScore > bestScore) {
                    bestScore = matchScore;
                    bestMatchVariant = entry.getValue();
                }
            }
            if (bestMatchVariant != null) {
                resolution.merge(bestMatchVariant);
            }
        }

        for (Map.Entry<String, String> stateEntry : stateValues.entrySet()) {
            JsonElement propertyVariantsElement = variants.get(stateEntry.getKey());
            if (propertyVariantsElement == null || !propertyVariantsElement.isJsonObject()) {
                continue;
            }
            JsonObject propertyVariants = propertyVariantsElement.getAsJsonObject();
            JsonElement matchedPropertyVariant = propertyVariants.get(stateEntry.getValue());
            if (matchedPropertyVariant != null) {
                resolution.merge(matchedPropertyVariant);
            }
        }

        return resolution.build();
    }

    private static JsonObject loadBlockstateJson(ResourceLocation blockRegistryName) {
        JsonObject cached = BLOCKSTATE_JSON_CACHE.get(blockRegistryName);
        if (cached != null) {
            return cached;
        }
        if (MISSING_BLOCKSTATE_JSON.contains(blockRegistryName)) {
            return null;
        }

        ResourceLocation file = new ResourceLocation(blockRegistryName.getNamespace(), "blockstates/" + blockRegistryName.getPath() + ".json");
        try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(file);
             InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject json = JSON_PARSER.parse(reader).getAsJsonObject();
            BLOCKSTATE_JSON_CACHE.put(blockRegistryName, json);
            return json;
        } catch (IOException | IllegalStateException ignored) {
            MISSING_BLOCKSTATE_JSON.add(blockRegistryName);
            return null;
        }
    }

    private static String buildStateCacheKey(ResourceLocation blockRegistryName, Map<String, String> stateValues) {
        String variant = buildStateVariantKey(stateValues);
        return blockRegistryName + "|" + variant;
    }

    private static String buildStateVariantKey(Map<String, String> stateValues) {
        if (stateValues.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : stateValues.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        return builder.toString();
    }

    private static Map<String, String> parseVariantString(String variant) {
        Map<String, String> values = new TreeMap<>();
        if (variant == null || variant.isEmpty() || "normal".equals(variant) || "inventory".equals(variant)) {
            return values;
        }
        String[] parts = variant.split(",");
        for (String rawPart : parts) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }
            int separator = part.indexOf('=');
            if (separator <= 0 || separator >= part.length() - 1) {
                continue;
            }
            String key = part.substring(0, separator).trim();
            String value = part.substring(separator + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static int matchVariantKey(String variantKey, Map<String, String> stateValues) {
        if (variantKey == null || variantKey.isEmpty()) {
            return stateValues.isEmpty() ? 0 : -1;
        }

        int matchedEntries = 0;
        String[] parts = variantKey.split(",");
        for (String rawPart : parts) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }
            int separator = part.indexOf('=');
            if (separator <= 0 || separator >= part.length() - 1) {
                return -1;
            }
            String key = part.substring(0, separator).trim();
            String value = part.substring(separator + 1).trim();
            String stateValue = stateValues.get(key);
            if (stateValue == null || !stateValue.equals(value)) {
                return -1;
            }
            matchedEntries++;
        }
        return matchedEntries;
    }

    private static Map<String, String> getStateValues(IBlockState state) {
        Map<String, String> values = new TreeMap<>();
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
            IProperty<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();
            values.put(property.getName(), getPropertyValueName(property, value));
        }
        return values;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String getPropertyValueName(IProperty<?> property, Comparable<?> value) {
        return ((IProperty) property).getName(value);
    }

    private static OutlineBox[] loadModelOutlines(ResourceLocation modelLocation) {
        OutlineBox[] cached = MODEL_OUTLINE_CACHE.get(modelLocation);
        if (cached != null) {
            return cached;
        }

        List<OutlineBox> outlines = new ArrayList<>();
        collectOutlines(modelLocation, outlines, new HashSet<>());
        OutlineBox[] loaded = outlines.isEmpty() ? EMPTY_OUTLINES : outlines.toArray(new OutlineBox[0]);
        MODEL_OUTLINE_CACHE.put(modelLocation, loaded);
        return loaded;
    }

    private static void collectOutlines(ResourceLocation modelLocation, List<OutlineBox> outlines, Set<ResourceLocation> visited) {
        if (!visited.add(modelLocation)) {
            return;
        }
        JsonObject modelJson = loadModelJson(modelLocation);
        if (modelJson == null) {
            return;
        }

        if (modelJson.has("elements") && modelJson.get("elements").isJsonArray()) {
            JsonArray elements = modelJson.getAsJsonArray("elements");
            for (JsonElement element : elements) {
                if (!element.isJsonObject()) {
                    continue;
                }
                OutlineBox outline = parseElementOutline(element.getAsJsonObject());
                if (outline != null) {
                    outlines.add(outline);
                }
            }
            return;
        }

        if (modelJson.has("parent")) {
            ResourceLocation parentModel = parseModelReference(modelJson.get("parent").getAsString(), modelLocation.getNamespace());
            if (parentModel != null) {
                collectOutlines(parentModel, outlines, visited);
            }
        }
    }

    private static JsonObject loadModelJson(ResourceLocation modelLocation) {
        JsonObject json = loadModelJsonAtPath(modelLocation);
        if (json != null) {
            return json;
        }
        String path = modelLocation.getPath();
        if (!path.startsWith("block/") && !path.startsWith("item/")) {
            return loadModelJsonAtPath(new ResourceLocation(modelLocation.getNamespace(), "block/" + path));
        }
        return null;
    }

    private static JsonObject loadModelJsonAtPath(ResourceLocation modelLocation) {
        ResourceLocation file = new ResourceLocation(modelLocation.getNamespace(), "models/" + modelLocation.getPath() + ".json");
        try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(file);
             InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return JSON_PARSER.parse(reader).getAsJsonObject();
        } catch (IOException | IllegalStateException ignored) {
            return null;
        }
    }

    private static ResourceLocation parseModelReference(String reference, String currentNamespace) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        if (reference.contains(":")) {
            return new ResourceLocation(reference);
        }
        return new ResourceLocation("minecraft", reference);
    }

    private static ResourceLocation parseBlockstateModelReference(String reference, String currentNamespace) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        ResourceLocation location = reference.contains(":") ? new ResourceLocation(reference) : new ResourceLocation(currentNamespace, reference);
        String path = location.getPath();
        if (!path.contains("/") && !path.startsWith("builtin/")) {
            return new ResourceLocation(location.getNamespace(), "block/" + path);
        }
        return location;
    }

    private static boolean isSkippedModel(ResourceLocation modelLocation) {
        if (modelLocation == null) {
            return false;
        }
        Set<String> aliases = getModelAliases(modelLocation);
        return matchesSkipList(aliases);
    }

    private static Set<String> getModelAliases(ResourceLocation modelLocation) {
        Set<String> aliases = new HashSet<>();
        String namespace = modelLocation.getNamespace().toLowerCase(Locale.ROOT);
        String path = modelLocation.getPath().toLowerCase(Locale.ROOT);

        aliases.add(path);
        aliases.add(namespace + ":" + path);

        if (path.startsWith("block/")) {
            String stripped = path.substring("block/".length());
            aliases.add(stripped);
            aliases.add(namespace + ":" + stripped);
        } else if (path.startsWith("item/")) {
            String stripped = path.substring("item/".length());
            aliases.add(stripped);
            aliases.add(namespace + ":" + stripped);
        } else {
            aliases.add("block/" + path);
            aliases.add(namespace + ":block/" + path);
            aliases.add("item/" + path);
            aliases.add(namespace + ":item/" + path);
        }
        return aliases;
    }

    private static boolean matchesSkipList(Set<String> aliases) {
        String[] skipList = MekanismConfig.current().client.jsonSelectionBoxModelSkipList.get();
        if (skipList == null || skipList.length == 0) {
            return false;
        }
        for (String rawEntry : skipList) {
            if (rawEntry == null) {
                continue;
            }
            String entry = rawEntry.trim().toLowerCase(Locale.ROOT);
            if (entry.isEmpty()) {
                continue;
            }
            if ("*".equals(entry)) {
                return true;
            }
            if (!entry.contains("*")) {
                if (aliases.contains(entry)) {
                    return true;
                }
                continue;
            }
            String regex = entry.replace(".", "\\.").replace("*", ".*");
            for (String alias : aliases) {
                if (alias.matches(regex)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static OutlineBox parseElementOutline(JsonObject element) {
        if (!element.has("from") || !element.has("to")) {
            return null;
        }

        double[] from = parseVector(element.getAsJsonArray("from"));
        double[] to = parseVector(element.getAsJsonArray("to"));
        if (from == null || to == null) {
            return null;
        }

        double minX = Math.min(from[0], to[0]) / 16D;
        double minY = Math.min(from[1], to[1]) / 16D;
        double minZ = Math.min(from[2], to[2]) / 16D;
        double maxX = Math.max(from[0], to[0]) / 16D;
        double maxY = Math.max(from[1], to[1]) / 16D;
        double maxZ = Math.max(from[2], to[2]) / 16D;

        // Ignore zero-thickness overlay planes (commonly used for LEDs/decals) as they create seam noise.
        if ((maxX - minX) < MIN_ELEMENT_THICKNESS
                || (maxY - minY) < MIN_ELEMENT_THICKNESS
                || (maxZ - minZ) < MIN_ELEMENT_THICKNESS) {
            return null;
        }

        Vec3d[] corners = createCorners(minX, minY, minZ, maxX, maxY, maxZ);
        if (element.has("rotation") && element.get("rotation").isJsonObject()) {
            JsonObject rotation = element.getAsJsonObject("rotation");
            if (rotation.has("axis") && rotation.has("angle") && rotation.has("origin")) {
                String axis = rotation.get("axis").getAsString();
                double angle = rotation.get("angle").getAsDouble();
                double[] origin = parseVector(rotation.getAsJsonArray("origin"));
                if (origin != null && Math.abs(angle) >= 1.0E-7D) {
                    corners = rotateCorners(corners, axis, angle, origin[0] / 16D, origin[1] / 16D, origin[2] / 16D);
                }
            }
        }

        return new OutlineBox(corners);
    }

    private static OutlineBox[] applyStateRotation(OutlineBox[] boxes, int xRotation, int yRotation) {
        int normalizedX = normalizeRotation(xRotation);
        int normalizedY = normalizeRotation(yRotation);
        if (normalizedX == 0 && normalizedY == 0) {
            return boxes;
        }

        OutlineBox[] rotated = new OutlineBox[boxes.length];
        for (int i = 0; i < boxes.length; i++) {
            Vec3d[] corners = boxes[i].getCorners();
            if (normalizedX != 0) {
                corners = rotateCorners(corners, "x", -normalizedX, 0.5D, 0.5D, 0.5D);
            }
            if (normalizedY != 0) {
                corners = rotateCorners(corners, "y", -normalizedY, 0.5D, 0.5D, 0.5D);
            }
            rotated[i] = new OutlineBox(corners);
        }
        return rotated;
    }

    private static int normalizeRotation(int rotation) {
        int normalized = rotation % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        return normalized;
    }

    private static Vec3d[] createCorners(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new Vec3d[]{
                new Vec3d(minX, minY, minZ),
                new Vec3d(minX, minY, maxZ),
                new Vec3d(minX, maxY, minZ),
                new Vec3d(minX, maxY, maxZ),
                new Vec3d(maxX, minY, minZ),
                new Vec3d(maxX, minY, maxZ),
                new Vec3d(maxX, maxY, minZ),
                new Vec3d(maxX, maxY, maxZ)
        };
    }

    private static Vec3d[] rotateCorners(Vec3d[] corners, String axisName, double angle, double ox, double oy, double oz) {
        if (Math.abs(angle) < 1.0E-7D) {
            return corners;
        }

        String axis = axisName == null ? "" : axisName.toLowerCase(Locale.ROOT);
        if (!"x".equals(axis) && !"y".equals(axis) && !"z".equals(axis)) {
            return corners;
        }

        double radians = Math.toRadians(angle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        Vec3d[] rotated = new Vec3d[corners.length];
        for (int i = 0; i < corners.length; i++) {
            Vec3d corner = corners[i];
            double x = corner.x - ox;
            double y = corner.y - oy;
            double z = corner.z - oz;

            double rx = x;
            double ry = y;
            double rz = z;

            switch (axis) {
                case "x" -> {
                    ry = y * cos - z * sin;
                    rz = y * sin + z * cos;
                }
                case "y" -> {
                    rx = x * cos + z * sin;
                    rz = -x * sin + z * cos;
                }
                case "z" -> {
                    rx = x * cos - y * sin;
                    ry = x * sin + y * cos;
                }
            }

            rotated[i] = new Vec3d(rx + ox, ry + oy, rz + oz);
        }
        return rotated;
    }

    private static double[] parseVector(JsonArray vector) {
        if (vector == null || vector.size() != 3) {
            return null;
        }
        return new double[]{vector.get(0).getAsDouble(), vector.get(1).getAsDouble(), vector.get(2).getAsDouble()};
    }

    private static JsonObject getVariantObject(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            JsonObject first = null;
            for (JsonElement arrayElement : array) {
                if (!arrayElement.isJsonObject()) {
                    continue;
                }
                JsonObject variant = arrayElement.getAsJsonObject();
                if (first == null) {
                    first = variant;
                }
                if (variant.has("model")) {
                    return variant;
                }
            }
            return first;
        }
        return null;
    }

    private static final class VariantResolution {

        private final String namespace;
        private ResourceLocation modelLocation;
        private int xRotation;
        private int yRotation;

        private VariantResolution(String namespace) {
            this.namespace = namespace;
        }

        private void merge(JsonElement element) {
            JsonObject variant = getVariantObject(element);
            if (variant == null) {
                return;
            }

            if (variant.has("model")) {
                ResourceLocation parsed = parseBlockstateModelReference(variant.get("model").getAsString(), namespace);
                if (parsed != null) {
                    modelLocation = parsed;
                }
            }
            if (variant.has("x")) {
                xRotation = normalizeRotation(xRotation + variant.get("x").getAsInt());
            }
            if (variant.has("y")) {
                yRotation = normalizeRotation(yRotation + variant.get("y").getAsInt());
            }
        }

        private ResolvedModel build() {
            if (modelLocation == null) {
                return null;
            }
            return new ResolvedModel(modelLocation, xRotation, yRotation);
        }
    }

    private static final class ResolvedModel {

        private final ResourceLocation modelLocation;
        private final int xRotation;
        private final int yRotation;

        private ResolvedModel(ResourceLocation modelLocation, int xRotation, int yRotation) {
            this.modelLocation = modelLocation;
            this.xRotation = normalizeRotation(xRotation);
            this.yRotation = normalizeRotation(yRotation);
        }

        private String cacheKey() {
            return modelLocation + "|x=" + xRotation + "|y=" + yRotation;
        }
    }

    public static final class OutlineBox {

        private final Vec3d[] corners;
        private final Vec3d[] lineVertices;
        private final AxisAlignedBB bounds;

        private OutlineBox(Vec3d[] corners) {
            this.corners = corners;
            this.lineVertices = computeLineVertices(corners);
            this.bounds = computeBounds(corners);
        }

        public Vec3d[] getCorners() {
            return corners;
        }

        public Vec3d[] getLineVertices() {
            return lineVertices;
        }

        public AxisAlignedBB getBounds() {
            return bounds;
        }

        private static Vec3d[] computeLineVertices(Vec3d[] corners) {
            if (corners == null || corners.length != 8) {
                return new Vec3d[0];
            }
            Vec3d[] vertices = new Vec3d[EDGE_INDICES.length * 2];
            int index = 0;
            for (int[] edge : EDGE_INDICES) {
                vertices[index++] = corners[edge[0]];
                vertices[index++] = corners[edge[1]];
            }
            return vertices;
        }

        private static AxisAlignedBB computeBounds(Vec3d[] corners) {
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;

            for (Vec3d corner : corners) {
                minX = Math.min(minX, corner.x);
                minY = Math.min(minY, corner.y);
                minZ = Math.min(minZ, corner.z);
                maxX = Math.max(maxX, corner.x);
                maxY = Math.max(maxY, corner.y);
                maxZ = Math.max(maxZ, corner.z);
            }
            return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
