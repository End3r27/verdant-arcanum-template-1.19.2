package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.block.entity.MagicHiveBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {
    // Magic Hive block entity
    public static BlockEntityType<MagicHiveBlockEntity> MAGIC_HIVE_ENTITY;

    public static void register() {
        // Register the Magic Hive block entity
        MAGIC_HIVE_ENTITY = Registry.register(
                Registry.BLOCK_ENTITY_TYPE,
                new Identifier(VerdantArcanum.MOD_ID, "magic_hive_entity"),
                FabricBlockEntityTypeBuilder.create(MagicHiveBlockEntity::new,
                        ModBlocks.MAGIC_HIVE
                ).build()
        );
    }
}