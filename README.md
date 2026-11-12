<p align="center"><img src="docs/icon.png" alt="MX Pocket Shulker icon" width="128"></p>

<h1 align="center">MX Pocket Shulker</h1>

<p align="center">
  Open shulker boxes without placing them.<br>
  Sneak + right-click the box in your hand, or press <b>B</b> to open it from anywhere in your inventory.
</p>

---

## Features

- **Sneak + right-click** with a shulker box in your main hand or off hand opens it instead of placing it. Works when clicking on a block and when clicking into the air.
- **Hotkey (default `B`)** opens a shulker box from anywhere in your inventory. With several boxes, it opens the one you last opened with sneak + right-click. If you never did that, it takes the first one: main hand, off hand, hotbar, then the rest of the inventory. The key can be changed under *Options → Controls → MX Pocket Shulker*.
- The normal shulker box screen opens, with open and close sounds.
- Every change is saved to the item immediately, not only when the screen closes.
- The slot holding the open box is locked: it can't be picked up, dropped, swapped with number keys or the off-hand key. If the box leaves its slot in some other way (a command, another plugin), the screen closes.
- Shulker boxes still can't be put into shulker boxes.
- Players without the mod can still use sneak + right-click on a server with the mod or plugin. Only the hotkey needs the mod.

## Downloads

Get both jars from [GitHub Releases](../../releases). Pick the release whose version starts with your Minecraft version.

| File | What | Install on |
|---|---|---|
| `MXPocketShulker-fabric-<version>.jar` | Fabric mod | every player's client who wants the hotkey, and the server if it runs Fabric |
| `PocketShulkerPaperBridge-<version>.jar` | Paper plugin | the server if it runs Paper |

### Which server needs what

Only the server may change what's in an item, so the opening happens on the server side:

| Where you play | Server side |
|---|---|
| Singleplayer, LAN world you host | Nothing extra, the mod covers it |
| Fabric server | Install the mod on the server too |
| Paper server (or a fork like Purpur) | Install PocketShulkerPaperBridge |
| Vanilla or any other server | Not supported. Sneak + right-click places the box as usual, and the hotkey shows a message |

## Requirements

| | |
|---|---|
| Minecraft | 26.3 |
| Client | [Fabric Loader](https://fabricmc.net/) 0.19.0 or newer and [Fabric API](https://modrinth.com/mod/fabric-api) |
| Paper server | [Paper](https://papermc.io) 26.3 |
| Java | 25 |

## How it works

The server opens a vanilla `ShulkerBoxMenu` backed by a container that reads the item's `minecraft:container` component and writes it back on every change. The client gets an ordinary shulker box screen, so no client code is needed to display it.

- **Sneak + right-click:** the server intercepts the use of a shulker box item while sneaking (Fabric: `UseBlockCallback`/`UseItemCallback`, Paper: `PlayerInteractEvent`). With the mod installed, the client also skips predicting the block placement when the server supports the feature, so no ghost block shows up.
- **Hotkey:** the client sends the empty payload `mxpocketshulker:open`. The server picks the box. A box opened with sneak + right-click gets the time stored in its custom data (`mxpocketshulker:last_opened`), and the hotkey opens the box with the newest time.
- The client only sends the hotkey payload if the server registered the channel. Otherwise it shows a message.

The opening logic lives in `common/` and is compiled into both the mod and the plugin. Both use Mojang names, so the same source works for Fabric and Paper.

## Building

Building needs JDK 25.

```sh
./gradlew build
```

builds both parts. The jars end up in `client-fabric/build/libs/` and `server-paper/build/libs/`. `client-fabric` and `server-paper` are separate Gradle builds (Fabric Loom and paperweight don't work well in one build), and the root build just runs both. Local builds are versioned `<minecraft>-dev`.

## Tests

```sh
./gradlew -p client-fabric runClientGameTest
```

starts a game client and creates a world. It tests sneak + right-click on a block and in the air, saving while the screen is open, the locked slot, closing when the box disappears, and the hotkey choosing the last opened box. Screenshots end up in `client-fabric/build/run/clientGameTest/screenshots/`.

To also test against a Paper server, start a Paper server with the plugin on port 25599 in offline mode with the whitelist off. Pipe its console input from a file named `cmds.txt` in the server folder (`tail -f cmds.txt | java -jar paper.jar nogui`) and set `POCKETSHULKER_PAPER_DIR` to that folder before running the tests.

## Versioning and releases

Versions have the form **`<minecraft version>-<major>.<minor>.<patch>`**, for example `26.3-1.0.0`. Both jars of a release always have the same version. Pushes to `main` build, tag and release through [GitHub Actions](.github/workflows/build.yml); [`.github/scripts/release-info.sh`](.github/scripts/release-info.sh) picks the next version from Conventional Commits (`feat!`/`BREAKING CHANGE` → major, `feat` → minor, everything else → patch) and writes the changelog. Each release also uploads the Fabric mod to CurseForge.

## License

[MIT License](LICENSE)
