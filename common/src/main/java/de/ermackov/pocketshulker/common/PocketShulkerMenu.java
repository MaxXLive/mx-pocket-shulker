package de.ermackov.pocketshulker.common;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Server-side shulker box menu for a shulker box item. The client opens it as a normal shulker box screen.
 *
 * <p>The slot holding the shulker box item is locked: it can't be picked up, swapped with number keys or the
 * off-hand key, dropped or dragged over. Otherwise the open box could be moved away (into a chest, onto the ground)
 * while its contents are still being edited here.
 */
public final class PocketShulkerMenu extends ShulkerBoxMenu {
	private final ShulkerItemContainer shulker;
	private final Inventory inventory;

	PocketShulkerMenu(int containerId, Inventory inventory, ShulkerItemContainer shulker) {
		super(containerId, inventory, shulker);
		this.shulker = shulker;
		this.inventory = inventory;
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
		if (touchesLockedSlot(slotIndex, buttonNum, input)) {
			// The client predicted the click; the next broadcast puts its view back in line with the server.
			return;
		}
		super.clicked(slotIndex, buttonNum, input, player);
	}

	private boolean touchesLockedSlot(int slotIndex, int buttonNum, ContainerInput input) {
		int locked = shulker.slot();
		// number keys and the off-hand key swap with inventory index buttonNum (0-8, or 40 for the off hand)
		if (input == ContainerInput.SWAP && buttonNum == locked) {
			return true;
		}
		// collecting all matching items could take the box itself if shulker boxes were made stackable
		if (input == ContainerInput.PICKUP_ALL && PocketShulker.isShulkerBox(getCarried())) {
			return true;
		}
		if (slotIndex >= 0 && slotIndex < slots.size()) {
			Slot slot = slots.get(slotIndex);
			return slot.container == inventory && slot.getContainerSlot() == locked;
		}
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		// Deliberately ignores Paper's checkReachable flag: the item check is what keeps this menu safe.
		return shulker.stillValid(player);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		PocketShulker.playSound(player, SoundEvents.SHULKER_BOX_CLOSE);
	}
}
