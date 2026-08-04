package com.creativemenu.network;

import com.creativemenu.CreativeMenu;
import com.creativemenu.data.PrescriptionSet;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> Client: die für DIESEN Spieler geltenden (nach seinem OP-Level bereits gefilterten)
 * Server-Vorschriften - gesendet beim Login und erneut bei jeder Admin-Änderung. Transportiert
 * als einzelner JSON-String statt eigener StreamCodecs für die verschachtelte Struktur (siehe
 * {@link PrescriptionSet}).
 */
public record ServerPrescriptionSyncPacket(String json) implements CustomPacketPayload {

    private static final Gson GSON = new Gson();

    public static final CustomPacketPayload.Type<ServerPrescriptionSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreativeMenu.MOD_ID, "prescription_sync"));

    public static final StreamCodec<ByteBuf, ServerPrescriptionSyncPacket> CODEC =
        ByteBufCodecs.STRING_UTF8.map(ServerPrescriptionSyncPacket::new, ServerPrescriptionSyncPacket::json);

    public static ServerPrescriptionSyncPacket of(PrescriptionSet set) {
        return new ServerPrescriptionSyncPacket(GSON.toJson(set));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Kein @OnlyIn(Dist.CLIENT): siehe LivingDexPacket-Vorbild in CobbleCompanion - der Verweis auf
    // client-only Code wird auf dem Dedicated Server nie ausgeführt (S2C-Paket).
    public static void handle(ServerPrescriptionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            PrescriptionSet set = GSON.fromJson(packet.json(), PrescriptionSet.class);
            com.creativemenu.client.tabs.ServerPrescriptionCache.setFiltered(set);
        });
    }
}
