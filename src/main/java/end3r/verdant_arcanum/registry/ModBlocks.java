package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.block.*;

import end3r.verdant_arcanum.block.RootgraspBloomBlock;
import end3r.verdant_arcanum.block.SolarBloomBloomBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlocks {
    // Grove Soil - special soil for magical plants
    public static final Block GROVE_SOIL = new GroveSoilBlock(
            FabricBlockSettings.of(Material.SOIL)
                    .strength(0.6f)
                    .sounds(BlockSoundGroup.GRAVEL)
                    .ticksRandomly()
    );

    // Magical Flowers
    public static final Block FLAME_FLOWER = new FlameFlowerBlock(
            FabricBlockSettings.of(Material.PLANT)
                    .noCollision()
                    .ticksRandomly()
                    .breakInstantly()
                    .sounds(BlockSoundGroup.CROP)
                    .luminance(state -> state.get(FlameFlowerBlock.AGE) > 0 ? 4 + state.get(FlameFlowerBlock.AGE) * 2 : 0)
    );

    public static final Block BLINK_FLOWER = new BlinkFlowerBlock(
            FabricBlockSettings.of(Material.PLANT)
                    .noCollision()
                    .ticksRandomly()
                    .breakInstantly()
                    .sounds(BlockSoundGroup.CROP)
                    .luminance(state -> state.get(BlinkFlowerBlock.AGE) > 0 ? 3 + state.get(BlinkFlowerBlock.AGE) : 0)
    );

    public static final Block ROOTGRASP_FLOWER = new RootgraspFlowerBlock(
            FabricBlockSettings.of(Material.PLANT)
                    .noCollision()
                    .ticksRandomly()
                    .breakInstantly()
                    .sounds(BlockSoundGroup.CROP)
                    .luminance(state -> state.get(RootgraspFlowerBlock.AGE) > 1 ? 2 : 0)
    );

    public static final Block GUST_FLOWER = new GustFlowerBlock(
            FabricBlockSettings.of(Material.PLANT)
                    .noCollision()
                    .ticksRandomly()
                    .breakInstantly()
                    .sounds(BlockSoundGroup.CROP)
                    .luminance(state -> state.get(GustFlowerBlock.AGE) == 2 ? 1 : 0)
    );

    // NEW: Placeable bloom blocks
    public static final Block FLAME_BLOOM = new FlameBloomBlock(
            FabricBlockSettings.of(Material.DECORATION)
                    .strength(0.2f)
                    .sounds(BlockSoundGroup.GRASS)
                    .luminance(6)
                    .ticksRandomly()
    );

    public static final Block BLINK_BLOOM = new BlinkBloomBlock(
            FabricBlockSettings.of(Material.DECORATION)
                    .strength(0.2f)
                    .sounds(BlockSoundGroup.GRASS)
                    .luminance(5)
                    .ticksRandomly()
    );

    public static final Block ROOTGRASP_BLOOM = new RootgraspBloomBlock(
            FabricBlockSettings.of(Material.DECORATION)
                    .strength(0.2f)
                    .sounds(BlockSoundGroup.GRASS)
                    .luminance(2)
                    .ticksRandomly()
    );

    public static final Block GUST_BLOOM = new GustBloomBlock(
            FabricBlockSettings.of(Material.DECORATION)
                    .strength(0.2f)
                    .sounds(BlockSoundGroup.GRASS)
                    .luminance(1)
                    .ticksRandomly()
    );

    public static final Block BREEZEVINE_BLOOM = new BreezevineBloomBlock(
            FabricBlockSettings.of(Material.DECORATION)
                    .strength(0.2f)
                    .sounds(BlockSoundGroup.GRASS)
                    .luminance(1)
                    .ticksRandomly()
    );

    public static final Block SOLARBLOOM_BLOOM = new SolarBloomBloomBlock(
            FabricBlockSettings.of(Material.DECORATION)
                    .strength(0.2f)
                    .sounds(BlockSoundGroup.GRASS)
                    .luminance(5)
                    .ticksRandomly()
    );


    public static final Block MAGIC_HIVE = new MagicHiveBlock(
            FabricBlockSettings.of(Material.WOOD)
                    .strength(3.0f, 6.0f)
                    .sounds(BlockSoundGroup.WOOD)
                    .nonOpaque()
    );

    public static final Block MAGIC_BEE_SPAWNER = new MagicBeeSpawnerBlock(
            FabricBlockSettings.of(Material.WOOD)
                    .strength(3.0f, 6.0f)
                    .sounds(BlockSoundGroup.WOOD)
                    .nonOpaque()
    );


    // This method will be called from ModRegistry
    public static void register() {
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "grove_soil"), GROVE_SOIL);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "flame_flower"), FLAME_FLOWER);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "blink_flower"), BLINK_FLOWER);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "rootgrasp_flower"), ROOTGRASP_FLOWER);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "gust_flower"), GUST_FLOWER);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "magic_hive"), MAGIC_HIVE);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "magic_bee_spawner"), MAGIC_BEE_SPAWNER);


        // Register new bloom blocks
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "flame_bloom"), FLAME_BLOOM);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "blink_bloom"), BLINK_BLOOM);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "rootgrasp_bloom"), ROOTGRASP_BLOOM);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "gust_bloom"), GUST_BLOOM);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "breezevine_bloom"), BREEZEVINE_BLOOM);
        Registry.register(Registry.BLOCK, new Identifier(VerdantArcanum.MOD_ID, "solarbloom_bloom"), SOLARBLOOM_BLOOM);

    }
}