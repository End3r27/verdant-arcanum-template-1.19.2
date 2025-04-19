package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModTags {
    public static class Items {
        // Tag for all spell essences
        public static final TagKey<Item> SPELL_ESSENCES = TagKey.of(
                Registry.ITEM_KEY,
                new Identifier(VerdantArcanum.MOD_ID, "spell_essences")
        );
    }

    public static class Blocks {
        // Tag for all magical flowers in bloom
        public static final TagKey<Block> MAGIC_FLOWERS_IN_BLOOM = TagKey.of(
                Registry.BLOCK_KEY,
                new Identifier(VerdantArcanum.MOD_ID, "magic_flowers_in_bloom")
        );

        // Tags for specific flower types in bloom
        public static final TagKey<Block> FLAME_FLOWERS_IN_BLOOM = TagKey.of(
                Registry.BLOCK_KEY,
                new Identifier(VerdantArcanum.MOD_ID, "flame_flowers_in_bloom")
        );

        public static final TagKey<Block> BLINK_FLOWERS_IN_BLOOM = TagKey.of(
                Registry.BLOCK_KEY,
                new Identifier(VerdantArcanum.MOD_ID, "blink_flowers_in_bloom")
        );

        public static final TagKey<Block> ROOTGRASP_FLOWERS_IN_BLOOM = TagKey.of(
                Registry.BLOCK_KEY,
                new Identifier(VerdantArcanum.MOD_ID, "rootgrasp_flowers_in_bloom")
        );

        public static final TagKey<Block> GUST_FLOWERS_IN_BLOOM = TagKey.of(
                Registry.BLOCK_KEY,
                new Identifier(VerdantArcanum.MOD_ID, "gust_flowers_in_bloom")
        );
        public static final TagKey<Block> BREEZEVINE_FLOWERS_IN_BLOOM = TagKey.of(
                Registry.BLOCK_KEY,
                new Identifier(VerdantArcanum.MOD_ID, "breezevine_flowers_in_bloom")
        );
        public static final TagKey<Block> SOLARBLOOM_FLOWERS_IN_BLOOM = TagKey.of(
                Registry.BLOCK_KEY,
                new Identifier(VerdantArcanum.MOD_ID, "solarbloom_flowers_in_bloom")
        );
        public static final TagKey<Block> FLAMESPIRAL_FLOWERS_IN_BLOOM = TagKey.of(
                Registry.BLOCK_KEY,
                new Identifier(VerdantArcanum.MOD_ID, "flamespiral_flowers_in_bloom")
        );
        public static final TagKey<Block> PHANTOMSTEP_FLOWERS_IN_BLOOM = TagKey.of(
                Registry.BLOCK_KEY,
                new Identifier(VerdantArcanum.MOD_ID, "phantomstep_flowers_in_bloom")
        );
    }
}