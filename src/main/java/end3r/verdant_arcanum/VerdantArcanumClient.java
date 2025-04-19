package end3r.verdant_arcanum;

import end3r.verdant_arcanum.client.ClientEvents;
import end3r.verdant_arcanum.client.gui.MagicHiveScreen;
import end3r.verdant_arcanum.client.ui.ManaHudRenderer;
import end3r.verdant_arcanum.entity.client.MagicInfusedBeeRenderer;
import end3r.verdant_arcanum.magic.ManaSyncPacket;
import end3r.verdant_arcanum.registry.*;
import end3r.verdant_arcanum.screen.LivingStaffScreen;
import end3r.verdant_arcanum.item.LivingStaffItem;
import end3r.verdant_arcanum.magic.ManaParticleSystem;
import end3r.verdant_arcanum.screen.LivingStaffScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class VerdantArcanumClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerdantArcanum.MOD_ID);

    // Mouse scroll tracking
    private static boolean mouseScrolled = false;
    private static int scrollDirection = 0;

    // Store the original scroll callback
    private static GLFWScrollCallbackI originalScrollCallback;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Verdant Arcanum Client...");

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLAME_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLINK_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ROOTGRASP_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GUST_BLOOM, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BREEZEVINE_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SOLARBLOOM_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLAMESPIRAL_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PHANTOMSTEP_BLOOM, RenderLayer.getCutout());


        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLAME_FLOWER, RenderLayer.getCutout());

        ManaSyncPacket.registerClient();

        ClientEvents.registerClientEvents();

        ClientEvents.registerInventoryChangeEvents();

        HandledScreens.register(ModScreenHandlers.MAGIC_HIVE_SCREEN_HANDLER, MagicHiveScreen::new);

        EntityRendererRegistry.register(ModEntities.MAGIC_INFUSED_BEE, MagicInfusedBeeRenderer::new);

                // Register the ManaHudRenderer
                ManaHudRenderer.register();

                ClientTickEvents.END_CLIENT_TICK.register(tickClient -> {
                    if (tickClient.player != null && tickClient.player.isSneaking() && mouseScrolled) {
                        ItemStack mainHandItem = tickClient.player.getMainHandStack();
                        ItemStack offHandItem = tickClient.player.getOffHandStack();

                        // Only handle if player has a staff
                        if (mainHandItem.getItem() instanceof LivingStaffItem ||
                                offHandItem.getItem() instanceof LivingStaffItem) {

                            // Send packet to server instead of handling locally
                            end3r.verdant_arcanum.network.StaffPacketHandler.sendScrollPacket(scrollDirection);
                        }

                        // Reset after handling (important to prevent repeated scrolling)
                        mouseScrolled = false;
                    }
                });

                // Register the screen handler type
                LivingStaffScreenHandler.register();

                // Register the screen handler with the correct screen factory
                ScreenRegistry.register(LivingStaffScreenHandler.HANDLER_TYPE, LivingStaffScreen::new);

                // Ensure ManaParticleSystem is initialized
                ManaParticleSystem.getInstance();

                // Get a reference to the MinecraftClient instance
                MinecraftClient client = MinecraftClient.getInstance();

                // Register client started event for mouse scroll setup
                ClientLifecycleEvents.CLIENT_STARTED.register(startedClient -> {
                    // Store the original callback
                    long window = client.getWindow().getHandle();
                    originalScrollCallback = GLFW.glfwSetScrollCallback(window, null);

                    // Set our custom callback
                    GLFW.glfwSetScrollCallback(window, (windowHandle, xoffset, yoffset) -> {
                        // Check if player is sneaking and has a staff
                        boolean shouldHandleStaffScroll = false;

                        if (client.player != null && client.player.isSneaking()) {
                            ItemStack mainHandItem = client.player.getMainHandStack();
                            ItemStack offHandItem = client.player.getOffHandStack();

                            if (mainHandItem.getItem() instanceof LivingStaffItem ||
                                    offHandItem.getItem() instanceof LivingStaffItem) {
                                shouldHandleStaffScroll = true;
                            }
                        }

                        // Only capture for staff if relevant
                        if (shouldHandleStaffScroll) {
                            mouseScrolled = true;
                            scrollDirection = yoffset > 0 ? 1 : -1;
                        } else if (originalScrollCallback != null) {
                            // Call the original callback for default behavior
                            originalScrollCallback.invoke(windowHandle, xoffset, yoffset);
                        }
                    });
                });

                // Register spell scroll handling in client tick
                ClientTickEvents.END_CLIENT_TICK.register(tickClient -> {
                    if (tickClient.player != null && tickClient.player.isSneaking() && mouseScrolled) {
                        ItemStack mainHandItem = tickClient.player.getMainHandStack();
                        ItemStack offHandItem = tickClient.player.getOffHandStack();

                        if (mainHandItem.getItem() instanceof LivingStaffItem) {
                            LivingStaffItem.handleScrollWheel(tickClient.world, tickClient.player, mainHandItem, scrollDirection);
                        } else if (offHandItem.getItem() instanceof LivingStaffItem) {
                            LivingStaffItem.handleScrollWheel(tickClient.world, tickClient.player, offHandItem, scrollDirection);
                        }

                        // Reset after handling
                        mouseScrolled = false;
                    }
                });

                LOGGER.info("Verdant Arcanum Client initialization complete!");
    }

}
