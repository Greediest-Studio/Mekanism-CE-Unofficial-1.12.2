package mekanism.common.base;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.common.InfuseStorage;
import mekanism.common.Mekanism;
import mekanism.common.MekanismFluids;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.*;
import mekanism.common.recipe.machines.*;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.tile.prefab.TileEntityFarmMachine;
import mekanism.common.util.LangUtils;
import mekanism.common.util.StackUtils;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;

/**
 * Internal interface for managing various Factory types.
 *
 * @author AidanBrady
 */
public interface IFactory {

    /**
     * Gets the recipe type this Smelting Factory currently has.
     *
     * @param itemStack - stack to check
     * @return RecipeType ordinal
     */
    int getRecipeType(ItemStack itemStack);

    /**
     * Gets the recipe type this Factory currently has.
     *
     * @param itemStack - stack to check
     * @return RecipeType or null if it has invalid NBT
     */
    @Nullable
    RecipeType getRecipeTypeOrNull(ItemStack itemStack);

    /**
     * Sets the recipe type of this Smelting Factory to a new value.
     *
     * @param type      - RecipeType ordinal
     * @param itemStack - stack to set
     */
    void setRecipeType(int type, ItemStack itemStack);

    enum MachineFuelType {
        BASIC,
        ADVANCED,
        DOUBLE,
        CHANCE,
        FARM,
        CHANCE2
    }


    enum RecipeType implements IStringSerializable {
        SMELTING("Smelting", "smelter", MachineType.ENERGIZED_SMELTER, MachineFuelType.BASIC, false, Recipe.ENERGIZED_SMELTER),
        ENRICHING("Enriching", "enrichment", MachineType.ENRICHMENT_CHAMBER, MachineFuelType.BASIC, false, Recipe.ENRICHMENT_CHAMBER),
        CRUSHING("Crushing", "crusher", MachineType.CRUSHER, MachineFuelType.BASIC, false, Recipe.CRUSHER),
        COMPRESSING("Compressing", "compressor", MachineType.OSMIUM_COMPRESSOR, MachineFuelType.ADVANCED, false, Recipe.OSMIUM_COMPRESSOR, true, false, false),
        COMBINING("Combining", "combiner", MachineType.COMBINER, MachineFuelType.DOUBLE, false, Recipe.COMBINER),
        PURIFYING("Purifying", "purifier", MachineType.PURIFICATION_CHAMBER, MachineFuelType.ADVANCED, true, Recipe.PURIFICATION_CHAMBER, true, false, false),
        INJECTING("Injecting", "injection", MachineType.CHEMICAL_INJECTION_CHAMBER, MachineFuelType.ADVANCED, true, Recipe.CHEMICAL_INJECTION_CHAMBER, true, false, false),
        INFUSING("Infusing", "metalinfuser", MachineType.METALLURGIC_INFUSER, MachineFuelType.BASIC, false, Recipe.METALLURGIC_INFUSER),
        SAWING("Sawing", "sawmill", MachineType.PRECISION_SAWMILL, MachineFuelType.CHANCE, false, Recipe.PRECISION_SAWMILL),
        STAMPING("Stamping", "stamping", MachineType.STAMPING, MachineFuelType.BASIC, false, Recipe.STAMPING),
        ROLLING("Rolling", "rolling", MachineType.ROLLING, MachineFuelType.BASIC, false, Recipe.ROLLING),
        BRUSHED("Brushed", "brushed", MachineType.BRUSHED, MachineFuelType.BASIC, false, Recipe.BRUSHED),
        TURNING("Turning", "turning", MachineType.TURNING, MachineFuelType.BASIC, false, Recipe.TURNING),
        AllOY("Alloy", "alloy", MachineType.ALLOY, MachineFuelType.DOUBLE, false, Recipe.ALLOY),
        EXTRACTOR("Extractor", "extractor", MachineType.CELL_EXTRACTOR, MachineFuelType.CHANCE, false, Recipe.CELL_EXTRACTOR),
        SEPARATOR("Separator", "separator", MachineType.CELL_SEPARATOR, MachineFuelType.CHANCE, false, Recipe.CELL_SEPARATOR),
        FARM("Farm", "farm", MachineType.ORGANIC_FARM, MachineFuelType.FARM, true, Recipe.ORGANIC_FARM, true, false, false),
        RECYCLER("Recycler", "Recycler", MachineType.RECYCLER, MachineFuelType.CHANCE2, false, Recipe.RECYCLER),
        Crystallizer("Crystallizer", "crystallizer", MachineType.CHEMICAL_CRYSTALLIZER, MachineFuelType.BASIC, false, Recipe.CHEMICAL_CRYSTALLIZER, false, false, false, true, true, false, false),
        //TODO:移除这个
        Dissolution("Dissolution", "dissolution", MachineType.CHEMICAL_DISSOLUTION_CHAMBER, MachineFuelType.BASIC, true, Recipe.CHEMICAL_DISSOLUTION_CHAMBER, true, true, true, false, true, true, false),
        PRC("PRC", "prc", MachineType.PRESSURIZED_REACTION_CHAMBER, MachineFuelType.BASIC, false, Recipe.PRESSURIZED_REACTION_CHAMBER, false, false, true, true, true, true, true),
        //TODO：移除这个
        OXIDIZER("Oxidizer", "oxidizer", MachineType.CHEMICAL_OXIDIZER, MachineFuelType.BASIC, false, Recipe.CHEMICAL_OXIDIZER, false, false, true, false, false, true, false),
        NUCLEOSYNTHESIZER("Nucleosynthesizer", "nucleosynthesizer", MachineType.ANTIPROTONIC_NUCLEOSYNTHESIZER, MachineFuelType.BASIC, false, Recipe.ANTIPROTONIC_NUCLEOSYNTHESIZER, false, false, true, true, true, false, false),
        //TODO：移除这个
        WASHER("Washer", "washer", MachineType.CHEMICAL_WASHER, MachineFuelType.BASIC, false, Recipe.CHEMICAL_WASHER, false, false, false, false, true, true, true);

        private String name;
        private SoundEvent sound;
        private MachineType type;
        private MachineFuelType fuelType;
        private boolean fuelSpeed;
        private Recipe recipe;
        private TileEntityAdvancedElectricMachine AdvancedElectricMachineCacheTile;

        private TileEntityFarmMachine FarmMachineCacheTile;
        private transient Map<Item, List<Object>> cachedInputMatchers;
        private transient Map<Item, List<AdvancedMachineInput>> cachedAdvancedInputMatchers;
        private transient Map<Item, List<DoubleMachineInput>> cachedDoubleExtraMatchers;
        private transient int cachedInputMatchersRecipeSize = -1;
        public boolean isFullBlock;
        public boolean isOpaqueCube;
        public boolean canInputItem;
        public boolean canOutputItem;
        public boolean canInputGas;
        public boolean canOuputGas;
        public boolean canInputFliud;


        RecipeType(String s, String s1, MachineType t, MachineFuelType ft, boolean speed, Recipe r) {
            this(s, s1, t, ft, speed, r, true, true, true, true, false, false, false);
        }

        RecipeType(String s, String s1, MachineType t, MachineFuelType ft, boolean speed, Recipe r, boolean inputGas, boolean outputGas, boolean inputFliud) {
            this(s, s1, t, ft, speed, r, true, true, true, true, inputGas, outputGas, inputFliud);
        }

        RecipeType(String s, String s1, MachineType t, MachineFuelType ft, boolean speed, Recipe r, boolean fullBlock, boolean opaque, boolean inputItem, boolean outputItem, boolean inputGas, boolean outputGas, boolean inputFliud) {
            name = s;
            sound = new SoundEvent(new ResourceLocation(Mekanism.MODID, "tile.machine." + s1));
            type = t;
            fuelType = ft;
            fuelSpeed = speed;
            recipe = r;
            isFullBlock = fullBlock;
            isOpaqueCube = opaque;
            canInputItem = inputItem;
            canOutputItem = outputItem;
            canInputGas = inputGas;
            canOuputGas = outputGas;
            canInputFliud = inputFliud;
        }

        public static RecipeType getFromMachine(Block block, int meta) {
            //Mekanism.logger.debug("Param block: " + block + " || meta:" + meta);
            RecipeType type = null;
            for (RecipeType iterType : RecipeType.values()) {
                ItemStack machineStack = iterType.getStack();
                Block machineBlock = Block.getBlockFromItem(machineStack.getItem());
                int machineMeta = machineStack.getItemDamage();
                // Mekanism.logger.debug("Search block: " + machineBlock + " || meta:" + machineMeta);
                if (machineBlock == block && machineMeta == meta) {
                    type = iterType;
                    break;
                }
            }
            return type;
        }

        @Nullable
        public static RecipeType getFromMachineType(MachineType machineType) {
            for (RecipeType type : values()) {
                if (type.type == machineType) {
                    return type;
                }
            }
            return null;
        }

        public BasicMachineRecipe<?> getRecipe(ItemStackInput input) {
            return (BasicMachineRecipe<?>) RecipeHandler.getRecipe(input, recipe);
        }

        public BasicMachineRecipe<?> getRecipe(ItemStack input) {
            return getRecipe(new ItemStackInput(input));
        }

        public AdvancedMachineRecipe<?> getRecipe(AdvancedMachineInput input) {
            return (AdvancedMachineRecipe<?>) RecipeHandler.getRecipe(input, recipe);
        }

        public AdvancedMachineRecipe<?> getRecipe(ItemStack input, Gas gas) {
            return getRecipe(new AdvancedMachineInput(input, gas));
        }

        public DoubleMachineRecipe<?> getRecipe(DoubleMachineInput input) {
            return (DoubleMachineRecipe<?>) RecipeHandler.getRecipe(input, recipe);
        }

        public DoubleMachineRecipe<?> getRecipe(ItemStack input, ItemStack extra) {
            return getRecipe(new DoubleMachineInput(input, extra));
        }

        public ChanceMachineRecipe<?> getChanceRecipe(ItemStackInput input) {
            return (ChanceMachineRecipe<?>) RecipeHandler.getRecipe(input, recipe);
        }

        public ChanceMachineRecipe<?> getChanceRecipe(ItemStack input) {
            return getChanceRecipe(new ItemStackInput(input));
        }


        public MetallurgicInfuserRecipe getRecipe(InfusionInput input) {
            return RecipeHandler.getMetallurgicInfuserRecipe(input);
        }

        public MetallurgicInfuserRecipe getRecipe(ItemStack input, InfuseStorage storage) {
            return getRecipe(new InfusionInput(storage, input));
        }

        public CrystallizerRecipe getRecipe(GasInput input) {
            return RecipeHandler.getChemicalCrystallizerRecipe(input);
        }

        public CrystallizerRecipe getCrystallizerRecipe(GasStack gas) {
            return getRecipe(new GasInput(gas));
        }

        public DissolutionRecipe getDissolutionRecipe(ItemStackInput input) {
            return (DissolutionRecipe) RecipeHandler.getRecipe(input, recipe);
        }

        public DissolutionRecipe getDissolutionRecipe(ItemStack input) {
            return getDissolutionRecipe(new ItemStackInput(input));
        }

        public OxidationRecipe getOxidationRecipe(ItemStackInput input) {
            return (OxidationRecipe) RecipeHandler.getRecipe(input, recipe);
        }

        public OxidationRecipe getOxidationRecipe(ItemStack input) {
            return getOxidationRecipe(new ItemStackInput(input));
        }

        public NucleosynthesizerRecipe getNucleosynthesizerRecipe(NucleosynthesizerInput input) {
            return (NucleosynthesizerRecipe) RecipeHandler.getRecipe(input, recipe);
        }

        public NucleosynthesizerRecipe getNucleosynthesizerRecipe(ItemStack input, GasStack gas) {
            return getNucleosynthesizerRecipe(new NucleosynthesizerInput(input, gas));
        }

        public PressurizedRecipe getPressurizedRecipe(PressurizedInput input) {
            return (PressurizedRecipe) RecipeHandler.getRecipe(input, recipe);
        }

        public PressurizedRecipe getPressurizedRecipe(ItemStack input, FluidStack fluid, GasStack gas) {
            return getPressurizedRecipe(new PressurizedInput(input, fluid, gas));
        }

        public WasherRecipe getWasherRecipe(GasAndFluidInput input) {
            return RecipeHandler.getChemicalWasherRecipe(input);
        }

        public WasherRecipe getWasherRecipe(GasStack gas, FluidStack stack) {
            return getWasherRecipe(new GasAndFluidInput(gas,stack));
        }

        public FarmMachineRecipe<?> getFarmRecipe(AdvancedMachineInput input) {
            return (FarmMachineRecipe<?>) RecipeHandler.getRecipe(input, recipe);
        }

        public FarmMachineRecipe<?> getFarmRecipe(ItemStack input, Gas gas) {
            return getFarmRecipe(new AdvancedMachineInput(input, gas));
        }

        public Chance2MachineRecipe<?> getChance2Recipe(ItemStackInput input) {
            return (Chance2MachineRecipe<?>) RecipeHandler.getRecipe(input, recipe);
        }

        public Chance2MachineRecipe<?> getChance2Recipe(ItemStack input) {
            return getChance2Recipe(new ItemStackInput(input));
        }


        @Nullable
        public MachineRecipe getAnyRecipe(ItemStack slotStack, ItemStack extraStack, Gas gasType, InfuseStorage infuse, GasStack gasStackType, FluidStack fluidStack) {
            if (fuelType == MachineFuelType.ADVANCED) {
                return getRecipe(slotStack, gasType);
            } else if (fuelType == MachineFuelType.DOUBLE) {
                return getRecipe(slotStack, extraStack);
            } else if (fuelType == MachineFuelType.CHANCE) {
                return getChanceRecipe(slotStack);
            } else if (fuelType == MachineFuelType.FARM) {
                return getFarmRecipe(slotStack, gasType);
            } else if (fuelType == MachineFuelType.CHANCE2) {
                return getChance2Recipe(slotStack);
            } else if (this == Crystallizer) {
                return getCrystallizerRecipe(gasStackType);
            } else if (this == Dissolution) {
                return getDissolutionRecipe(slotStack);
            } else if (this == PRC) {
                return getPressurizedRecipe(slotStack, fluidStack, gasStackType);
            } else if (this == OXIDIZER) {
                return getOxidationRecipe(slotStack);
            } else if (this == NUCLEOSYNTHESIZER) {
                return getNucleosynthesizerRecipe(slotStack, gasStackType);
            } else if (this == WASHER) {
                return getWasherRecipe(gasStackType,fluidStack);
            } else if (this == INFUSING) {
                if (infuse.getType() != null) {
                    return RecipeHandler.getMetallurgicInfuserRecipe(new InfusionInput(infuse, slotStack));
                }
                for (Entry<InfusionInput, MetallurgicInfuserRecipe> entry : Recipe.METALLURGIC_INFUSER.get().entrySet()) {
                    if (ItemHandlerHelper.canItemStacksStack(entry.getKey().inputStack, slotStack)) {
                        return entry.getValue();
                    }
                }
            }
            return getRecipe(slotStack);
        }

        public int getSecondaryEnergyPerTick() {
            if (fuelType == MachineFuelType.ADVANCED) {
                return getTile().BASE_SECONDARY_ENERGY_PER_TICK;
            } else if (fuelType == MachineFuelType.FARM) {
                return getTile2().BASE_SECONDARY_ENERGY_PER_TICK;
            }
            return 0;
        }

        public boolean canReceiveGas(EnumFacing side, Gas type) {
            if (fuelType == MachineFuelType.ADVANCED) {
                return getTile().canReceiveGas(side, type);
            } else if (fuelType == MachineFuelType.FARM) {
                return getTile2().canReceiveGas(side, type);
            }
            return false;
        }

        public boolean isValidGas(Gas gas) {
            if (fuelType == MachineFuelType.ADVANCED) {
                return getTile().isValidGas(gas);
            } else if (fuelType == MachineFuelType.FARM) {
                return getTile2().isValidGas(gas);
            } else if (this == Crystallizer) {
                return Recipe.CHEMICAL_CRYSTALLIZER.containsRecipe(gas);
            } else if (this == Dissolution) {
                return gas == MekanismFluids.SulfuricAcid; //todo
            } else if (this == PRC) {
                return Recipe.PRESSURIZED_REACTION_CHAMBER.containsRecipe(gas);
            } else if (this == WASHER) {
                return Recipe.CHEMICAL_WASHER.containsRecipe(gas);
            } else if (this == NUCLEOSYNTHESIZER) {
                return Recipe.ANTIPROTONIC_NUCLEOSYNTHESIZER.containsRecipe(gas);
            }
            return false;
        }

        public boolean hasRecipe(ItemStack itemStack) {
            if (itemStack.isEmpty()) {
                return false;
            }
            rebuildInputMatchersIfNeeded();
            List<AdvancedMachineInput> matchers = cachedAdvancedInputMatchers.get(itemStack.getItem());
            if (matchers == null || matchers.isEmpty()) {
                return false;
            }
            for (AdvancedMachineInput input : matchers) {
                if (StackUtils.equalsWildcard(input.itemStack, itemStack)) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasRecipeForExtra(ItemStack extraStack) {
            if (extraStack.isEmpty()) {
                return false;
            }
            rebuildInputMatchersIfNeeded();
            List<DoubleMachineInput> matchers = cachedDoubleExtraMatchers.get(extraStack.getItem());
            if (matchers == null || matchers.isEmpty()) {
                return false;
            }
            for (DoubleMachineInput input : matchers) {
                if (StackUtils.equalsWildcard(input.extraStack, extraStack)) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasRecipeForInput(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            rebuildInputMatchersIfNeeded();
            List<Object> matchers = cachedInputMatchers.get(stack.getItem());
            if (matchers == null || matchers.isEmpty()) {
                return false;
            }
            for (Object matcher : matchers) {
                if (matcher instanceof AdvancedMachineInput input && MachineInput.inputItemMatches(input.itemStack, stack)) {
                    return true;
                }
                if (matcher instanceof ItemStackInput input && MachineInput.inputItemMatches(input.ingredient, stack)) {
                    return true;
                }
                if (matcher instanceof DoubleMachineInput input && MachineInput.inputItemMatches(input.itemStack, stack)) {
                    return true;
                }
                if (matcher instanceof InfusionInput input && MachineInput.inputItemMatches(input.inputStack, stack)) {
                    return true;
                }
                if (matcher instanceof NucleosynthesizerInput input && MachineInput.inputItemMatches(input.getSolid(), stack)) {
                    return true;
                }
                if (matcher instanceof PressurizedInput input && MachineInput.inputItemMatches(input.getSolid(), stack)) {
                    return true;
                }
            }
            return false;
        }

        private void rebuildInputMatchersIfNeeded() {
            int recipeSize = recipe.get().size();
            if (cachedInputMatchers != null && cachedInputMatchersRecipeSize == recipeSize) {
                return;
            }
            Map<Item, List<Object>> rebuiltMatchers = new HashMap<>();
            Map<Item, List<AdvancedMachineInput>> rebuiltAdvancedMatchers = new HashMap<>();
            Map<Item, List<DoubleMachineInput>> rebuiltExtraMatchers = new HashMap<>();
            for (Object obj : recipe.get().entrySet()) {
                Object key = ((Map.Entry<?, ?>) obj).getKey();
                if (key instanceof AdvancedMachineInput input) {
                    addInputMatcher(rebuiltMatchers, input.itemStack, key);
                    addTypedMatcher(rebuiltAdvancedMatchers, input.itemStack, input);
                } else if (key instanceof ItemStackInput input) {
                    addInputMatcher(rebuiltMatchers, input.ingredient, key);
                } else if (key instanceof DoubleMachineInput input) {
                    addInputMatcher(rebuiltMatchers, input.itemStack, key);
                    addTypedMatcher(rebuiltExtraMatchers, input.extraStack, input);
                } else if (key instanceof InfusionInput input) {
                    addInputMatcher(rebuiltMatchers, input.inputStack, key);
                } else if (key instanceof NucleosynthesizerInput input) {
                    addInputMatcher(rebuiltMatchers, input.getSolid(), key);
                } else if (key instanceof PressurizedInput input) {
                    addInputMatcher(rebuiltMatchers, input.getSolid(), key);
                }
            }
            cachedInputMatchers = rebuiltMatchers;
            cachedAdvancedInputMatchers = rebuiltAdvancedMatchers;
            cachedDoubleExtraMatchers = rebuiltExtraMatchers;
            cachedInputMatchersRecipeSize = recipeSize;
        }

        private static void addInputMatcher(Map<Item, List<Object>> matcherMap, ItemStack ingredient, Object matcher) {
            if (ingredient.isEmpty()) {
                return;
            }
            matcherMap.computeIfAbsent(ingredient.getItem(), item -> new ArrayList<>()).add(matcher);
        }

        private static <T> void addTypedMatcher(Map<Item, List<T>> matcherMap, ItemStack ingredient, T matcher) {
            if (ingredient.isEmpty()) {
                return;
            }
            matcherMap.computeIfAbsent(ingredient.getItem(), item -> new ArrayList<>()).add(matcher);
        }


        public TileEntityAdvancedElectricMachine getTile() {
            if (AdvancedElectricMachineCacheTile == null) {
                MachineType type = MachineType.get(getStack());
                AdvancedElectricMachineCacheTile = (TileEntityAdvancedElectricMachine) type.create();
            }
            return AdvancedElectricMachineCacheTile;
        }

        public TileEntityFarmMachine getTile2() {
            if (FarmMachineCacheTile == null) {
                MachineType type = MachineType.get(getStack());
                FarmMachineCacheTile = (TileEntityFarmMachine) type.create();
            }
            return FarmMachineCacheTile;
        }

        public double getEnergyUsage() {
            return type.getUsage();
        }

        public double getEnergyStorage() {
            return type.getStorage();
        }

        public ItemStack getStack() {
            return type.getStack();
        }

        public String getTranslationKey() {
            return name;
        }

        public String getLocalizedName() {
            return LangUtils.localize("gui.factory." + name);
        }

        public SoundEvent getSound() {
            return sound;
        }

        public MachineFuelType getFuelType() {
            return fuelType;
        }

        public boolean fuelEnergyUpgrades() {
            return fuelSpeed;
        }

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public MachineType getType() {
            return type;
        }

        public Recipe getrecipe() {
            return recipe;
        }

        public boolean supportsGas() {
            return getCanInputGas() || getCanOuputGas();
        }

        public boolean getCanInputItem() {
            return canInputItem;
        }

        public boolean getCanOuputItem() {
            return canOutputItem;
        }

        public boolean getCanInputGas() {
            return canInputGas;
        }

        public boolean getCanOuputGas() {
            return canOuputGas;
        }

        public boolean getCanInputFluid() {
            return canInputFliud;
        }
    }
}
