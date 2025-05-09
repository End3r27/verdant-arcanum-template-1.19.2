package end3r.verdant_arcanum;

import end3r.verdant_arcanum.client.ClientEvents;
import end3r.verdant_arcanum.client.event.WindParticleHandler;
import end3r.verdant_arcanum.client.gui.MagicHiveScreen;
import end3r.verdant_arcanum.client.ui.ManaHudRenderer;
import end3r.verdant_arcanum.event.StrongWindsEvent;
import end3r.verdant_arcanum.magic.ManaSyncPacket;
import end3r.verdant_arcanum.registry.*;
import end3r.verdant_arcanum.screen.LivingStaffMk2Screen;
import end3r.verdant_arcanum.screen.LivingStaffMk2ScreenHandler;
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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;


@Environment(EnvType.CLIENT)
public class VerdantArcanumClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerdantArcanum.MOD_ID);

    private static final Identifier STAFF_SPELL_CHANGE_CHANNEL =
            new Identifier(VerdantArcanum.MOD_ID, "staff_spell_change");


    // Mouse scroll tracking
    private static boolean mouseScrolled = false;
    private static int scrollDirection = 0;

    // Store the original scroll callback
    private static GLFWScrollCallbackI originalScrollCallback;

    private static void processStaffScroll() {
        if (mouseScrolled) {
            // Get the current player
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client.player;

            if (player != null) {
                // Determine which hand has the staff
                ItemStack mainHandItem = player.getMainHandStack();
                ItemStack offHandItem = player.getOffHandStack();

                boolean hasStaff = false;
                boolean isMainHand = false;

                if (mainHandItem.getItem() instanceof LivingStaffItem) {
                    hasStaff = true;
                    isMainHand = true;
                } else if (offHandItem.getItem() instanceof LivingStaffItem) {
                    hasStaff = true;
                    isMainHand = false;
                }

                if (hasStaff) {
                    // Create packet with the hand and direction
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeBoolean(isMainHand); // true for main hand, false for offhand
                    buf.writeInt(scrollDirection);

                    // Send to server
                    ClientPlayNetworking.send(STAFF_SPELL_CHANGE_CHANNEL, buf);
                }
            }

            // Reset scroll state after processing
            mouseScrolled = false;
            scrollDirection = 0;
        }
    }




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
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLINK_FLOWER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GUST_FLOWER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ROOTGRASP_FLOWER, RenderLayer.getCutout());


        ManaSyncPacket.registerClient();

        ClientEvents.registerClientEvents();

        ClientEvents.registerInventoryChangeEvents();

        HandledScreens.register(ModScreenHandlers.MAGIC_HIVE_SCREEN_HANDLER, MagicHiveScreen::new);

        end3r.verdant_arcanum.network.BeamSyncPacket.registerClient();

        WindParticleHandler.init();

        ClientPlayNetworking.registerGlobalReceiver(StrongWindsEvent.WIND_PACKET_ID, (client, handler, buf, responseSender) -> {
            // Read wind data from packet
            double windX = buf.readDouble();
            double windY = buf.readDouble();
            double windZ = buf.readDouble();
            float strength = buf.readFloat();
            boolean active = buf.readBoolean();

            Vec3d windDirection = new Vec3d(windX, windY, windZ);

            // Apply on main thread
            client.execute(() -> {
                if (active) {
                    WindParticleHandler.setWindDirection(windDirection, strength);
                } else {
                    WindParticleHandler.setWindDirection(Vec3d.ZERO, 0);
                }
            });
        });


        ModEntities.registerRenderers();
        LOGGER.info("Entity renderers registered");

        // Add in onInitializeClient method
        ClientPlayNetworking.registerGlobalReceiver(VerdantArcanum.ENTITY_SPAWN_PACKET_ID,
                (client, handler, buf, responseSender) -> {
                    EntityType<?> entityType = Registry.ENTITY_TYPE.get(buf.readVarInt());
                    LOGGER.info("Received spawn packet for entity type: {}", entityType);

                    // Only proceed if this is our solar beam entity
                    if (entityType == ModEntities.SOLAR_BEAM) {
                        int entityId = buf.readVarInt();
                        UUID uuid = buf.readUuid();
                        double x = buf.readDouble();
                        double y = buf.readDouble();
                        double z = buf.readDouble();

                        client.execute(() -> {
                            // Code to handle spawn when needed
                            LOGGER.info("Processing spawn packet for SolarBeamEntity ID: {}", entityId);
                        });
                    }
                });







        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            processStaffScroll();
        });


        // Register the ManaHudRenderer
                ManaHudRenderer.register();

// Register the packet receiver on the client side
        ClientPlayNetworking.registerGlobalReceiver(STAFF_SPELL_CHANGE_CHANNEL,
                (client, handler, buf, responseSender) -> {
                    // Read the response data
                    int newActiveSlot = buf.readInt();
                    String spellName = buf.readString();

                    // Handle on main client thread
                    client.execute(() -> {
                        if (client.player != null) {
                            // Display a status message
                            client.player.sendMessage(Text.translatable("item.verdant_arcanum.living_staff.spell_changed", spellName), true);
                        }
                    });
                });


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            processStaffScroll();
        });




        // Register the screen handler type
                LivingStaffScreenHandler.register();

                LivingStaffMk2ScreenHandler.register();


        // Register the screen handler with the correct screen factory
                ScreenRegistry.register(LivingStaffScreenHandler.HANDLER_TYPE, LivingStaffScreen::new);


                ScreenRegistry.register(LivingStaffMk2ScreenHandler.HANDLER_TYPE, LivingStaffMk2Screen::new);

        // Ensure ManaParticleSystem is initialized
                ManaParticleSystem.getInstance();

                // Register client started event for mouse scroll setup
                ClientLifecycleEvents.CLIENT_STARTED.register(startedClient -> {
                    // Store the original callback
                    long window = MinecraftClient.getInstance().getWindow().getHandle();
                    originalScrollCallback = GLFW.glfwSetScrollCallback(window, null);

                    // Set our custom callback
                    GLFW.glfwSetScrollCallback(window, (windowHandle, xoffset, yoffset) -> {
                        // Check if player is sneaking and has a staff
                        boolean shouldHandleStaffScroll = false;

                        if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.isSneaking()) {
                            ItemStack mainHandItem = MinecraftClient.getInstance().player.getMainHandStack();
                            ItemStack offHandItem = MinecraftClient.getInstance().player.getOffHandStack();

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



                LOGGER.info("Verdant Arcanum Client initialization complete!");
    }

}
