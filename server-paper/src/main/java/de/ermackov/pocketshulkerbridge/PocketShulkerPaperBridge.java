package de.ermackov.pocketshulkerbridge;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import de.ermackov.pocketshulker.common.PocketShulker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/**
 * Paper counterpart of the MX Pocket Shulker mod. Shift + right-click works for every player, with or without
 * the mod; the hotkey needs the mod, which sends an empty {@code mxpocketshulker:open} message.
 */
public final class PocketShulkerPaperBridge extends JavaPlugin implements Listener, PluginMessageListener {
	@Override
	public void onEnable() {
		// Registering the incoming channel also tells mod clients that this server supports the hotkey.
		getServer().getMessenger().registerIncomingPluginChannel(this, PocketShulker.OPEN_CHANNEL, this);
		getServer().getPluginManager().registerEvents(this, this);
		saveResource("mx-guide.yml", false);
		saveResource("mx-guide_de_DE.yml", false);
	}

	@Override
	public void onDisable() {
		getServer().getMessenger().unregisterIncomingPluginChannel(this);
		// Open boxes are saved on every change already; closing just takes the screens away cleanly.
		for (Player player : getServer().getOnlinePlayers()) {
			if (handle(player).containerMenu instanceof de.ermackov.pocketshulker.common.PocketShulkerMenu) {
				player.closeInventory();
			}
		}
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (PocketShulker.OPEN_CHANNEL.equals(channel)) {
			PocketShulker.openFromInventory(handle(player));
		}
	}

	/**
	 * LOWEST so that the box opens before anything reacts to the would-be block placement. Opening a box from your
	 * own inventory doesn't touch the world, so protection plugins have nothing to decide here.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent event) {
		if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
				|| !event.getPlayer().isSneaking()
				|| event.getHand() == null
				|| event.useItemInHand() == Event.Result.DENY) {
			return;
		}
		InteractionHand hand = event.getHand() == EquipmentSlot.OFF_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		if (PocketShulker.openFromHand(handle(event.getPlayer()), hand)) {
			event.setUseItemInHand(Event.Result.DENY);
			event.setUseInteractedBlock(Event.Result.DENY);
		}
	}

	private static ServerPlayer handle(Player player) {
		return ((CraftPlayer) player).getHandle();
	}
}
