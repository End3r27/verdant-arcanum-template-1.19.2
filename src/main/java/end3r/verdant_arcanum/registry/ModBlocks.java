// ModBlocks.java
package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.block.FlameFlowerBlock;
import end3r.verdant_arcanum.block.GroveSoilBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import end3r.verdant_arcanum.VerdantArcanum;

public class ModBlocks {
    public static final Block GROVE_SOIL = new GroveSoilBlock(FabricBlockSettings
            .of(Material.SOIL)
            .strength(0.5f)
            .sounds(BlockSoundGroup.GRAVEL));

    public static final Block FLAME_FLOWER = new FlameFlowerBlock(FabricBlockSettings
            .of(Material.PLANT)
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.CROP)
            .luminance(state -> state.get(FlameFlowerBlock.AGE) == 2 ? 8 : 0)); // Light level 8 when fully grown

    // This method will be called from ModRegistry
    public static void register() {
        // Register blocks
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "grove_soil"), GROVE_SOIL);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "flame_flower"), FLAME_FLOWER);
    }
}