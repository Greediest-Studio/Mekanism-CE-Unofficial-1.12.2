package mekanism.multiblockmachine.client.render.block.machine.bloom;

import mekanism.common.util.BloomEffect;
import mekanism.multiblockmachine.client.model.machine.ModelLargeSolarNeutronActivator;
import mekanism.multiblockmachine.client.render.block.machine.RenderLargeSolarNeutronActivator;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeSolarNeutronActivator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BloomRenderLargeSolarNeutronActivator extends BloomEffect<TileEntityLargeSolarNeutronActivator> {

    private final TileEntityLargeSolarNeutronActivator tile;

    public BloomRenderLargeSolarNeutronActivator(TileEntityLargeSolarNeutronActivator tile) {
        super(tile, 0, 180, 90, 270);
        this.tile = tile;
    }

    @Override
    protected void RenderModelBloom() {
        RenderLargeSolarNeutronActivator renderer = (RenderLargeSolarNeutronActivator) TileEntityRendererDispatcher.instance.renderers.get(TileEntityLargeSolarNeutronActivator.class);
        ModelLargeSolarNeutronActivator model = renderer.getModel();
        model.renderBloom(0.0625F, tile.getActive(), Minecraft.getMinecraft().renderEngine);
    }
}

