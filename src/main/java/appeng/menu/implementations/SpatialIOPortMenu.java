/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.menu.implementations;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.blockentity.spatial.SpatialIOPortBlockEntity;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.OutputSlot;
import appeng.menu.slot.RestrictedInputSlot;

/**
 * @see appeng.client.gui.implementations.SpatialIOPortScreen
 */
public class SpatialIOPortMenu extends AEBaseMenu {

    public static final MenuType<SpatialIOPortMenu> TYPE = MenuTypeBuilder
            .create(SpatialIOPortMenu::new, SpatialIOPortBlockEntity.class)
            .build("spatialioport");

    @GuiSync(0)
    public long currentPower;
    @GuiSync(1)
    public long maxPower;
    @GuiSync(2)
    public long reqPower;
    @GuiSync(3)
    public long eff;
    private int delay = 40;

    @GuiSync(31)
    public int xSize;
    @GuiSync(32)
    public int ySize;
    @GuiSync(33)
    public int zSize;

    public SpatialIOPortMenu(int id, Inventory ip, SpatialIOPortBlockEntity spatialIOPort) {
        super(TYPE, id, ip, spatialIOPort);

        this.addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.SPATIAL_STORAGE_CELLS,
                spatialIOPort.getInternalInventory(), 0), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new OutputSlot(spatialIOPort.getInternalInventory(), 1,
                RestrictedInputSlot.PlacableItemType.SPATIAL_STORAGE_CELLS_NO_SHADOW.icon),
                SlotSemantics.MACHINE_OUTPUT);

        this.createPlayerInventorySlots(ip);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.delay++;

            var gridNode = ((SpatialIOPortBlockEntity) getBlockEntity()).getGridNode();
            var grid = gridNode != null ? gridNode.getGrid() : null;

            if (this.delay > 15 && grid != null) {
                this.delay = 0;

                var eg = grid.getEnergyService();
                var sc = grid.getSpatialService();
                this.setCurrentPower((long) (100.0 * eg.getStoredPower()));
                this.setMaxPower((long) (100.0 * eg.getMaxStoredPower()));
                this.setRequiredPower((long) (100.0 * sc.requiredPower()));
                this.setEfficency((long) (100.0f * sc.currentEfficiency()));

                var min = sc.getMin();
                var max = sc.getMax();

                if (min != null && max != null && sc.isValidRegion()) {
                    this.xSize = sc.getMax().getX() - sc.getMin().getX() - 1;
                    this.ySize = sc.getMax().getY() - sc.getMin().getY() - 1;
                    this.zSize = sc.getMax().getZ() - sc.getMin().getZ() - 1;
                } else {
                    this.xSize = 0;
                    this.ySize = 0;
                    this.zSize = 0;
                }
            }
        }

        super.broadcastChanges();
    }

    public long getCurrentPower() {
        return this.currentPower;
    }

    private void setCurrentPower(long currentPower) {
        this.currentPower = currentPower;
    }

    public long getMaxPower() {
        return this.maxPower;
    }

    private void setMaxPower(long maxPower) {
        this.maxPower = maxPower;
    }

    public long getRequiredPower() {
        return this.reqPower;
    }

    private void setRequiredPower(long reqPower) {
        this.reqPower = reqPower;
    }

    public long getEfficency() {
        return this.eff;
    }

    private void setEfficency(long eff) {
        this.eff = eff;
    }
}
