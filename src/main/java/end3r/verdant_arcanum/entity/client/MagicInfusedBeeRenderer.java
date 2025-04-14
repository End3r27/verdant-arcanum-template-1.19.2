package end3r.verdant_arcanum.entity.client;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.entity.MagicInfusedBee;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BeeEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class MagicInfusedBeeRenderer extends MobEntityRenderer<MagicInfusedBee, BeeEntityModel<MagicInfusedBee>> {
    private static final Identifier TEXTURE = new Identifier(VerdantArcanum.MOD_ID, "textures/entity/magic_infused_bee.png");

    public MagicInfusedBeeRenderer(EntityRendererFactory.Context context) {
        super(context, new BeeEntityModel<>(context.getPart(EntityModelLayers.BEE)), 0.4f);
    }

    @Override
    public Identifier getTexture(MagicInfusedBee entity) {
        return TEXTURE;
    }
}