package gregtech.common.items.behaviors;

import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IElectricItem;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.impl.ModularUIContainer;
import gregtech.api.items.gui.ItemUIFactory;
import gregtech.api.items.gui.PlayerInventoryHolder;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.common.terminal.app.prospector.widget.WidgetOreList;
import gregtech.common.terminal.app.prospector.widget.WidgetProspectingMap;
import gregtech.common.terminal.component.SearchComponent;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class ProspectorScannerBehavior implements IItemBehaviour, ItemUIFactory, SearchComponent.IWidgetSearch<String> {

    private static final long VOLTAGE_FACTOR = 16L;
    private static final int FLUID_PROSPECTION_THRESHOLD = GTValues.HV;

    private final int radius;
    private final int tier;
    private int mode;

    private WidgetOreList widgetOreList;

    public ProspectorScannerBehavior(int radius, int tier) {
        this.radius = radius + 1;
        this.tier = tier;
        this.mode = 0;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, @Nonnull EntityPlayer player, EnumHand hand) {
        ItemStack heldItem = player.getHeldItem(hand);
        if (!world.isRemote) {
            if (player.isSneaking()) {
                if (getNextMode() == WidgetProspectingMap.ORE_PROSPECTING_MODE) {
                    if (this.tier >= FLUID_PROSPECTION_THRESHOLD)
                        incrementMode();
                    player.sendMessage(new TextComponentTranslation("metaitem.prospector.mode.ores"));
                } else if (getNextMode() == WidgetProspectingMap.FLUID_PROSPECTING_MODE && this.tier >= FLUID_PROSPECTION_THRESHOLD) {
                    incrementMode();
                    player.sendMessage(new TextComponentTranslation("metaitem.prospector.mode.fluid"));
                }
            } else if (checkCanUseScanner(heldItem, player, true)) {
                PlayerInventoryHolder holder = new PlayerInventoryHolder(player, hand);
                holder.openUI();
            } else {
                player.sendMessage(new TextComponentTranslation("behavior.scanner.not_enough_energy"));
            }
        }
        return ActionResult.newResult(EnumActionResult.SUCCESS, heldItem);
    }

    private int getNextMode() {
        return (this.mode + 1) % 2;
    }

    private void incrementMode() {
        this.mode = getNextMode();
    }

    private boolean checkCanUseScanner(ItemStack stack, @Nonnull EntityPlayer player, boolean simulate) {
        return player.isCreative() || drainEnergy(stack, GTValues.V[tier] / VOLTAGE_FACTOR, simulate);
    }

    private boolean drainEnergy(@Nonnull ItemStack stack, long amount, boolean simulate) {
        IElectricItem electricItem = stack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
        if (electricItem == null)
            return false;

        return electricItem.discharge(amount, Integer.MAX_VALUE, true, false, simulate) >= amount;
    }

    @Override
    public ModularUI createUI(PlayerInventoryHolder holder, @Nonnull EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 332, 200);
        this.widgetOreList = new WidgetOreList(32 * radius - 6, 18, 332 - 32 * radius, 176);
        builder.widget(this.widgetOreList);
        builder.widget(new WidgetProspectingMap(6, 18, radius, this.widgetOreList, mode, 1));
        return builder.label(6, 6, getTranslationKey()).build(holder, entityPlayer);
    }

    private String getTranslationKey() {
        return String.format("metaitem.prospector.%s.name", GTValues.VN[tier].toLowerCase(Locale.ROOT));
    }

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        IItemBehaviour.super.addInformation(itemStack, lines);
        if (tier >= GTValues.HV) lines.add(I18n.format("metaitem.prospector.tooltip.fluids", radius));
        else lines.add(I18n.format("metaitem.prospector.tooltip.ores", radius));
    }

    @Override
    public String resultDisplay(String result) {
        if (widgetOreList != null) {
            return widgetOreList.ores.get(result);
        }
        return "";
    }

    @Override
    public void selectResult(String result) {
        if (widgetOreList != null) {
            widgetOreList.setSelected(result);
        }
    }

    @Override
    public void search(String word, Consumer<String> find) {
        if (widgetOreList != null) {
            word = word.toLowerCase();
            for (Map.Entry<String, String> entry : widgetOreList.ores.entrySet()) {
                if (entry.getKey().toLowerCase().contains(word) || entry.getValue().toLowerCase().contains(word)) {
                    find.accept(entry.getKey());
                }
            }
        }
    }

    @Override
    public void onUpdate(ItemStack itemStack, Entity entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player.openContainer instanceof ModularUIContainer) {
                ModularUIContainer modularUIContainer = (ModularUIContainer) player.openContainer;
                if (modularUIContainer.getModularUI().holder instanceof PlayerInventoryHolder) {
                    if (((PlayerInventoryHolder) (modularUIContainer).getModularUI().holder).getCurrentItem() == itemStack) {
                        if (!player.isCreative()) {
                            if (checkCanUseScanner(itemStack, player, true))
                                drainEnergy(itemStack, GTValues.V[tier] / VOLTAGE_FACTOR, false);
                            else
                                player.closeScreen();
                        }
                    }
                }
            }
        }
    }
}
