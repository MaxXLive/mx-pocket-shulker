package de.ermackov.pocketshulker.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.ermackov.pocketshulker.client.PocketShulkerClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Connects to a running Paper server with PocketShulkerPaperBridge. Only runs when POCKETSHULKER_PAPER_DIR points
 * to the server directory, whose console reads commands from cmds.txt.
 */
public class PaperBridgeClientGameTest implements FabricClientGameTest {
	private static final String ADDRESS = "localhost:25599";
	private static final int HOTBAR_0 = 54;

	@Override
	public void runTest(ClientGameTestContext context) {
		String serverDir = System.getenv("POCKETSHULKER_PAPER_DIR");
		if (serverDir == null) {
			return;
		}
		Path commands = Path.of(serverDir, "cmds.txt");

		context.runOnClient(mc -> ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(ADDRESS),
			new ServerData("Paper", ADDRESS, ServerData.Type.OTHER), false, null));
		context.waitFor(mc -> mc.player != null && mc.level != null && mc.gui.screen() == null || mc.gui.screen() instanceof DisconnectedScreen, 20 * 120);
		if (context.computeOnClient(mc -> mc.gui.screen() instanceof DisconnectedScreen)) {
			throw new AssertionError("Paper server refused the connection");
		}
		context.waitTicks(60);

		command(commands, "gamemode survival @a");
		command(commands, "effect give @a resistance infinite 255 true");
		command(commands, "clear @a");
		command(commands, "item replace entity @a hotbar.0 with blue_shulker_box[container=[{slot:0,item:{id:\"minecraft:diamond\",count:5}}]]");
		command(commands, "item replace entity @a hotbar.3 with red_shulker_box[container=[{slot:0,item:{id:\"minecraft:gold_ingot\",count:7}}]]");
		command(commands, "item replace entity @a hotbar.1 with stone 3");
		command(commands, "execute as @a at @s run tp @s ~ ~ ~ 0 90");
		context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
		context.waitTicks(40);

		// Shift + right-click on the ground opens the box on Paper, it is not placed
		sneakUse(context);
		context.waitForScreen(ShulkerBoxScreen.class);
		context.waitTicks(10);
		if (!context.computeOnClient(mc -> mc.player.containerMenu.slots.get(0).getItem().is(Items.DIAMOND))) {
			throw new AssertionError("Client does not see the diamonds of the box opened on Paper");
		}
		context.takeScreenshot("paper_pocket_shulker_open");

		// Real click packets: diamonds out, stone in, then try to move the open box itself
		context.runOnClient(mc -> {
			int id = mc.player.containerMenu.containerId;
			mc.gameMode.handleContainerInput(id, 0, 0, ContainerInput.QUICK_MOVE, mc.player);
			mc.gameMode.handleContainerInput(id, HOTBAR_0 + 1, 0, ContainerInput.QUICK_MOVE, mc.player);
			mc.gameMode.handleContainerInput(id, HOTBAR_0, 0, ContainerInput.PICKUP, mc.player);
			mc.gameMode.handleContainerInput(id, HOTBAR_0, 1, ContainerInput.THROW, mc.player);
			mc.gameMode.handleContainerInput(id, HOTBAR_0 + 3, 0, ContainerInput.SWAP, mc.player);
		});
		context.waitTicks(20);
		context.runOnClient(mc -> mc.player.closeContainer());
		context.waitTicks(20);

		// Everything shown now comes from the server
		ItemStack box = context.computeOnClient(mc -> mc.player.getInventory().getItem(0).copy());
		ItemContainerContents contents = box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		int diamonds = context.computeOnClient(mc -> mc.player.getInventory().countItem(Items.DIAMOND));
		boolean redStill = context.computeOnClient(mc -> mc.player.getInventory().getItem(3).is(Items.DYED_SHULKER_BOX.red()));
		if (!box.is(Items.DYED_SHULKER_BOX.blue()) || !redStill) {
			throw new AssertionError("The open box was moved on Paper: slot 0 = " + box);
		}
		if (diamonds != 5 || contents.nonEmptyItemCopyStream().anyMatch(s -> s.is(Items.DIAMOND))
				|| contents.nonEmptyItemCopyStream().noneMatch(s -> s.is(Items.STONE) && s.getCount() == 3)) {
			throw new AssertionError("Unexpected contents after clicks on Paper: diamonds in inventory=" + diamonds + ", box=" + contents);
		}

		// Hotkey: the blue box was opened last, even though the red one is now selected
		context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(3));
		context.waitTicks(10);
		context.getInput().holdKeyFor(options -> PocketShulkerClient.OPEN_KEY, 2);
		context.waitForScreen(ShulkerBoxScreen.class);
		context.waitTicks(10);
		if (!context.computeOnClient(mc -> mc.player.containerMenu.slots.get(0).getItem().is(Items.STONE))) {
			throw new AssertionError("Hotkey on Paper did not open the last opened (blue) box");
		}
		context.takeScreenshot("paper_pocket_shulker_hotkey");

		context.runOnClient(mc -> mc.player.closeContainer());
		context.runOnClient(mc -> mc.disconnect(new TitleScreen(), false));
		context.waitForScreen(TitleScreen.class);
	}

	private static void sneakUse(ClientGameTestContext context) {
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(3);
		context.getInput().holdKeyFor(options -> options.keyUse, 2);
		context.getInput().releaseKey(options -> options.keyShift);
	}

	private static void command(Path commands, String command) {
		try {
			Files.writeString(commands, command + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
