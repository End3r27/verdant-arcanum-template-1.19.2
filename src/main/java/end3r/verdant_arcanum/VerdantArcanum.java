// VerdantArcanum.java
package end3r.verdant_arcanum;

import end3r.verdant_arcanum.item.LivingStaffItem;
import end3r.verdant_arcanum.item.LivingStaffMk2Item;
import end3r.verdant_arcanum.registry.ModEntities;
import end3r.verdant_arcanum.registry.ModRegistry;
import end3r.verdant_arcanum.registry.SpellRegistry;
import end3r.verdant_arcanum.spell.Spell;
import end3r.verdant_arcanum.spell.tier2.SolarBloomSpell;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerdantArcanum implements ModInitializer {
	// Define mod ID constant
	public static final String MOD_ID = "verdant_arcanum";

	public static final Identifier ENTITY_SPAWN_PACKET_ID = new Identifier("verdant_arcanum", "entity_spawn");

	private static final Identifier STAFF_SPELL_CHANGE_CHANNEL =
			new Identifier(MOD_ID, "staff_spell_change");
	public static EntityType<?> SOLAR_BEAM_ENTITY_TYPE;

	// Add this helper method to your VerdantArcanum class
	private static String formatSpellName(String spellId) {
		if (spellId == null || spellId.isEmpty()) {
			return "None";
		}

		// Split by underscores if present and capitalize each word
		String[] parts;
		if (spellId.contains("_")) {
			parts = spellId.split("_");
		} else {
			parts = new String[]{spellId};
		}

		StringBuilder formattedName = new StringBuilder();
		for (String part : parts) {
			if (!part.isEmpty()) {
				formattedName.append(Character.toUpperCase(part.charAt(0)))
						.append(part.substring(1))
						.append(" ");
			}
		}

		return formattedName.toString().trim();
	}


	// Logger for mod
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Verdant Arcanum...");

		ModEntities.registerModEntities();


		// Register all mod components
		ModRegistry.registerAll();

		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

		ServerPlayNetworking.registerGlobalReceiver(STAFF_SPELL_CHANGE_CHANNEL,
				(server, player, handler, buf, responseSender) -> {
					// Read packet data
					boolean isMainHand = buf.readBoolean();
					int direction = buf.readInt();
					Hand hand = isMainHand ? Hand.MAIN_HAND : Hand.OFF_HAND;

					// Process on server thread
					server.execute(() -> {
						// Get the staff item
						ItemStack staffStack = player.getStackInHand(hand);

						// Determine which type of staff and handle appropriately
						if (staffStack.getItem() instanceof LivingStaffMk2Item) {
							// Use Mk2 specific handling
							LivingStaffMk2Item.handleScrollWheelMk2(player.world, player, staffStack, direction);
						} else if (staffStack.getItem() instanceof LivingStaffItem) {
							// Use standard staff handling
							LivingStaffItem.handleScrollWheel(player.world, player, staffStack, direction);
						}

						// Get updated information to send back to client
						NbtCompound nbt = staffStack.getOrCreateNbt();
						int activeSlot = nbt.getInt(LivingStaffItem.ACTIVE_SLOT_KEY);
						String spellId = nbt.getString(LivingStaffItem.SLOT_PREFIX + activeSlot);
						String spellName = "None";

						// Get the actual spell id if a spell exists
						if (!spellId.isEmpty()) {
							// Convert spellId to a more readable format
							spellName = formatSpellName(spellId);
						}

						// Send response to client
						PacketByteBuf responseBuf = PacketByteBufs.create();
						responseBuf.writeInt(activeSlot);
						responseBuf.writeString(spellName);
						ServerPlayNetworking.send(player, STAFF_SPELL_CHANGE_CHANNEL, responseBuf);
					});
				});



		LOGGER.info("Verdant Arcanum initialization complete!");
	}

	private void onServerTick(MinecraftServer server) {
		for (ServerWorld world : server.getWorlds()) {
			SolarBloomSpell.tickActiveSpells(world, null);
		}

	}

}