package gregtech.common.tools;

import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IElectricItem;
import gregtech.api.items.metaitem.MetaItem;
import gregtech.common.items.MetaItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ToolChainsaw extends ToolSaw {

    private final int tier;

    public ToolChainsaw(int tier) {
        this.tier = tier;
    }

    @Override
    public int getToolDamagePerBlockBreak(ItemStack stack) {
        switch (tier) {
            case GTValues.LV: return 1;
            case GTValues.MV: return 4;
            case GTValues.HV: return 8;
        }
        return super.getToolDamagePerBlockBreak(stack);
    }

    @Override
    public int getToolDamagePerDropConversion(ItemStack stack) {
        switch (tier) {
            case GTValues.LV: return 1;
            case GTValues.MV: return 4;
            case GTValues.HV: return 16;
        }
        return super.getToolDamagePerDropConversion(stack);
    }

    @Override
    public int getToolDamagePerContainerCraft(ItemStack stack) {
        switch (tier) {
            case GTValues.LV: return 8;
            case GTValues.MV: return 32;
            case GTValues.HV: return 128;
        }
        return super.getToolDamagePerContainerCraft(stack);
    }

    @Override
    public int getToolDamagePerEntityAttack(ItemStack stack) {
        switch (tier) {
            case GTValues.LV: return 2;
            case GTValues.MV: return 8;
            case GTValues.HV: return 32;
        }
        return super.getToolDamagePerEntityAttack(stack);
    }

    @Override
    public int getBaseQuality(ItemStack stack) {
        switch (tier) {
            case GTValues.LV: return 0;
            case GTValues.MV:
            case GTValues.HV: return 1;
        }
        return super.getBaseQuality(stack);
    }

    @Override
    public float getBaseDamage(ItemStack stack) {
        switch (tier) {
            case GTValues.LV: return 4.0F;
            case GTValues.MV: return 5.0F;
            case GTValues.HV: return 6.0F;
        }
        return super.getBaseDamage(stack);
    }

    @Override
    public float getDigSpeedMultiplier(ItemStack stack) {
        switch (tier) {
            case GTValues.LV: return 2.0F;
            case GTValues.MV: return 3.0F;
            case GTValues.HV: return 4.0F;
        }
        return super.getDigSpeedMultiplier(stack);
    }

    @Override
    public float getMaxDurabilityMultiplier(ItemStack stack) {
        switch (tier) {
            case GTValues.LV: return 1.0F;
            case GTValues.MV: return 2.0F;
            case GTValues.HV: return 4.0F;
        }
        return super.getMaxDurabilityMultiplier(stack);
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        super.onBlockDestroyed(stack, world, state, pos, entity);
        if (!entity.isSneaking() && entity instanceof EntityPlayer) {
            this.onBreakingUse(stack, world, pos);
            ToolUtility.applyTimberAxe(stack, world, pos, (EntityPlayer) entity);
        }
    }

    @Override
    public ItemStack getBrokenStack(ItemStack stack) {
        IElectricItem electricItem = stack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
        MetaItem<?>.MetaValueItem powerUnit = null;
        switch (tier) {
            case GTValues.LV: powerUnit = MetaItems.POWER_UNIT_LV; break;
            case GTValues.MV: powerUnit = MetaItems.POWER_UNIT_MV; break;
            case GTValues.HV: powerUnit = MetaItems.POWER_UNIT_HV; break;
        }
        return powerUnit == null ? ItemStack.EMPTY : powerUnit.getChargedStackWithOverride(electricItem);
    }
}
