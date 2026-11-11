package de.ermackov.pocketshulker.common;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

/**
 * The 27 slots of a shulker box item in a player's inventory.
 *
 * <p>Every change is written back to the item immediately instead of when the menu closes. The item therefore
 * always matches what the menu shows, no matter how the menu ends (closing, death, disconnect, crash), and there
 * is no window in which the same items exist both in the menu and in a stale copy of the item.
 */
final class ShulkerItemContainer extends SimpleContainer {
	private final Player player;
	private final int slot;
	private final ItemStack stack;

	ShulkerItemContainer(Player player, int slot, ItemStack stack) {
		super(ShulkerBoxBlockEntity.CONTAINER_SIZE);
		this.player = player;
		this.slot = slot;
		this.stack = stack;
		stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(getItems());
	}

	/** Inventory index of the shulker box item; this slot is locked while the menu is open. */
	int slot() {
		return slot;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		// Written to this exact stack object, never to whatever lies in the slot now. If the stack was split or
		// emptied, nothing is written; the menu closes on the next validity check anyway.
		if (PocketShulker.isOpenable(stack)) {
			stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
		}
	}

	/** The menu stays usable only while the very same item still sits in its slot, alone and unchanged in kind. */
	@Override
	public boolean stillValid(Player player) {
		return player == this.player
				&& player.isAlive()
				&& player.getInventory().getItem(slot) == stack
				&& PocketShulker.isOpenable(stack);
	}
}
