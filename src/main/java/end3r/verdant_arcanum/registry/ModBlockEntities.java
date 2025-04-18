package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.block.entity.MagicBeeSpawnerBlockEntity;
import end3r.verdant_arcanum.block.entity.MagicHiveBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static end3r.verdant_arcanum.VerdantArcanum.MOD_ID;

public class ModBlockEntities {
    // Magic Hive block entity
    public static BlockEntityType<MagicHiveBlockEntity> MAGIC_HIVE_ENTITY;

    public static void register() {
        // Register the Magic Hive block entity
        MAGIC_HIVE_ENTITY = Registry.register(
                Registry.BLOCK_ENTITY_TYPE,
                new Identifier(MOD_ID, "magic_hive_entity"),
                FabricBlockEntityTypeBuilder.create(MagicHiveBlockEntity::new,
                        ModBlocks.MAGIC_HIVE
                ).build()
        );

    }
    public static final BlockEntityType<MagicBeeSpawnerBlockEntity> MAGIC_BEE_SPAWNER_ENTITY =
            Registry.register(
                    Registry.BLOCK_ENTITY_TYPE,
                    new Identifier(MOD_ID, "magic_bee_spawner"),
                    FabricBlockEntityTypeBuilder.create(
                            MagicBeeSpawnerBlockEntity::new,
                            ModBlocks.MAGIC_BEE_SPAWNER
                    ).build()
            );
}