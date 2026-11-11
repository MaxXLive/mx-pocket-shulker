package de.ermackov.pocketshulker.common;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Opens shulker boxes straight from the player's inventory. Shared by the Fabric mod and the Paper plugin,
 * so both servers behave the same.
 *
 * <p>The GUI is the vanilla shulker box menu, so the client needs nothing special to show it. Every change
 * in the menu is written to the item right away (see {@link ShulkerItemContainer}), and the slot holding the
 * shulker box is locked while the menu is open (see {@link PocketShulkerMenu}).
 */
public final class PocketShulker {
	public static final String MOD_ID = "mxpocketshulker";
	/** Payload id of the hotkey request sent by the client. It carries no data. */
	public static final String OPEN_CHANNEL = MOD_ID + ":open";
	/** Key in the item's custom data that stores when it was last opened by shift + right-click (epoch millis). */
	static final String LAST_OPENED_KEY = MOD_ID + ":last_opened";

	private PocketShulker() {
	}

	/** True for a single shulker box item with no unrolled loot table. */
	public static boolean isOpenable(ItemStack stack) {
		return isShulkerBox(stack) && stack.getCount() == 1 && !stack.has(DataComponents.CONTAINER_LOOT);
	}

	public static boolean isShulkerBox(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
	}

	/** Inventory index of the item in the given hand. */
	public static int slotOf(Player player, InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
	}

	/**
	 * Shift + right-click with a shulker box in the hand. Remembers this box for the hotkey.
	 *
	 * @return whether the shulker box was opened, in which case the click must not do anything else (like placing it)
	 */
	public static boolean openFromHand(ServerPlayer player, InteractionHand hand) {
		int slot = slotOf(player, hand);
		ItemStack stack = player.getInventory().getItem(slot);
		if (!isOpenable(stack)) {
			return false;
		}
		if (player.containerMenu instanceof PocketShulkerMenu) {
			// The client sends a use packet for each hand; the first one already opened a box.
			return true;
		}
		if (!open(player, slot)) {
			return false;
		}
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(LAST_OPENED_KEY, System.currentTimeMillis()));
		return true;
	}

	/**
	 * Hotkey: opens the shulker box that was last opened by shift + right-click. If none of the boxes in the
	 * inventory was opened that way, the first one is taken: hand, off hand, hotbar, then the rest.
	 *
	 * @return whether a shulker box was opened
	 */
	public static boolean openFromInventory(ServerPlayer player) {
		int slot = findPreferredSlot(player.getInventory());
		return slot >= 0 && open(player, slot);
	}

	static int findPreferredSlot(Inventory inventory) {
		int best = -1;
		long bestOpened = Long.MIN_VALUE;
		for (int slot : searchOrder(inventory)) {
			ItemStack stack = inventory.getItem(slot);
			if (!isOpenable(stack)) {
				continue;
			}
			long opened = lastOpened(stack);
			// strictly greater, so among never-opened boxes the first in search order wins
			if (best < 0 || opened > bestOpened) {
				best = slot;
				bestOpened = opened;
			}
		}
		return best;
	}

	private static int[] searchOrder(Inventory inventory) {
		int selected = inventory.getSelectedSlot();
		int[] order = new int[Inventory.INVENTORY_SIZE + 1];
		int i = 0;
		order[i++] = selected;
		order[i++] = Inventory.SLOT_OFFHAND;
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			if (slot != selected) {
				order[i++] = slot;
			}
		}
		return order;
	}

	private static long lastOpened(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? Long.MIN_VALUE : data.copyTag().getLongOr(LAST_OPENED_KEY, Long.MIN_VALUE);
	}

	private static boolean open(ServerPlayer player, int slot) {
		ItemStack stack = player.getInventory().getItem(slot);
		if (player.isSpectator() || !player.isAlive() || !isOpenable(stack)) {
			return false;
		}
		ShulkerItemContainer container = new ShulkerItemContainer(player, slot, stack);
		boolean opened = player.openMenu(new SimpleMenuProvider(
				(containerId, inventory, $) -> new PocketShulkerMenu(containerId, inventory, container),
				stack.getHoverName())).isPresent();
		if (opened) {
			playSound(player, net.minecraft.sounds.SoundEvents.SHULKER_BOX_OPEN);
		}
		return opened;
	}

	static void playSound(Player player, SoundEvent sound) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
				0.5F, player.level().getRandom().nextFloat() * 0.1F + 0.9F);
	}
}
