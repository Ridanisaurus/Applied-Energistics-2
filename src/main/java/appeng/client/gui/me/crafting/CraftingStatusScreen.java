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

package appeng.client.gui.me.crafting;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingStatusMenu;

/**
 * This specialized version of the {@link CraftingCPUScreen} allows a player to cycle through the CPUs that are
 * currently working on crafting jobs, and see their crafting status.
 */
public class CraftingStatusScreen extends CraftingCPUScreen<CraftingStatusMenu> {

    public CraftingStatusScreen(CraftingStatusMenu menu,
            Inventory playerInventory,
            Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        AESubScreen.addBackButton(menu, "back", widgets);

        var scrollbar = widgets.addScrollBar("selectCpuScrollbar", Scrollbar.BIG);
        widgets.add("selectCpuList", new CPUSelectionList(menu, scrollbar, style));
    }

    @Override
    protected Component getGuiDisplayName(Component in) {
        return in; // the cpu name is on the button
    }

}
