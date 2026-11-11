package de.ermackov.pocketshulker;

import de.ermackov.pocketshulker.common.PocketShulker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Sent by the client when the hotkey is pressed. The server picks the shulker box, so the payload is empty. */
public final class OpenShulkerPayload implements CustomPacketPayload {
	public static final OpenShulkerPayload INSTANCE = new OpenShulkerPayload();
	public static final CustomPacketPayload.Type<OpenShulkerPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.parse(PocketShulker.OPEN_CHANNEL));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenShulkerPayload> CODEC = StreamCodec.unit(INSTANCE);

	private OpenShulkerPayload() {
	}

	@Override
	public CustomPacketPayload.Type<OpenShulkerPayload> type() {
		return TYPE;
	}
}
