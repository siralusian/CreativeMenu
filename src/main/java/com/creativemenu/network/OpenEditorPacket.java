package com.creativemenu.network;

import com.creativemenu.CreativeMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> Client: Anweisung, das Bearbeitungsmenü zu öffnen (lokal oder serverseitig, je nach
 * {@code serverMode}). Kommandos laufen serverseitig (auch im Singleplayer über den integrierten
 * Server), das Öffnen eines Screens muss deshalb per Paket an genau den ausführenden Spieler-Client
 * zurückgeschickt werden.
 */
public record OpenEditorPacket(boolean serverMode) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenEditorPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
            CreativeMenu.MOD_ID, "open_editor"));

    public static final StreamCodec<ByteBuf, OpenEditorPacket> CODEC =
        ByteBufCodecs.BOOL.map(OpenEditorPacket::new, OpenEditorPacket::serverMode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Kein @OnlyIn(Dist.CLIENT): siehe ClientEditorHandler-Klassenkommentar - der Verweis auf
    // client-only Code hier wird auf dem Dedicated Server nie ausgeführt (S2C-Paket).
    public static void handle(OpenEditorPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.creativemenu.client.ClientEditorHandler.openEditor(packet.serverMode()));
    }
}
