package de.ermackov.pocketshulker.test;

import java.util.function.Predicate;

import de.ermackov.pocketshulker.client.PocketShulkerClient;
import de.ermackov.pocketshulker.common.PocketShulkerMenu;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;

public class PocketShulkerClientGameTest implements FabricClientGameTest {
	/** Menu slot of hotbar slot 0 in a shulker box menu: 27 box slots, then 27 inventory slots, then the hotbar. */
	private static final int HOTBAR_0 = 54;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			singleplayer.getServer().runCommand("gamemode survival @a");
			singleplayer.getServer().runCommand("effect give @a resistance infinite 255 true");
			singleplayer.getServer().runCommand("clear @a");
			singleplayer.getServer().runCommand("item replace entity @a hotbar.0 with blue_shulker_box[container=[{slot:0,item:{id:\"minecraft:diamond\",count:5}}]]");
			singleplayer.getServer().runCommand("item replace entity @a hotbar.3 with red_shulker_box[container=[{slot:0,item:{id:\"minecraft:gold_ingot\",count:7}}]]");
			singleplayer.getServer().runCommand("item replace entity @a hotbar.1 with stone 3");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
			context.waitTicks(5);

			// --- Shift + right-click on the ground must open the box, not place it
			singleplayer.getServer().runCommand("tp @a ~ ~ ~ 0 90");
			context.waitTicks(5);
			sneakUse(context);
			context.waitForScreen(ShulkerBoxScreen.class);
			assertServer(singleplayer, "server menu is a PocketShulkerMenu", p -> p.containerMenu instanceof PocketShulkerMenu);
			assertServer(singleplayer, "box was not placed", p -> !p.level().getBlockState(p.blockPosition().below()).is(Blocks.DYED_SHULKER_BOX.blue())
					&& !p.level().getBlockState(p.blockPosition()).is(Blocks.DYED_SHULKER_BOX.blue())
					&& p.getInventory().getItem(0).is(Items.DYED_SHULKER_BOX.blue()));
			boolean clientSeesDiamonds = context.computeOnClient(mc -> mc.player.containerMenu.slots.get(0).getItem().is(Items.DIAMOND));
			if (!clientSeesDiamonds) {
				throw new AssertionError("Client does not see the diamonds in the opened box");
			}
			context.takeScreenshot("pocket_shulker_open");

			// --- Taking items out is written to the item immediately, while the menu is still open
			click(singleplayer, 0, 0, ContainerInput.QUICK_MOVE);
			assertServer(singleplayer, "diamonds moved to the inventory and removed from the item", p ->
					p.getInventory().countItem(Items.DIAMOND) == 5 && contents(p.getInventory().getItem(0)).nonEmptyItemCopyStream().findAny().isEmpty());

			// --- Putting items in, too
			click(singleplayer, HOTBAR_0 + 1, 0, ContainerInput.QUICK_MOVE);
			assertServer(singleplayer, "stone moved into the item", p ->
					contents(p.getInventory().getItem(0)).nonEmptyItemCopyStream().anyMatch(s -> s.is(Items.STONE) && s.getCount() == 3));

			// --- The slot holding the open box is locked
			click(singleplayer, HOTBAR_0, 0, ContainerInput.PICKUP);
			click(singleplayer, HOTBAR_0, 1, ContainerInput.THROW);
			click(singleplayer, HOTBAR_0 + 3, 0, ContainerInput.SWAP); // number key 1 on the red box's slot
			click(singleplayer, 5, 0, ContainerInput.SWAP); // number key 1 on an empty box slot
			click(singleplayer, HOTBAR_0, 40, ContainerInput.SWAP); // off-hand key on the box
			assertServer(singleplayer, "locked box stays in its slot and the menu stays open", p ->
					p.getInventory().getItem(0).is(Items.DYED_SHULKER_BOX.blue()) && p.containerMenu.getCarried().isEmpty()
							&& p.getInventory().getItem(3).is(Items.DYED_SHULKER_BOX.red()) && p.containerMenu instanceof PocketShulkerMenu);
			// Shulker boxes still can't go into the box
			click(singleplayer, HOTBAR_0 + 3, 0, ContainerInput.QUICK_MOVE);
			assertServer(singleplayer, "red box stays out of the blue box", p -> p.getInventory().getItem(3).is(Items.DYED_SHULKER_BOX.red()));

			// --- If the box disappears from its slot, the menu closes
			singleplayer.getServer().runCommand("item replace entity @a hotbar.0 with air");
			context.waitTicks(3);
			assertServer(singleplayer, "menu closes when the box is gone", p -> p.containerMenu == p.inventoryMenu);
			context.waitTicks(5);

			// --- Put a blue box back; open the red one by shift + right-click into the air from the off hand
			singleplayer.getServer().runCommand("item replace entity @a hotbar.0 with blue_shulker_box");
			singleplayer.getServer().runCommand("item replace entity @a weapon.offhand from entity @a hotbar.3");
			singleplayer.getServer().runCommand("item replace entity @a hotbar.3 with air");
			singleplayer.getServer().runCommand("item replace entity @a hotbar.0 with air");
			singleplayer.getServer().runCommand("tp @a ~ ~ ~ 0 -90");
			context.waitTicks(5);
			sneakUse(context);
			context.waitForScreen(ShulkerBoxScreen.class);
			boolean clientSeesGold = context.computeOnClient(mc -> mc.player.containerMenu.slots.get(0).getItem().is(Items.GOLD_INGOT));
			if (!clientSeesGold) {
				throw new AssertionError("Off-hand box did not open with its gold");
			}
			context.runOnClient(mc -> mc.player.closeContainer());
			context.waitTicks(5);

			// --- Hotkey: with several boxes around, the last one opened by shift + right-click wins
			singleplayer.getServer().runCommand("item replace entity @a hotbar.0 with green_shulker_box[container=[{slot:0,item:{id:\"minecraft:emerald\",count:2}}]]");
			singleplayer.getServer().runCommand("item replace entity @a inventory.5 from entity @a weapon.offhand");
			singleplayer.getServer().runCommand("item replace entity @a weapon.offhand with air");
			context.waitTicks(5);
			context.getInput().holdKeyFor(options -> PocketShulkerClient.OPEN_KEY, 2);
			context.waitForScreen(ShulkerBoxScreen.class);
			boolean hotkeyOpenedRed = context.computeOnClient(mc -> mc.player.containerMenu.slots.get(0).getItem().is(Items.GOLD_INGOT));
			if (!hotkeyOpenedRed) {
				throw new AssertionError("Hotkey did not open the last opened (red) box");
			}
			context.takeScreenshot("pocket_shulker_hotkey");
			context.runOnClient(mc -> mc.player.closeContainer());
			context.waitTicks(5);

			// --- Without any box, the hotkey does nothing
			singleplayer.getServer().runCommand("clear @a");
			context.waitTicks(5);
			context.getInput().holdKeyFor(options -> PocketShulkerClient.OPEN_KEY, 2);
			context.waitTicks(10);
			assertServer(singleplayer, "no menu without a box", p -> p.containerMenu == p.inventoryMenu);
		}
	}

	private static void sneakUse(ClientGameTestContext context) {
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(3);
		context.getInput().holdKeyFor(options -> options.keyUse, 2);
		context.getInput().releaseKey(options -> options.keyShift);
	}

	private static ItemContainerContents contents(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
	}

	private static void click(TestSingleplayerContext singleplayer, int slot, int button, ContainerInput input) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.containerMenu.clicked(slot, button, input, player);
			player.containerMenu.broadcastChanges();
		});
	}

	private static void assertServer(TestSingleplayerContext singleplayer, String what, Predicate<ServerPlayer> check) {
		boolean ok = singleplayer.getServer().computeOnServer(server -> check.test(player(server)));
		if (!ok) {
			throw new AssertionError("Failed: " + what);
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
