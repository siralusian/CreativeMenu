package com.creativemenu.network;

import com.creativemenu.CreativeMenu;
import com.creativemenu.data.CreativeMenuAdminManager;
import com.creativemenu.data.CreativeMenuPermissionConfig;
import com.creativemenu.data.PrescriptionSet;
import com.creativemenu.data.ServerMenuConfigManager;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> Server: der Admin-Editor sendet den komplett bearbeiteten Vorschriften-Satz zum
 * Speichern. WICHTIG: der Absender wird hier serverseitig NOCHMAL gegen
 * {@link CreativeMenuAdminManager#isAdmin} geprüft - das Öffnen des Editors ist zwar schon per
 * Command-Berechtigung abgesichert, aber ein manipulierter Client könnte dieses Paket auch ohne
 * den Umweg über den Befehl verschicken.
 */
public record ServerPrescriptionSaveRequestPacket(String json) implements CustomPacketPayload {

    private static final Gson GSON = new Gson();

    public static final CustomPacketPayload.Type<ServerPrescriptionSaveRequestPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreativeMenu.MOD_ID, "prescription_save_request"));

    public static final StreamCodec<ByteBuf, ServerPrescriptionSaveRequestPacket> CODEC =
        ByteBufCodecs.STRING_UTF8.map(ServerPrescriptionSaveRequestPacket::new, ServerPrescriptionSaveRequestPacket::json);

    public static ServerPrescriptionSaveRequestPacket of(PrescriptionSet set) {
        return new ServerPrescriptionSaveRequestPacket(GSON.toJson(set));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerPrescriptionSaveRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!CreativeMenuAdminManager.isAdmin(player.getUUID())) {
                CreativeMenu.LOGGER.warn("[CreativeMenu] {} tried to save server prescriptions without admin permission - ignored.",
                    player.getName().getString());
                return;
            }

            PrescriptionSet set = GSON.fromJson(packet.json(), PrescriptionSet.class);
            ServerMenuConfigManager.replaceAll(set.customTabs, set.categories, set.orderByLevel);

            // Allen Online-Spielern (jeweils nach ihrem eigenen OP-Level gefiltert) die neuen
            // Vorschriften zuschicken, plus dem Admin selbst den vollen Datensatz zur Bestätigung.
            for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
                int opLevel = ServerMenuConfigManager.opLevelOf(online);
                PrescriptionSet filtered = ServerMenuConfigManager.filteredSetFor(opLevel);
                filtered.minOpLevelAddRemove = CreativeMenuPermissionConfig.minOpLevelAddRemove();
                filtered.minOpLevelShowHide = CreativeMenuPermissionConfig.minOpLevelShowHide();
                filtered.minOpLevelSort = CreativeMenuPermissionConfig.minOpLevelSort();
                PacketDistributor.sendToPlayer(online, ServerPrescriptionSyncPacket.of(filtered));
            }
            PacketDistributor.sendToPlayer(player, ServerPrescriptionAdminSyncPacket.of(ServerMenuConfigManager.fullSet()));
        });
    }
}
