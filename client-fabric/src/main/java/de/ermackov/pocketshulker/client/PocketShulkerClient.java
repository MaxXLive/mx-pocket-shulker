package de.ermackov.pocketshulker.client;

import de.ermackov.pocketshulker.OpenShulkerPayload;
import de.ermackov.pocketshulker.common.PocketShulker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class PocketShulkerClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(PocketShulker.MOD_ID, "main"));
	public static final KeyMapping OPEN_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.mxpocketshulker.open", InputConstants.KEY_B, CATEGORY));

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(PocketShulkerClient::onTick);

		// Shift + right-click: if the server can open the box, don't predict placing it, the server opens the screen.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> predictOpen(player, hand));
		UseItemCallback.EVENT.register((player, level, hand) -> predictOpen(player, hand));
	}

	private static boolean serverSupported() {
		return ClientPlayNetworking.canSend(OpenShulkerPayload.TYPE);
	}

	private static InteractionResult predictOpen(Player player, InteractionHand hand) {
		if (player instanceof LocalPlayer && player.isShiftKeyDown()
				&& PocketShulker.isOpenable(player.getItemInHand(hand)) && serverSupported()) {
			// SUCCESS still sends the use packet to the server
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private static void onTick(Minecraft minecraft) {
		while (OPEN_KEY.consumeClick()) {
			LocalPlayer player = minecraft.player;
			if (player == null || minecraft.gui.screen() != null) {
				continue;
			}
			if (!serverSupported()) {
				player.sendOverlayMessage(Component.translatable("message.mxpocketshulker.unsupported"));
			} else if (!hasShulkerBox(player.getInventory())) {
				player.sendOverlayMessage(Component.translatable("message.mxpocketshulker.none"));
			} else {
				ClientPlayNetworking.send(OpenShulkerPayload.INSTANCE);
			}
		}
	}

	private static boolean hasShulkerBox(Inventory inventory) {
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			if (PocketShulker.isOpenable(inventory.getItem(slot))) {
				return true;
			}
		}
		return PocketShulker.isOpenable(inventory.getItem(Inventory.SLOT_OFFHAND));
	}
}
