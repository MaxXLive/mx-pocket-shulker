package de.ermackov.pocketshulker.test;

import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;

/**
 * Records frames for the animated picture on the mod page; not a test. Only runs when the environment variable
 * MX_SHOWCASE is set, e.g. {@code MX_SHOWCASE=1 ./gradlew -p client-fabric runClientGameTest}. Frames end up in
 * {@code client-fabric/build/run/clientGameTest/showcase/<scene>/}.
 */
public class PocketShulkerShowcaseGameTest implements FabricClientGameTest {
	private static final String BOX = "purple_shulker_box[container=["
			+ "{slot:0,item:{id:\"minecraft:diamond\",count:24}},"
			+ "{slot:1,item:{id:\"minecraft:emerald\",count:17}},"
			+ "{slot:2,item:{id:\"minecraft:gold_ingot\",count:40}},"
			+ "{slot:3,item:{id:\"minecraft:iron_ingot\",count:64}},"
			+ "{slot:4,item:{id:\"minecraft:redstone\",count:52}},"
			+ "{slot:5,item:{id:\"minecraft:lapis_lazuli\",count:33}},"
			+ "{slot:9,item:{id:\"minecraft:golden_apple\",count:6}},"
			+ "{slot:10,item:{id:\"minecraft:ender_pearl\",count:12}},"
			+ "{slot:11,item:{id:\"minecraft:cooked_beef\",count:48}},"
			+ "{slot:12,item:{id:\"minecraft:torch\",count:64}},"
			+ "{slot:18,item:{id:\"minecraft:oak_log\",count:64}},"
			+ "{slot:19,item:{id:\"minecraft:cobblestone\",count:64}},"
			+ "{slot:20,item:{id:\"minecraft:glass\",count:32}}"
			+ "]]";

	private int frame;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("MX_SHOWCASE") == null) {
			return;
		}
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamemode survival @a");
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("difficulty peaceful");
			server.runCommand("effect give @a resistance infinite 255 true");
			server.runCommand("effect give @a saturation infinite 255 true");
			server.runCommand("clear @a");
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.options.bobView().set(false);
			});

			recordSneakOpen(context, server);
		}
	}

	private void recordSneakOpen(ClientGameTestContext context, TestServerContext server) {
		BlockPos feet = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().blockPosition());
		int x = feet.getX();
		int y = feet.getY();
		int z = feet.getZ();
		// a clear stage: grass floor with air above
		server.runCommand(String.format("fill %d %d %d %d %d %d air", x - 6, y, z - 6, x + 6, y + 5, z + 6));
		server.runCommand(String.format("fill %d %d %d %d %d %d grass_block", x - 6, y - 1, z - 6, x + 6, y - 1, z + 6));
		server.runCommand(String.format("tp @a %d %d %d -90 25", x, y, z));
		server.runCommand("item replace entity @a hotbar.0 with " + BOX);
		server.runCommand("item replace entity @a hotbar.1 with diamond_pickaxe");
		server.runCommand("item replace entity @a hotbar.2 with diamond_sword");
		server.runCommand("item replace entity @a hotbar.3 with bread 16");
		context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
		context.waitTicks(30);

		Path dir = showcaseDir("sneak_open");
		TestInput input = context.getInput();
		capture(context, dir, 8);

		// sneak, then right-click the ground: the box opens instead of being placed
		input.holdKey(options -> options.keyShift);
		capture(context, dir, 5);
		input.holdKeyFor(options -> options.keyUse, 2);
		context.waitForScreen(ShulkerBoxScreen.class);
		input.releaseKey(options -> options.keyShift);
		capture(context, dir, 6);

		// hover over the diamonds to show the tooltip, then shift-click them into the inventory
		moveCursorToSlot(context, 0);
		capture(context, dir, 8);
		// the click itself goes straight to the server menu; simulated shift + mouse clicks don't reach the screen reliably
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.containerMenu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
			player.containerMenu.broadcastChanges();
		});
		capture(context, dir, 4);
		moveCursorToSlot(context, 1);
		capture(context, dir, 5);

		// close the screen: the box is still in the hand, nothing was placed
		input.pressKey(options -> options.keyInventory);
		context.runOnClient(mc -> {
			if (mc.gui.screen() != null) {
				mc.player.closeContainer();
			}
		});
		capture(context, dir, 12);
	}

	/** Moves the cursor to the middle of a shulker box slot (0-26) of the open screen. */
	private void moveCursorToSlot(ClientGameTestContext context, int slot) {
		double[] pos = context.computeOnClient(mc -> {
			double scale = mc.getWindow().getGuiScale();
			int left = (mc.getWindow().getGuiScaledWidth() - 176) / 2;
			int top = (mc.getWindow().getGuiScaledHeight() - 166) / 2;
			double gx = left + 8 + (slot % 9) * 18 + 8;
			double gy = top + 18 + (slot / 9) * 18 + 8;
			return new double[] {gx * scale, gy * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
	}

	private Path showcaseDir(String scene) {
		frame = 0;
		return FabricLoader.getInstance().getGameDir().resolve("showcase").resolve(scene);
	}

	private void capture(ClientGameTestContext context, Path dir, int frames) {
		for (int i = 0; i < frames; i++) {
			context.waitTicks(2);
			captureFrame(context, dir);
		}
	}

	private void captureFrame(ClientGameTestContext context, Path dir) {
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.takeScreenshot(TestScreenshotOptions.of(String.format("frame_%04d", frame++)).disableCounterPrefix().withDestinationDir(dir));
	}
}
