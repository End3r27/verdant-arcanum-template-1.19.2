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



    public static final EntityType<MagicInfusedBee> MAGIC_INFUSED_BEE = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(VerdantArcanum.MOD_ID, "magic_infused_bee"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, MagicInfusedBee::new)
                    .dimensions(EntityDimensions.fixed(0.7F, 0.6F))
                    .trackRangeBlocks(8)
                    .build()
    );

    // Register the SolarBeamEntity
    public static final EntityType<SolarBeamEntity> SOLAR_BEAM = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(VerdantArcanum.MOD_ID, "solar_beam"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, SolarBeamEntity::new)
                    .dimensions(EntityDimensions.fixed(0.1F, 0.1F)) // Small hitbox
                    .trackRangeBlocks(128)
                    .trackedUpdateRate(1)
                    .build()
    );



    public static void registerModEntities() {
        VerdantArcanum.LOGGER.info("Registering Mod Entities for " + VerdantArcanum.MOD_ID);

        // Register attributes
        FabricDefaultAttributeRegistry.register(MAGIC_INFUSED_BEE, MagicInfusedBee.createMagicInfusedBeeAttributes());
    }

    public static void registerRenderers() {
        VerdantArcanum.LOGGER.info("Registering Entity Renderers. SOLAR_BEAM id: {}", SOLAR_BEAM.toString());
        EntityRendererRegistry.register(MAGIC_INFUSED_BEE, MagicInfusedBeeRenderer::new);
        EntityRendererRegistry.register(SOLAR_BEAM, context -> {
            VerdantArcanum.LOGGER.info("Creating SolarBeamEntityRenderer");
            return new SolarBeamEntityRenderer(context);
        });
        VerdantArcanum.LOGGER.info("Entity Renderers registered.");
    }
}