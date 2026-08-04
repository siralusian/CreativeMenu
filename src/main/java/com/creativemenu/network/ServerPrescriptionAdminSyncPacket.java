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
 * Server -> Client: der VOLLE, ungefilterte Satz an Server-Vorschriften für den Admin-Editor
 * (nur an bereits verifizierte Admins gesendet, siehe CreativeMenuCommands - anders als
 * {@link ServerPrescriptionSyncPacket}, das nach OP-Level gefiltert ist).
 */
public record ServerPrescriptionAdminSyncPacket(String json) implements CustomPacketPayload {

    private static final Gson GSON = new Gson();

    public static final CustomPacketPayload.Type<ServerPrescriptionAdminSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreativeMenu.MOD_ID, "prescription_admin_sync"));

    public static final StreamCodec<ByteBuf, ServerPrescriptionAdminSyncPacket> CODEC =
        ByteBufCodecs.STRING_UTF8.map(ServerPrescriptionAdminSyncPacket::new, ServerPrescriptionAdminSyncPacket::json);

    public static ServerPrescriptionAdminSyncPacket of(PrescriptionSet set) {
        return new ServerPrescriptionAdminSyncPacket(GSON.toJson(set));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerPrescriptionAdminSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            PrescriptionSet set = GSON.fromJson(packet.json(), PrescriptionSet.class);
            com.creativemenu.client.tabs.ServerPrescriptionCache.setAdminFull(set);
        });
    }
}
