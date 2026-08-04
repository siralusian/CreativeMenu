package com.creativemenu.network;

import com.creativemenu.CreativeMenu;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> Client: setzt die lokale (clientseitige) Kreativmenü-Konfiguration zurück. Die lokale
 * Konfiguration liegt als Datei im Client-Config-Ordner - nur der Client selbst kann sie löschen,
 * der Server hat keinen Zugriff darauf, deshalb der Umweg über ein Paket statt direktem Dateizugriff.
 */
public record ResetLocalConfigPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ResetLocalConfigPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
            CreativeMenu.MOD_ID, "reset_local_config"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, ResetLocalConfigPacket> CODEC =
        StreamCodec.unit(new ResetLocalConfigPacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResetLocalConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(com.creativemenu.client.tabs.ClientTabConfigManager::resetAll);
    }
}
