package de.ermackov.pocketshulker;

import de.ermackov.pocketshulker.common.PocketShulker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/**
 * Server side (dedicated server and singleplayer): shift + right-click with a shulker box opens it instead of
 * placing it, and the hotkey payload opens the last opened one from the inventory.
 */
public class PocketShulkerMod implements ModInitializer {
	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(OpenShulkerPayload.TYPE, OpenShulkerPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(OpenShulkerPayload.TYPE, (payload, context) -> PocketShulker.openFromInventory(context.player()));

		// Covers clicks on blocks (which would place the box) and clicks into the air.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> openFromHand(player, hand));
		UseItemCallback.EVENT.register((player, level, hand) -> openFromHand(player, hand));
	}

	private static InteractionResult openFromHand(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer && serverPlayer.isShiftKeyDown()
				&& PocketShulker.openFromHand(serverPlayer, hand)) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}
