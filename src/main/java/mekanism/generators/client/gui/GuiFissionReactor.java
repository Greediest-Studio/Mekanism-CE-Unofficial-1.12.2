package mekanism.generators.client.gui;

import mekanism.api.TileNetworkList;
import mekanism.api.gas.GasTank;
import mekanism.api.math.MathUtils;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.button.GuiDisableableButton;
import mekanism.client.gui.element.*;
import mekanism.client.gui.element.GuiRateBarHorizontal.IRateInfoHandler;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.FluidType;
import mekanism.common.Mekanism;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.container.ContainerNull;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import mekanism.generators.client.gui.element.GuiFissionReactorTab;
import mekanism.generators.client.gui.element.GuiFissionReactorTab.FissionReactorTab;
import mekanism.generators.common.content.fission.SynchronizedFissionData;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class GuiFissionReactor extends GuiMekanismTile<TileEntityFissionReactorCasing> {

    private GuiDisableableButton activateButton;
    private GuiDisableableButton scramButton;
    private final GasTank emptyTank = new GasTank(1);
    private final GuiGraph heatGraph;

    public GuiFissionReactor(InventoryPlayer inventory, TileEntityFissionReactorCasing tile) {
        super(tile, new ContainerNull(inventory.player, tile));
        xSize = 195;
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiFissionReactorTab(this, tileEntity, FissionReactorTab.STAT, resource));
        addGuiElement(new GuiFissionCoolantGauge(this, resource, 6, 13));
        addGuiElement(new GuiGasGauge(() -> tileEntity.structure != null ? tileEntity.structure.fuelTank : emptyTank, GuiGauge.Type.STANDARD, this, resource, 25, 13)
                .withColor(GuiGauge.TypeColor.RED));
        addGuiElement(new GuiFissionHeatedCoolantGauge(this, resource, 152, 13));
        addGuiElement(new GuiGasGauge(() -> tileEntity.structure != null ? tileEntity.structure.wasteTank : emptyTank, GuiGauge.Type.STANDARD, this, resource, 171, 13)
                .withColor(GuiGauge.TypeColor.YELLOW));
        addGuiElement(new GuiInnerScreen(this, resource, 45, 17, 105, 56, () -> {
            if (tileEntity.structure == null) {
                return Arrays.asList(
                        LangUtils.localize("gui.status") + ": " + LangUtils.localize("gui.incomplete")
                );
            }
            SynchronizedFissionData data = tileEntity.structure;
            String status = data.active ? LangUtils.localize("gui.on") : LangUtils.localize("gui.off");
            if (data.isForceDisabled()) {
                status = status + " (" + LangUtils.localize("fission.force_disabled") + ")";
            }
            String coolingMode = data.coolantTank.getFluidAmount() > 0 ? LangUtils.localize("fission.cooling.water")
                    : (data.gasCoolantTank.getStored() > 0 ? LangUtils.localize("fission.cooling.sodium") : LangUtils.localize("fission.cooling.idle"));
            return Arrays.asList(
                    LangUtils.localize("gui.status") + ": " + status,
                    LangUtils.localize("gui.mode") + ": " + coolingMode,
                    LangUtils.localize("gui.burnRate") + ": " + UnitDisplayUtils.roundDecimals(data.lastBurnRate) + " /t",
                    LangUtils.localize("gui.boilRate") + ": " + data.lastBoilRate + " /t",
                    LangUtils.localize("gui.temp") + ": " + MekanismUtils.getTemperatureDisplay(data.temperature, TemperatureUnit.KELVIN),
                    LangUtils.localize("gui.reactorDamage") + ": " + UnitDisplayUtils.roundDecimals(data.reactorDamage) + "%"
            );
        }).defaultFormat().clearFormat().padding(4).spacing(1).textScale(0.75F));
        addGuiElement(new GuiHeatInfo(() -> {
            if (tileEntity.structure == null) {
                return Collections.emptyList();
            }
            TemperatureUnit unit = TemperatureUnit.values()[MekanismConfig.current().general.tempUnit.val().ordinal()];
            String environment = UnitDisplayUtils.getDisplayShort(tileEntity.structure.lastEnvironmentLoss * unit.intervalSize, false, unit);
            return Collections.singletonList(LangUtils.localize("gui.dissipated") + ": " + environment + "/t");
        }, this, resource));
        addGuiElement(new GuiEnergyInfo(() -> Collections.singletonList(LangUtils.localize("gui.fissionReactor")), this, resource));
        addGuiElement(new GuiRateBarHorizontal(this, new IRateInfoHandler() {
            @Override
            public String getTooltip() {
                if (tileEntity.structure == null) {
                    return LangUtils.localize("gui.temp") + ": 0 K";
                }
                return LangUtils.localize("gui.temp") + ": " + MekanismUtils.getTemperatureDisplay(tileEntity.structure.temperature, TemperatureUnit.KELVIN);
            }

            @Override
            public double getLevel() {
                if (tileEntity.structure == null) {
                    return 0;
                }
                return Math.min(1, tileEntity.structure.temperature / SynchronizedFissionData.MAX_DAMAGE_TEMPERATURE);
            }
        }, resource, 105, 95));
        addGuiElement(heatGraph = new GuiGraph(this, resource, 5, 121, 185, 36,
                data -> LangUtils.localize("gui.temp") + ": " + data + " K"));
        heatGraph.setMinScale(1_600);
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(activateButton = new GuiDisableableButton(0, guiLeft + 6, guiTop + 75, 81, 16, LangUtils.localize("gui.activate")));
        buttonList.add(scramButton = new GuiDisableableButton(1, guiLeft + 89, guiTop + 75, 81, 16, LangUtils.localize("gui.scram")));
        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton guiButton) throws IOException {
        super.actionPerformed(guiButton);
        if (guiButton.id == activateButton.id || guiButton.id == scramButton.id) {
            Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(0)));
        }
    }

    private void updateButtons() {
        if (activateButton == null || scramButton == null) {
            return;
        }
        if (tileEntity.structure == null) {
            activateButton.enabled = false;
            scramButton.enabled = false;
            return;
        }
        activateButton.enabled = !tileEntity.structure.active && !tileEntity.structure.isForceDisabled();
        scramButton.enabled = tileEntity.structure.active;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        updateButtons();
        String title = LangUtils.localize("gui.fissionReactor");
        fontRenderer.drawString(title, (xSize / 2) - (fontRenderer.getStringWidth(title) / 2), 5, 0x404040);
        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        if (tileEntity.structure != null) {
            String tempText = LangUtils.localize("gui.temp") + ": " + MekanismUtils.getTemperatureDisplay(tileEntity.structure.temperature, TemperatureUnit.KELVIN);
            renderScaledText(tempText, 8, 99, 0x404040, 95);
            if (tileEntity.structure.isForceDisabled() && !tileEntity.structure.active && xAxis >= 6 && xAxis <= 87 && yAxis >= 75 && yAxis <= 91) {
                displayTooltip(LangUtils.localize("fission.force_disabled"), xAxis, yAxis);
            }
        }
        drawRect(173, 76, 182, 85, 0xFF2B2B2B);
        boolean active = tileEntity.structure != null && tileEntity.structure.active;
        drawRect(174, 77, 181, 84, active ? 0xFF3DFF7A : 0xFFB03232);
        if (xAxis >= 173 && xAxis <= 181 && yAxis >= 76 && yAxis <= 84) {
            String lightStatus = tileEntity.structure != null && tileEntity.structure.active ? LangUtils.localize("gui.on") : LangUtils.localize("gui.off");
            displayTooltip(LangUtils.localize("gui.status") + ": " + lightStatus, xAxis, yAxis);
        }
        fontRenderer.drawString(LangUtils.localize("gui.fissionHeatGraph"), 8, 113, 0x404040);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (tileEntity.structure == null) {
            heatGraph.addData((int) SynchronizedFissionData.BASE_TEMPERATURE);
        } else {
            heatGraph.addData(MathUtils.clampToInt(Math.round(tileEntity.structure.temperature)));
        }
    }

    private static class GuiFissionCoolantGauge extends GuiGauge<Void> {

        private final TileEntityFissionReactorCasing tile;

        public GuiFissionCoolantGauge(GuiFissionReactor gui, ResourceLocation def, int x, int y) {
            super(Type.STANDARD, gui, def, x, y);
            tile = gui.tileEntity;
        }

        @Override
        public int getScaledLevel() {
            if (tile.structure == null) {
                return 0;
            }
            SynchronizedFissionData data = tile.structure;
            if (data.gasCoolantTank.getStored() > 0 && data.coolantTank.getFluidAmount() == 0) {
                if (data.gasCoolantTank.getMaxGas() <= 0) {
                    return 0;
                }
                double scale = data.gasCoolantTank.getStored() / (double) data.gasCoolantTank.getMaxGas();
                return MathUtils.clampToInt(Math.round(scale * (height - 2)));
            }
            if (data.coolantTank.getFluidAmount() > 0) {
                if (data.coolantTank.getCapacity() <= 0) {
                    return 0;
                }
                double scale = data.coolantTank.getFluidAmount() / (double) data.coolantTank.getCapacity();
                return MathUtils.clampToInt(Math.round(scale * (height - 2)));
            }
            return 0;
        }

        @Override
        public TextureAtlasSprite getIcon() {
            if (tile.structure == null) {
                return null;
            }
            SynchronizedFissionData data = tile.structure;
            if (data.gasCoolantTank.getStored() > 0 && data.coolantTank.getFluidAmount() == 0 && data.gasCoolantTank.getGas() != null
                    && data.gasCoolantTank.getGas().getGas() != null) {
                return data.gasCoolantTank.getGas().getGas().getSprite();
            }
            FluidStack coolant = data.coolantTank.getFluid();
            return coolant == null ? null : MekanismRenderer.getFluidTexture(coolant, FluidType.STILL);
        }

        @Override
        public String getTooltipText() {
            if (tile.structure == null) {
                return LangUtils.localize("gui.empty");
            }
            SynchronizedFissionData data = tile.structure;
            if (data.gasCoolantTank.getStored() > 0 && data.coolantTank.getFluidAmount() == 0 && data.gasCoolantTank.getGas() != null) {
                return LangUtils.localize("gui.coolant") + ": " + data.gasCoolantTank.getGas().getGas().getLocalizedName() + " (" + data.gasCoolantTank.getStored() + " mB)";
            }
            if (data.coolantTank.getFluid() != null) {
                return LangUtils.localize("gui.coolant") + ": " + LangUtils.localizeFluidStack(data.coolantTank.getFluid()) + " (" + data.coolantTank.getFluidAmount() + " mB)";
            }
            return LangUtils.localize("gui.empty");
        }

        @Override
        public mekanism.api.transmitters.TransmissionType getTransmission() {
            return null;
        }

        @Override
        protected void applyRenderColor() {
            if (tile.structure == null) {
                return;
            }
            SynchronizedFissionData data = tile.structure;
            if (data.gasCoolantTank.getStored() > 0 && data.coolantTank.getFluidAmount() == 0) {
                MekanismRenderer.color(data.gasCoolantTank.getGas());
            } else {
                MekanismRenderer.color(data.coolantTank.getFluid());
            }
        }
    }

    private static class GuiFissionHeatedCoolantGauge extends GuiGauge<Void> {

        private final TileEntityFissionReactorCasing tile;

        public GuiFissionHeatedCoolantGauge(GuiFissionReactor gui, ResourceLocation def, int x, int y) {
            super(Type.STANDARD, gui, def, x, y);
            tile = gui.tileEntity;
        }

        @Override
        public int getScaledLevel() {
            if (tile.structure == null) {
                return 0;
            }
            SynchronizedFissionData data = tile.structure;
            if (data.heatedCoolantTank.getStored() > 0 && data.steamTank.getFluidAmount() == 0) {
                if (data.heatedCoolantTank.getMaxGas() <= 0) {
                    return 0;
                }
                double scale = data.heatedCoolantTank.getStored() / (double) data.heatedCoolantTank.getMaxGas();
                return MathUtils.clampToInt(Math.round(scale * (height - 2)));
            }
            if (data.steamTank.getFluidAmount() > 0) {
                if (data.steamTank.getCapacity() <= 0) {
                    return 0;
                }
                double scale = data.steamTank.getFluidAmount() / (double) data.steamTank.getCapacity();
                return MathUtils.clampToInt(Math.round(scale * (height - 2)));
            }
            return 0;
        }

        @Override
        public TextureAtlasSprite getIcon() {
            if (tile.structure == null) {
                return null;
            }
            SynchronizedFissionData data = tile.structure;
            if (data.heatedCoolantTank.getStored() > 0 && data.steamTank.getFluidAmount() == 0 && data.heatedCoolantTank.getGas() != null
                    && data.heatedCoolantTank.getGas().getGas() != null) {
                return data.heatedCoolantTank.getGas().getGas().getSprite();
            }
            FluidStack heatedCoolant = data.steamTank.getFluid();
            return heatedCoolant == null ? null : MekanismRenderer.getFluidTexture(heatedCoolant, FluidType.STILL);
        }

        @Override
        public String getTooltipText() {
            if (tile.structure == null) {
                return LangUtils.localize("gui.empty");
            }
            SynchronizedFissionData data = tile.structure;
            if (data.heatedCoolantTank.getStored() > 0 && data.steamTank.getFluidAmount() == 0 && data.heatedCoolantTank.getGas() != null) {
                return LangUtils.localize("gui.output") + ": " + data.heatedCoolantTank.getGas().getGas().getLocalizedName() + " (" + data.heatedCoolantTank.getStored() + " mB)";
            }
            if (data.steamTank.getFluid() != null) {
                return LangUtils.localize("gui.output") + ": " + LangUtils.localizeFluidStack(data.steamTank.getFluid()) + " (" + data.steamTank.getFluidAmount() + " mB)";
            }
            return LangUtils.localize("gui.empty");
        }

        @Override
        public mekanism.api.transmitters.TransmissionType getTransmission() {
            return null;
        }

        @Override
        protected void applyRenderColor() {
            if (tile.structure == null) {
                return;
            }
            SynchronizedFissionData data = tile.structure;
            if (data.heatedCoolantTank.getStored() > 0 && data.steamTank.getFluidAmount() == 0) {
                MekanismRenderer.color(data.heatedCoolantTank.getGas());
            } else {
                MekanismRenderer.color(data.steamTank.getFluid());
            }
        }
    }
}
