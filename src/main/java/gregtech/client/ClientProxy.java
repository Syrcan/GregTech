package gregtech.client;

import codechicken.lib.texture.TextureUtils;
import codechicken.lib.util.ItemNBTUtils;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.realmsclient.gui.ChatFormatting;
import gregtech.api.GTValues;
import gregtech.client.model.customtexture.CustomTextureModelHandler;
import gregtech.client.model.customtexture.MetadataSectionCTM;
import gregtech.client.renderer.handler.MetaTileEntityRenderer;
import gregtech.client.renderer.handler.SurfaceRockRenderer;
import gregtech.client.shader.Shaders;
import gregtech.api.terminal.TerminalRegistry;
import gregtech.api.unification.OreDictUnifier;
import gregtech.api.unification.material.info.MaterialIconSet;
import gregtech.api.unification.material.info.MaterialIconType;
import gregtech.api.unification.stack.UnificationEntry;
import gregtech.api.util.FluidTooltipUtil;
import gregtech.api.util.GTLog;
import gregtech.api.util.ModCompatibility;
import gregtech.api.util.input.KeyBinds;
import gregtech.common.CommonProxy;
import gregtech.common.ConfigHolder;
import gregtech.common.MetaEntities;
import gregtech.common.MetaFluids;
import gregtech.common.blocks.*;
import gregtech.client.renderer.handler.FacadeRenderer;
import gregtech.common.items.MetaItems;
import gregtech.client.renderer.pipe.CableRenderer;
import gregtech.client.renderer.pipe.FluidPipeRenderer;
import gregtech.client.renderer.pipe.ItemPipeRenderer;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    private static final ResourceLocation GREGTECH_CAPE_TEXTURE = new ResourceLocation(GTValues.MODID, "textures/gregtechcape.png");

    public static final IBlockColor COMPRESSED_BLOCK_COLOR = (IBlockState state, IBlockAccess worldIn, BlockPos pos, int tintIndex) ->
            state.getValue(((BlockCompressed) state.getBlock()).variantProperty).getMaterialRGB();

    public static final IItemColor COMPRESSED_ITEM_COLOR = (stack, tintIndex) -> {
        BlockCompressed block = (BlockCompressed) ((ItemBlock) stack.getItem()).getBlock();
        IBlockState state = block.getStateFromMeta(stack.getItemDamage());
        return state.getValue(block.variantProperty).getMaterialRGB();
    };

    public static final IBlockColor FRAME_BLOCK_COLOR = (IBlockState state, IBlockAccess worldIn, BlockPos pos, int tintIndex) ->
            state.getValue(((BlockFrame) state.getBlock()).variantProperty).getMaterialRGB();

    public static final IItemColor FRAME_ITEM_COLOR = (stack, tintIndex) -> {
        BlockFrame block = (BlockFrame) ((ItemBlock) stack.getItem()).getBlock();
        IBlockState state = block.getStateFromMeta(stack.getItemDamage());
        return state.getValue(block.variantProperty).getMaterialRGB();
    };

    public static final IBlockColor ORE_BLOCK_COLOR = (IBlockState state, IBlockAccess worldIn, BlockPos pos, int tintIndex) ->
            tintIndex == 1 ? ((BlockOre) state.getBlock()).material.getMaterialRGB() : 0xFFFFFF;

    public static final IItemColor ORE_ITEM_COLOR = (stack, tintIndex) ->
            tintIndex == 1 ? ((BlockOre) ((ItemBlock) stack.getItem()).getBlock()).material.getMaterialRGB() : 0xFFFFFF;

    public static final IBlockColor FOAM_BLOCK_COLOR = (IBlockState state, IBlockAccess worldIn, BlockPos pos, int tintIndex) ->
            state.getValue(BlockColored.COLOR).colorValue;

    public void onPreLoad() {
        super.onPreLoad();

        if (!GTValues.isModLoaded(GTValues.MODID_CTM)) {
            Minecraft.getMinecraft().metadataSerializer.registerMetadataSectionType(new MetadataSectionCTM.Serializer(), MetadataSectionCTM.class);
            MinecraftForge.EVENT_BUS.register(CustomTextureModelHandler.INSTANCE);
            ((SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(CustomTextureModelHandler.INSTANCE);
        }

        KeyBinds.initBinds();
        MetaTileEntityRenderer.preInit();
        CableRenderer.preInit();
        FluidPipeRenderer.preInit();
        ItemPipeRenderer.preInit();
        SurfaceRockRenderer.preInit();
        MetaEntities.initRenderers();
        TextureUtils.addIconRegister(MetaFluids::registerSprites);
    }

    @Override
    public void onLoad() {
        KeyBinds.registerClient();
        super.onLoad();
        if (ConfigHolder.misc.debug) {
            ClientCommandHandler.instance.registerCommand(new Shaders.ShaderCommand());
        }
        registerColors();
    }

    @Override
    public void onPostLoad() {
        super.onPostLoad();
        TerminalRegistry.initTerminalFiles();
        ModCompatibility.initCompat();
        FacadeRenderer.init();
        startCapeLoadingThread();
    }

    public void registerColors() {
        MetaBlocks.registerColors();
        MetaItems.registerColors();
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        MetaBlocks.registerStateMappers();
        MetaBlocks.registerItemModels();
        MetaItems.registerModels();
    }

    @SubscribeEvent
    public static void registerSprites(TextureStitchEvent.Pre event) {
        for (MaterialIconSet set : MaterialIconSet.ICON_SETS.values()) {
            event.getMap().registerSprite(MaterialIconType.ore.getBlockPath(set));
            event.getMap().registerSprite(MaterialIconType.block.getBlockPath(set));
        }
        MetaBlocks.COMPRESSED.values().stream().distinct().forEach(c -> c.onTextureStitch(event));
        MetaBlocks.FRAMES.values().stream().distinct().forEach(f -> f.onTextureStitch(event));
        MetaBlocks.ORES.forEach(o -> o.onTextureStitch(event));
    }

    @SubscribeEvent
    public static void addMaterialFormulaHandler(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();

        // Handles Item tooltips
        String chemicalFormula = null;

        // Test for Items
        UnificationEntry unificationEntry = OreDictUnifier.getUnificationEntry(itemStack);
        if (unificationEntry != null && unificationEntry.material != null) {
            chemicalFormula = unificationEntry.material.getChemicalFormula();

        // Test for Fluids
        } else if (ItemNBTUtils.hasTag(itemStack)) {

            // Vanilla bucket
            chemicalFormula = FluidTooltipUtil.getFluidTooltip(ItemNBTUtils.getString(itemStack, "FluidName"));

            // GTCE Cells, Forestry cans, some other containers
            if (chemicalFormula == null) {
                NBTTagCompound compound = itemStack.getTagCompound();
                if (compound != null && compound.hasKey(FluidHandlerItemStack.FLUID_NBT_KEY, Constants.NBT.TAG_COMPOUND)) {
                    chemicalFormula = FluidTooltipUtil.getFluidTooltip(FluidStack.loadFluidStackFromNBT(compound.getCompoundTag(FluidHandlerItemStack.FLUID_NBT_KEY)));
                }
            }

        // Water buckets have a separate registry name from other buckets
        } else if (itemStack.getItem().equals(Items.WATER_BUCKET)) {
            chemicalFormula = FluidTooltipUtil.getWaterTooltip();
        }
        if (chemicalFormula != null && !chemicalFormula.isEmpty()) {
            event.getToolTip().add(1, ChatFormatting.YELLOW.toString() + chemicalFormula);
        }
    }

    private static final String[] clearRecipes = new String[]{
            "quantum_tank",
            "quantum_chest",
            "super_chest",
            "super_tank",
            "drum.",
            "_tank",
            "fluid_cell"
    };

    @SubscribeEvent
    public static void addNBTClearingTooltip(ItemTooltipEvent event) {
        // Quantum Tank/Chest NBT Clearing Recipe Tooltip
        final EntityPlayer player = event.getEntityPlayer();
        if (player != null) {
            InventoryCrafting inv = null;
            InventoryCraftResult result = null;

            if (player.openContainer instanceof ContainerWorkbench) {
                inv = ((ContainerWorkbench) player.openContainer).craftMatrix;
                result = ((ContainerWorkbench) player.openContainer).craftResult;
            } else if (player.openContainer instanceof ContainerPlayer) {
                inv = ((ContainerPlayer) player.openContainer).craftMatrix;
                result = ((ContainerPlayer) player.openContainer).craftResult;
            }

            if (inv != null) {
                ItemStack stackResult = result.getStackInSlot(0);

                if (stackResult == event.getItemStack()) {
                    if (!stackResult.isEmpty() && ItemStack.areItemsEqual(stackResult, event.getItemStack())) {
                        String unlocalizedName = stackResult.getTranslationKey();
                        //noinspection ConstantConditions
                        String namespace = stackResult.getItem().getRegistryName().getNamespace();
                        for (String key : clearRecipes) {
                            if (unlocalizedName.contains(key) && namespace.equals(GTValues.MODID)) {

                                for (int i = 0; i < inv.getSizeInventory(); i++) {
                                    ItemStack craftStack = inv.getStackInSlot(i);
                                    if (!craftStack.isEmpty()) {
                                        if (!craftStack.isItemEqual(stackResult) || !craftStack.hasTagCompound())
                                            return;
                                    }
                                }
                                event.getToolTip().add(I18n.format("gregtech.universal.clear_nbt_recipe.tooltip"));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static final Set<UUID> capeHoldersUUIDs = new HashSet<>();

    private static void startCapeLoadingThread() {
        Thread capeListLoadThread = new Thread(ClientProxy::loadCapesList, "GregTech Cape List Downloader");
        capeListLoadThread.setDaemon(true);
        capeListLoadThread.start();
    }

    private static void loadCapesList() {
        capeHoldersUUIDs.add(UUID.fromString("4bdba267-1479-449a-8ae4-d1957dd39f29"));
        capeHoldersUUIDs.add(UUID.fromString("6cb05251-cd1b-481e-bf59-07637add1c64"));
        capeHoldersUUIDs.add(UUID.fromString("a82fb558-64f9-4dd6-a87d-84040e84bb43"));
        capeHoldersUUIDs.add(UUID.fromString("5c2933b3-5340-4356-81e7-783c53bd7845"));
        try {
            URL connectURL = new URL("https://www.dropbox.com/s/zc07k4y1h4ftmz3/GregTechPatreonList.txt?dl=1");
            HttpURLConnection connection = (HttpURLConnection) connectURL.openConnection(Minecraft.getMinecraft().getProxy());
            try {
                connection.setDoInput(true);
                connection.setDoOutput(false);
                connection.connect();
                InputStream inputStream = connection.getInputStream();
                capeHoldersUUIDs.addAll(retrieveCapeUUIDs(inputStream));
            } finally {
                connection.disconnect();
            }
        } catch (UnknownHostException |
                SocketTimeoutException |
                MalformedURLException ignored) {
        } catch (IOException exception) {
            GTLog.logger.warn("Failed to fetch cape list", exception);
        }
    }

    private static Set<UUID> retrieveCapeUUIDs(InputStream inputStream) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        Set<UUID> result = new HashSet<>();
        for (; ; ) {
            String line = reader.readLine();
            if (line == null)
                break;
            int firstCommentIndex = line.indexOf('#');
            if (firstCommentIndex > -1) {
                line = line.substring(0, firstCommentIndex);
            }
            try {
                UUID playerUUID = UUID.fromString(line.trim());
                result.add(playerUUID);
            } catch (IllegalArgumentException exception) {
                GTLog.logger.warn("Failed to parse cape player UUID {}", line.trim(), exception);
            }
        }
        return result;
    }

    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Pre event) {
        AbstractClientPlayer clientPlayer = (AbstractClientPlayer) event.getEntityPlayer();
        if (capeHoldersUUIDs.contains(clientPlayer.getUniqueID()) && clientPlayer.hasPlayerInfo() && clientPlayer.getLocationCape() == null) {
            NetworkPlayerInfo playerInfo = ObfuscationReflectionHelper.getPrivateValue(AbstractClientPlayer.class, clientPlayer, 0);
            Map<Type, ResourceLocation> playerTextures = ObfuscationReflectionHelper.getPrivateValue(NetworkPlayerInfo.class, playerInfo, 1);
            playerTextures.put(Type.CAPE, GREGTECH_CAPE_TEXTURE);
        }
    }

}
