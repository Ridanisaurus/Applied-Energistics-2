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

package appeng.tile.spatial;

import java.io.IOException;
import java.util.EnumSet;

import javax.annotation.Nonnull;

import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;

import appeng.api.networking.GridFlags;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.me.GridAccessException;
import appeng.me.cluster.IAEMultiBlock;
import appeng.me.cluster.implementations.SpatialPylonCalculator;
import appeng.me.cluster.implementations.SpatialPylonCluster;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.AENetworkProxyMultiblock;
import appeng.tile.grid.AENetworkTileEntity;

public class SpatialPylonTileEntity extends AENetworkTileEntity implements IAEMultiBlock<SpatialPylonCluster> {

    public static final ModelProperty<Integer> STATE = new ModelProperty<>(value -> {
        // The lower 6 bits are used
        return (value & ~0x3F) == 0;
    });

    public static final int DISPLAY_END_MIN = 0x01;
    public static final int DISPLAY_END_MAX = 0x02;
    public static final int DISPLAY_MIDDLE = 0x01 + 0x02;
    public static final int DISPLAY_X = 0x04;
    public static final int DISPLAY_Y = 0x08;
    public static final int DISPLAY_Z = 0x04 + 0x08;
    public static final int MB_STATUS = 0x01 + 0x02 + 0x04 + 0x08;

    public static final int DISPLAY_ENABLED = 0x10;
    public static final int DISPLAY_POWERED_ENABLED = 0x20;
    public static final int NET_STATUS = 0x10 + 0x20;

    private final SpatialPylonCalculator calc = new SpatialPylonCalculator(this);
    private int displayBits = 0;
    private SpatialPylonCluster cluster;
    private boolean didHaveLight = false;

    public SpatialPylonTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.MULTIBLOCK);
        this.getProxy().setIdlePowerUsage(0.5);
        this.getProxy().setValidSides(EnumSet.noneOf(Direction.class));
    }

    @Override
    protected AENetworkProxy createProxy() {
        return new AENetworkProxyMultiblock(this, "proxy", this.getItemFromTile(this), true);
    }

    @Override
    public void onChunkUnloaded() {
        this.disconnect(false);
        super.onChunkUnloaded();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.calc.calculateMultiblock(world, pos);
    }

    @Override
    public void remove() {
        this.disconnect(false);
        super.remove();
    }

    public void neighborChanged(BlockPos changedPos) {
        this.calc.updateMultiblockAfterNeighborUpdate(this.world, pos, changedPos);
    }

    @Override
    public void disconnect(final boolean b) {
        if (this.cluster != null) {
            this.cluster.destroy();
            this.updateStatus(null);
        }
    }

    @Override
    public SpatialPylonCluster getCluster() {
        return this.cluster;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public void updateStatus(final SpatialPylonCluster c) {
        this.cluster = c;
        this.getProxy().setValidSides(c == null ? EnumSet.noneOf(Direction.class) : EnumSet.allOf(Direction.class));
        this.recalculateDisplay();
    }

    public void recalculateDisplay() {
        final int oldBits = this.displayBits;

        this.displayBits = 0;

        if (this.cluster != null) {
            if (this.cluster.getBoundsMin().equals(this.pos)) {
                this.displayBits = DISPLAY_END_MIN;
            } else if (this.cluster.getBoundsMax().equals(this.pos)) {
                this.displayBits = DISPLAY_END_MAX;
            } else {
                this.displayBits = DISPLAY_MIDDLE;
            }

            switch (this.cluster.getCurrentAxis()) {
                case X:
                    this.displayBits |= DISPLAY_X;
                    break;
                case Y:
                    this.displayBits |= DISPLAY_Y;
                    break;
                case Z:
                    this.displayBits |= DISPLAY_Z;
                    break;
                default:
                    this.displayBits = 0;
                    break;
            }

            try {
                if (this.getProxy().getEnergy().isNetworkPowered()) {
                    this.displayBits |= DISPLAY_POWERED_ENABLED;
                }

                if (this.cluster.isValid() && this.getProxy().isActive()) {
                    this.displayBits |= DISPLAY_ENABLED;
                }
            } catch (final GridAccessException e) {
                // nothing?
            }
        }

        if (oldBits != this.displayBits) {
            this.markForUpdate();
        }
    }

    @Override
    public void markForUpdate() {
        super.markForUpdate();
        final boolean hasLight = this.getLightValue() > 0;
        if (hasLight != this.didHaveLight) {
            this.didHaveLight = hasLight;
            this.world.getLightManager().checkBlock(this.pos);
        }
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    public int getLightValue() {
        if ((this.displayBits & DISPLAY_POWERED_ENABLED) == DISPLAY_POWERED_ENABLED) {
            return 8;
        }
        return 0;
    }

    @Override
    protected boolean readFromStream(final PacketBuffer data) throws IOException {
        final boolean c = super.readFromStream(data);
        final int old = this.displayBits;
        this.displayBits = data.readByte();
        return old != this.displayBits || c;
    }

    @Override
    protected void writeToStream(final PacketBuffer data) throws IOException {
        super.writeToStream(data);
        data.writeByte(this.displayBits);
    }

    @MENetworkEventSubscribe
    public void powerRender(final MENetworkPowerStatusChange c) {
        this.recalculateDisplay();
    }

    @MENetworkEventSubscribe
    public void activeRender(final MENetworkChannelsChanged c) {
        this.recalculateDisplay();
    }

    public int getDisplayBits() {
        return this.displayBits;
    }

    @Nonnull
    @Override
    public IModelData getModelData() {
        // FIXME: Must force model data update on changes, should potentially be moved
        // to block state (?)
        return new ModelDataMap.Builder().withInitial(STATE, getDisplayBits()).build();
    }

}
