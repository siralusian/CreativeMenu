package com.creativemenu;

import com.creativemenu.data.CreativeMenuAdminManager;
import com.creativemenu.data.CreativeMenuPermissionConfig;
import com.creativemenu.data.PrescriptionSet;
import com.creativemenu.data.ServerMenuConfigManager;
import com.creativemenu.network.OpenEditorPacket;
import com.creativemenu.network.ResetLocalConfigPacket;
import com.creativemenu.network.ServerPrescriptionAdminSyncPacket;
import com.creativemenu.network.ServerPrescriptionSaveRequestPacket;
import com.creativemenu.network.ServerPrescriptionSyncPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(CreativeMenu.MOD_ID)
public class CreativeMenu {

    public static final String MOD_ID = "creativemenu";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreativeMenu(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        LOGGER.info("CreativeMenu loading...");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("CreativeMenu common setup complete.");
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID);
        registrar.optional()
            .playToClient(
                OpenEditorPacket.TYPE,
                OpenEditorPacket.CODEC,
                OpenEditorPacket::handle)
            .playToClient(
                ResetLocalConfigPacket.TYPE,
                ResetLocalConfigPacket.CODEC,
                ResetLocalConfigPacket::handle)
            .playToClient(
                ServerPrescriptionSyncPacket.TYPE,
                ServerPrescriptionSyncPacket.CODEC,
                ServerPrescriptionSyncPacket::handle)
            .playToClient(
                ServerPrescriptionAdminSyncPacket.TYPE,
                ServerPrescriptionAdminSyncPacket.CODEC,
                ServerPrescriptionAdminSyncPacket::handle)
            .playToServer(
                ServerPrescriptionSaveRequestPacket.TYPE,
                ServerPrescriptionSaveRequestPacket.CODEC,
                ServerPrescriptionSaveRequestPacket::handle);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        com.creativemenu.commands.CreativeMenuCommands.register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event) {
        CreativeMenuAdminManager.init(event.getServer());
        ServerMenuConfigManager.init(event.getServer());
        CreativeMenuPermissionConfig.init(event.getServer());
    }

    /** Jeder Spieler braucht die für sein OP-Level geltenden Vorschriften, nicht nur Admins/Ops. */
    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            int opLevel = ServerMenuConfigManager.opLevelOf(player);
            PrescriptionSet filtered = ServerMenuConfigManager.filteredSetFor(opLevel);
            filtered.minOpLevelAddRemove = CreativeMenuPermissionConfig.minOpLevelAddRemove();
            filtered.minOpLevelShowHide = CreativeMenuPermissionConfig.minOpLevelShowHide();
            filtered.minOpLevelSort = CreativeMenuPermissionConfig.minOpLevelSort();
            PacketDistributor.sendToPlayer(player, ServerPrescriptionSyncPacket.of(filtered));
        } catch (Throwable t) {
            LOGGER.error("[CreativeMenu] Failed to sync server prescriptions to " + player.getName().getString(), t);
        }
    }
}
