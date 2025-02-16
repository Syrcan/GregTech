package gregtech.api.capability.impl;

import java.util.List;
import java.util.ArrayList;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IElectricItem;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTUtility;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandlerModifiable;

public class EnergyContainerBatteryBuffer extends EnergyContainerHandler {

    private final int tier;

    public EnergyContainerBatteryBuffer(MetaTileEntity metaTileEntity, int tier, int inventorySize) {
        super(metaTileEntity, GTValues.V[tier] * inventorySize * 32L, GTValues.V[tier], inventorySize * 2L, GTValues.V[tier], inventorySize);
        this.tier = tier;
    }

    @Override
    public long acceptEnergyFromNetwork(EnumFacing side, long voltage, long amperage) {
        if (amperage <= 0 || voltage <= 0)
            return 0;

        List<IElectricItem> batteries = getNonFullBatteries();
        long maxAmps = batteries.size() * 2L - amps;
        long usedAmps = Math.min(maxAmps, amperage);
        if (maxAmps <= 0)
            return 0;

        if (side == null || inputsEnergy(side)) {
            if (voltage > getInputVoltage()) {
                GTUtility.doOvervoltageExplosion(metaTileEntity, voltage);
                return usedAmps;
            }

            //Prioritizes as many packets as available from the buffer
            long internalAmps = Math.min(maxAmps, Math.max(0, getInternalStorage() / voltage));

            usedAmps = Math.min(usedAmps, maxAmps - internalAmps);
            amps += usedAmps;
            energyInputPerSec += usedAmps * voltage;

            long energy = (usedAmps + internalAmps) * voltage;
            long distributed = energy / batteries.size();

            for (IElectricItem electricItem : batteries) {
                energy -= electricItem.charge(Math.min(distributed, GTValues.V[electricItem.getTier()] * 2L), getTier(), true, false);
            }

            //Remove energy used and then transfer overflow energy into the internal buffer
            setEnergyStored(getInternalStorage() - internalAmps * voltage + energy);
            return usedAmps;
        }
        return 0;
    }

    @Override
    public void update() {
        amps = 0;
        if (metaTileEntity.getWorld().isRemote) {
            return;
        }
        if (metaTileEntity.getOffsetTimer() % 20 == 0) {
            lastEnergyInputPerSec = energyInputPerSec;
            lastEnergyOutputPerSec = energyOutputPerSec;
            energyInputPerSec = 0;
            energyOutputPerSec = 0;
        }

        EnumFacing outFacing = metaTileEntity.getFrontFacing();
        TileEntity tileEntity = metaTileEntity.getWorld().getTileEntity(metaTileEntity.getPos().offset(outFacing));
        if (tileEntity == null) {
            return;
        }
        IEnergyContainer energyContainer = tileEntity.getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, outFacing.getOpposite());
        if (energyContainer == null) {
            return;
        }

        long voltage = getOutputVoltage();
        List<IElectricItem> batteries = getNonEmptyBatteries();
        if (batteries.size() > 0) {
            //Prioritize as many packets as available of energy created
            long internalAmps = Math.abs(Math.min(0, getInternalStorage() / voltage));
            long genAmps = Math.max(0, batteries.size() - internalAmps);
            long outAmps = 0L;

            if (genAmps > 0) {
                outAmps = energyContainer.acceptEnergyFromNetwork(outFacing.getOpposite(), voltage, genAmps);
                if (outAmps == 0 && internalAmps == 0)
                    return;
                energyOutputPerSec += outAmps * voltage;
            }

            long energy = (outAmps + internalAmps) * voltage;
            long distributed = energy / batteries.size();

            for (IElectricItem electricItem : batteries) {
                energy -= electricItem.discharge(distributed, getTier(), false, true, false);
            }

            //Subtract energy created out of thin air from the buffer
            setEnergyStored(getInternalStorage() + internalAmps * voltage - energy);
        }
    }

    private long getInternalStorage() {
        return energyStored;
    }

    private List<IElectricItem> getNonFullBatteries() {
        IItemHandlerModifiable inventory = getInventory();
        List<IElectricItem> batteries = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack batteryStack = inventory.getStackInSlot(i);
            IElectricItem electricItem = getBatteryContainer(batteryStack);
            if (electricItem == null) continue;
            if (electricItem.getCharge() < electricItem.getMaxCharge()) {
                batteries.add(electricItem);
            }
        }
        return batteries;
    }

    private List<IElectricItem> getNonEmptyBatteries() {
        IItemHandlerModifiable inventory = getInventory();
        List<IElectricItem> batteries = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack batteryStack = inventory.getStackInSlot(i);
            IElectricItem electricItem = getBatteryContainer(batteryStack);
            if (electricItem == null) continue;
            if (electricItem.canProvideChargeExternally() && electricItem.getCharge() > 0) {
                batteries.add(electricItem);
            }
        }
        return batteries;
    }

    @Override
    public long getEnergyCapacity() {
        long energyCapacity = 0L;
        IItemHandlerModifiable inventory = getInventory();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack batteryStack = inventory.getStackInSlot(i);
            IElectricItem electricItem = getBatteryContainer(batteryStack);
            if (electricItem == null) continue;
            energyCapacity += electricItem.getMaxCharge();
        }
        return energyCapacity;
    }

    @Override
    public long getEnergyStored() {
        long energyStored = 0L;
        IItemHandlerModifiable inventory = getInventory();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack batteryStack = inventory.getStackInSlot(i);
            IElectricItem electricItem = getBatteryContainer(batteryStack);
            if (electricItem == null) continue;
            energyStored += electricItem.getCharge();
        }
        return energyStored;
    }

    @Override
    public void setEnergyStored(long energyStored) {
        this.energyStored = energyStored;
        if (!metaTileEntity.getWorld().isRemote) {
            metaTileEntity.markDirty();
            notifyEnergyListener(false);
        }
    }

    public IElectricItem getBatteryContainer(ItemStack itemStack) {
        IElectricItem electricItem = itemStack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
        if (electricItem != null && getTier() >= electricItem.getTier())
            return electricItem;
        return null;
    }

    public void notifyEnergyListener(boolean isInitialChange) {
        if (metaTileEntity instanceof IEnergyChangeListener) {
            ((IEnergyChangeListener) metaTileEntity).onEnergyChanged(this, isInitialChange);
        }
    }

    @Override
    public boolean inputsEnergy(EnumFacing side) {
        return getMetaTileEntity().getFrontFacing() != side;
    }

    @Override
    public boolean outputsEnergy(EnumFacing side) {
        return !inputsEnergy(side);
    }

    @Override
    public String getName() {
        return "BatteryEnergyContainer";
    }

    protected IItemHandlerModifiable getInventory() {
        return metaTileEntity.getImportItems();
    }

    protected int getTier() {
        return tier;
    }
}
