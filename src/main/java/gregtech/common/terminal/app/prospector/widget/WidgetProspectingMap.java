package gregtech.common.terminal.app.prospector.widget;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.net.packets.PacketProspecting;
import gregtech.api.unification.OreDictUnifier;
import gregtech.api.util.GTUtility;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import gregtech.api.worldgen.bedrockFluids.BedrockFluidVeinHandler;
import gregtech.common.terminal.app.prospector.ProspectingTexture;
import net.minecraft.block.Block;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WidgetProspectingMap extends Widget {
    private final int chunkRadius;
    private final WidgetOreList oreList;
    private final int mode;
    private final int scanTick;
    private boolean darkMode = false;
    private int chunkIndex = 0;
    @SideOnly(Side.CLIENT)
    private ProspectingTexture texture;

    public static final int ORE_PROSPECTING_MODE = 0;
    public static final int FLUID_PROSPECTING_MODE = 1;

    public WidgetProspectingMap(int xPosition, int yPosition, int chunkRadius, WidgetOreList widgetOreList, int mode, int scanTick) {
        super(new Position(xPosition, yPosition), new Size(16 * (chunkRadius * 2 - 1), 16 * (chunkRadius * 2 - 1)));
        this.chunkRadius = chunkRadius;
        this.mode = mode;
        this.scanTick = scanTick;
        oreList = widgetOreList;
        if (oreList != null) {
            oreList.onSelected = name->{
                if (texture != null) {
                    texture.loadTexture(null, name);
                }
            };
        }
    }

    @SideOnly(Side.CLIENT)
    public void setDarkMode(boolean mode) {
        if (darkMode != mode) {
            darkMode = mode;
            if (texture != null) {
                texture.loadTexture(null, darkMode);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public boolean getDarkMode() {
        return darkMode;
    }

    @Override
    public void detectAndSendChanges() {
        EntityPlayer player = gui.entityPlayer;
        World world = player.world;
        if (FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter() % scanTick == 0 && chunkIndex < (chunkRadius * 2 - 1) * (chunkRadius * 2 - 1)) {
            int cX = ((int) player.posX) >> 4;
            int cZ = ((int) player.posZ) >> 4;
            int r = (int) Math.floor(Math.sqrt(chunkIndex));
            r = r / 2 + ((r % 2 == 0) ? 0 : 1);
            int side = r == 0 ? 0 : (chunkIndex -  (2 * r - 1) * (2 * r - 1)) / (2 * r);
            int offset = r == 0 ? -1 : (chunkIndex -  (2 * r - 1) * (2 * r - 1)) % (2 * r);

            int ox = side == 0 ? -r : side == 1 ? (offset - r + 1) : side == 2 ? r : -(offset - r + 1);
            int oz = side == 3 ? r : side == 0 ? -(offset - r + 1) : side == 1 ? -r : (offset - r + 1);

            Chunk chunk = world.getChunk(cX + ox, cZ + oz);
            PacketProspecting packet = new PacketProspecting(cX + ox, cZ + oz, (int) player.posX, (int) player.posZ, this.mode);

            switch (mode) {
                case ORE_PROSPECTING_MODE:
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            int ySize = chunk.getHeightValue(x, z);
                            for (int y = 1; y < ySize; y++) {
                                Block block = chunk.getBlockState(x, y, z).getBlock();
                                if (GTUtility.isOre(block)) {
                                    packet.addBlock(x, y, z, OreDictUnifier.getOreDictionaryNames(new ItemStack(block)).stream().findFirst().get());
                                }
                            }
                        }
                    }
                    break;
                case FLUID_PROSPECTING_MODE:
                    BedrockFluidVeinHandler.FluidVeinWorldEntry fStack = BedrockFluidVeinHandler.getFluidVeinWorldEntry(world, chunk.x, chunk.z);
                    if (fStack != null && fStack.getVein() != null) {
                        packet.addBlock(0, 2, 0, fStack.getCurrentFluidAmount() + "");
                        packet.addBlock(0, 1, 0, fStack.getVein().getStoredFluid().getName());
                    }
                    break;
                default:
                    break;
            }
            writeUpdateInfo(2, packet::writePacketData);
//            if (oreList != null) {
//                oreList.addOres(packet.ores, packet.mode);
//            }
            chunkIndex++;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void drawInBackground(int mouseX, int mouseY, float partialTicks, IRenderContext context) {
        if(texture !=null) {
            GlStateManager.color(1,1,1,1);
            texture.draw(this.getPosition().x, this.getPosition().y);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == 2) {
            PacketProspecting packet = PacketProspecting.readPacketData(buffer);
            if (packet != null) {
                if (texture == null) {
                    texture = new ProspectingTexture(packet.mode, chunkRadius, darkMode);
                }
                texture.updateTexture(packet);
                if (oreList != null) {
                    oreList.addOres(packet.ores, packet.mode);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void drawInForeground(int mouseX, int mouseY) {
        // draw tooltips
        if (this.isMouseOverElement(mouseX, mouseY) && texture != null) {
            List<String> tooltips = new ArrayList<>();
            int cX = (mouseX - this.getPosition().x) / 16;
            int cZ = (mouseY - this.getPosition().y) / 16;
            if (cX >= chunkRadius * 2 - 1 || cZ >= chunkRadius * 2 - 1)
                return;
            // draw hover layer
            Gui.drawRect(cX * 16 + this.getPosition().x,
                    cZ * 16 + this.getPosition().y,
                    (cX + 1) * 16 + this.getPosition().x,
                    (cZ + 1) * 16 + this.getPosition().y,
                    new Color(0x4B6C6C6C, true).getRGB());
            if (this.mode == 0) { // draw ore
                tooltips.add(I18n.format("terminal.prospector.ore"));
                HashMap<String, Integer> oreInfo = new HashMap<>();
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        if (texture.map[cX * 16 + i][cZ * 16 + j] != null) {
                            texture.map[cX * 16 + i][cZ * 16 + j].values().forEach(dict -> {
                                String name = OreDictUnifier.get(dict).getDisplayName();
                                if (texture.getSelected().equals("[all]") || texture.getSelected().equals(dict)) {
                                    oreInfo.put(name, oreInfo.getOrDefault(name, 0) + 1);
                                }
                            });
                        }
                    }
                }
                oreInfo.forEach((name, count)->tooltips.add(name + " --- " + count));
            } else if(this.mode == 1){
                tooltips.add(I18n.format("terminal.prospector.fluid"));
                if (texture.map[cX][cZ] != null && !texture.map[cX][cZ].isEmpty()) {
                    String name = FluidRegistry.getFluidStack(texture.map[cX][cZ].get((byte) 1),1).getLocalizedName();
                    if (texture.getSelected().equals("[all]") || texture.getSelected().equals(texture.map[cX][cZ].get((byte) 1))) {
                        tooltips.add(name + " --- " + texture.map[cX][cZ].get((byte) 2));
                    }
                }
            }
            this.drawHoveringText(ItemStack.EMPTY, tooltips, 300, mouseX, mouseY);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
        }
    }
}
