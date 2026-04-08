package mekanism.common.item.armor;

import ic2.api.item.IHazmatLike;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.ItemCapabilityWrapper;
import mekanism.common.capabilities.radiation.item.NCRadiationShieldingHandler;
import mekanism.common.capabilities.radiation.item.RadiationShieldingHandler;
import mekanism.common.integration.MekanismHooks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.Method;

import javax.annotation.Nonnull;

@Interface(iface = "ic2.api.item.IHazmatLike", modid = MekanismHooks.IC2_MOD_ID)
public class ItemHazmatSuitArmor extends ItemArmor implements IHazmatLike {

    public static ArmorMaterial MekHazmat = EnumHelper.addArmorMaterial("mekhazmat", "mekanism:hazmat", 0, new int[]{0, 0, 0, 0}, 0, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.0F);

    public ItemHazmatSuitArmor(int renderIndexIn, EntityEquipmentSlot equipmentSlotIn) {
        super(MekHazmat, renderIndexIn, equipmentSlotIn);
        setMaxStackSize(1);
        setCreativeTab(Mekanism.tabMekanism);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        int layer = slot == EntityEquipmentSlot.LEGS ? 2 : 1;
        return "mekanism:armor/hazmat_layer_" + layer + ".png";
    }


    public static double getShieldingByArmor(EntityEquipmentSlot type) {
        if (type == EntityEquipmentSlot.HEAD) {
            return 0.25;
        } else if (type == EntityEquipmentSlot.CHEST) {
            return 0.4;
        } else if (type == EntityEquipmentSlot.LEGS) {
            return 0.2;
        } else if (type == EntityEquipmentSlot.FEET) {
            return 0.15;
        }
        return 0;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
        return new ItemCapabilityWrapper(stack, RadiationShieldingHandler.create(item -> getShieldingByArmor(armorType)),
                NCRadiationShieldingHandler.create(item -> getShieldingByArmor(armorType) * 100, item -> 0, false));
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        super.onUpdate(stack, world, entity, itemSlot, isSelected);
        NCRadiationShieldingHandler.clearLegacyArmorShieldingTag(stack);
    }

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player) {
        super.onCreated(stack, world, player);
        NCRadiationShieldingHandler.clearLegacyArmorShieldingTag(stack);
    }

    @Override
    public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
        super.onArmorTick(world, player, stack);
        NCRadiationShieldingHandler.clearLegacyArmorShieldingTag(stack);
    }

    @Override
    public boolean isEnchantable(@Nonnull ItemStack stack) {
        return getItemEnchantability() > 0 && super.isEnchantable(stack);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return isEnchantable(stack) && super.isBookEnchantable(stack, book);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return isEnchantable(stack) && super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    @Method(modid = MekanismHooks.IC2_MOD_ID)
    public boolean addsProtection(EntityLivingBase entityLivingBase, EntityEquipmentSlot entityEquipmentSlot, ItemStack itemStack) {
        return true;
    }
}
