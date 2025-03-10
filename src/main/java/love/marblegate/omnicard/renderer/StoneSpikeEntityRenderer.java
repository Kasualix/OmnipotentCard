package love.marblegate.omnicard.renderer;

import love.marblegate.omnicard.entity.StoneSpikeEntity;
import love.marblegate.omnicard.model.StoneSpikeEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;

public class StoneSpikeEntityRenderer extends GeoProjectilesRenderer<StoneSpikeEntity> {
    public StoneSpikeEntityRenderer(EntityRendererManager renderManager) {
        super(renderManager, new StoneSpikeEntityModel());
    }
}
