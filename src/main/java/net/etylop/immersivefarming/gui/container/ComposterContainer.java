/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package net.etylop.immersivefarming.gui.container;

import blusunrize.immersiveengineering.api.energy.IMutableEnergyStorage;
import blusunrize.immersiveengineering.api.energy.MutableEnergyStorage;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcess;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu;
import blusunrize.immersiveengineering.common.gui.IESlot;
import blusunrize.immersiveengineering.common.gui.sync.GenericContainerData;
import blusunrize.immersiveengineering.common.gui.sync.GetterAndSetter;
import net.etylop.immersivefarming.block.multiblocks.composter.ComposterLogic;
import net.etylop.immersivefarming.block.multiblocks.composter.ComposterLogic.State;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class ComposterContainer extends IEContainerMenu implements IESlot.ICallbackContainer
{
	public static ComposterContainer makeServer(
			MenuType<?> type, int id, Inventory invPlayer, MultiblockMenuContext<State> ctx
	)
	{
		final State state = ctx.mbContext().getState();
		final GetterAndSetter<Float> progress = GetterAndSetter.getterOnly(() -> {
			final MultiblockProcess<?, ?> process = ComposterLogic.getActiveProcess(state);
			if(process==null)
				return -1f;
			final Level level = ctx.mbContext().getLevel().getRawLevel();
			return 1-(process.processTick/(float)process.getMaxTicks(level));
		});
		return new ComposterContainer(
				multiblockCtx(type, id, ctx), invPlayer, state.getInventory(), state.energy, state.tanks, progress
		);
	}

	public static ComposterContainer makeClient(MenuType<?> type, int id, Inventory invPlayer)
	{
		return new ComposterContainer(
				clientCtx(type, id),
				invPlayer,
				new ItemStackHandler(ComposterLogic.NUM_SLOTS),
				new MutableEnergyStorage(ComposterLogic.ENERGY_CAPACITY),
				new FluidTank[]{
						new FluidTank(ComposterLogic.TANK_VOLUME),
						new FluidTank(ComposterLogic.TANK_VOLUME),
						new FluidTank(ComposterLogic.TANK_VOLUME)
				},
				GetterAndSetter.standalone(-1f)
		);
	}

	public final IEnergyStorage energy;
	public final FluidTank[] tanks;
	// Negative when idle
	public final GetterAndSetter<Float> progress;

	private ComposterContainer(
			MenuContext ctx, Inventory inventoryPlayer, IItemHandler inv, IMutableEnergyStorage energy, FluidTank[] tanks,
			GetterAndSetter<Float> progress
	)
	{
		super(ctx);
		this.energy = energy;
		this.tanks = tanks;
		this.progress = progress;

		this.addSlot(new IESlot.ContainerCallback(this, inv, ComposterLogic.INPUT_SLOT, 22, 15));
		ownSlotCount = ComposterLogic.NUM_SLOTS;

		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				addSlot(new Slot(inventoryPlayer, j+i*9+9, 8+j*18, 86+i*18));
		for(int i = 0; i < 9; i++)
			addSlot(new Slot(inventoryPlayer, i, 8+i*18, 144));
		addGenericData(GenericContainerData.energy(energy));
		for(FluidTank tank : tanks)
			addGenericData(GenericContainerData.fluid(tank));
		addGenericData(GenericContainerData.float32(progress::get, progress::set));
	}

	@Override
	public boolean canInsert(ItemStack stack, int slotNumber, Slot slotObject)
	{
		return true;
	}

	@Override
	public boolean canTake(ItemStack stack, int slotNumber, Slot slotObject)
	{
		return true;
	}
}
