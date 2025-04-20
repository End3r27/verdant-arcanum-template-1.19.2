package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.entity.MagicInfusedBee;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.entity.client.MagicInfusedBeeRenderer;
import end3r.verdant_arcanum.entity.client.SolarBeamEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModEntities {

    // Helper to get raw entity type ID
    public static int getRawId(EntityType<?> type) {
        return Registry.ENTITY_TYPE.getRawId(type);
    }


    public static final EntityType<MagicInfusedBee> MAGIC_INFUSED_BEE = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(VerdantArcanum.MOD_ID, "magic_infused_bee"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, MagicInfusedBee::new)
                    .dimensions(EntityDimensions.fixed(0.7F, 0.6F))
                    .trackRangeBlocks(8)
                    .build()
    );

    // Register the SolarBeamEntity
    public static final EntityType<end3r.verdant_arcanum.entity.SolarBeamEntity> SOLAR_BEAM_ENTITY = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(VerdantArcanum.MOD_ID, "solar_beam_entity"),
            FabricEntityTypeBuilder.<end3r.verdant_arcanum.entity.SolarBeamEntity>create(SpawnGroup.MISC, end3r.verdant_arcanum.entity.SolarBeamEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0f, 1.0f)) // Beam has a fixed size
                    .trackable(128, 10, true) // Set tracking distance and update interval
                    .build()
    );



    public static void registerModEntities() {
        VerdantArcanum.LOGGER.info("Registering Mod Entities for " + VerdantArcanum.MOD_ID);



        // Register attributes
        FabricDefaultAttributeRegistry.register(MAGIC_INFUSED_BEE, MagicInfusedBee.createMagicInfusedBeeAttributes());
    }

    public static void registerRenderers() {
        EntityRendererRegistry.register(MAGIC_INFUSED_BEE, MagicInfusedBeeRenderer::new);
        EntityRendererRegistry.register(SOLAR_BEAM_ENTITY, SolarBeamEntityRenderer::new);
        VerdantArcanum.LOGGER.info("Registering Entity Renderers.");

    }
}