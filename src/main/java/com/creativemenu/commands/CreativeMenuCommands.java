package com.creativemenu.commands;

import com.creativemenu.data.CreativeMenuAdminManager;
import com.creativemenu.data.CreativeMenuPermissionConfig;
import com.creativemenu.data.ServerMenuConfigManager;
import com.creativemenu.network.OpenEditorPacket;
import com.creativemenu.network.ResetLocalConfigPacket;
import com.creativemenu.network.ServerPrescriptionAdminSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class CreativeMenuCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> main = dispatcher.register(
            Commands.literal("creativemenu")
                .then(Commands.literal("admin")
                    .requires(CreativeMenuAdminManager::canGrant)
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            grantAdmin(ctx.getSource(), StringArgumentType.getString(ctx, "name"));
                            return 1;
                        })))
                .then(Commands.literal("open")
                    .requires(source -> source.getPlayer() != null)
                    .executes(ctx -> {
                        openEditor(ctx.getSource(), false);
                        return 1;
                    }))
                .then(Commands.literal("server")
                    .requires(source -> source.getPlayer() != null
                        && CreativeMenuAdminManager.isAdmin(source.getPlayer().getUUID()))
                    .executes(ctx -> {
                        openServerEditor(ctx.getSource());
                        return 1;
                    })
                    .then(Commands.literal("resett")
                        .then(Commands.literal("true")
                            .executes(ctx -> {
                                resetServer(ctx.getSource());
                                return 1;
                            }))))
                .then(Commands.literal("resett")
                    .requires(source -> source.getPlayer() != null)
                    .then(Commands.literal("true")
                        .executes(ctx -> {
                            resetLocal(ctx.getSource());
                            return 1;
                        })))
                .then(Commands.literal("permissions")
                    .requires(source -> source.getPlayer() != null
                        && CreativeMenuAdminManager.isAdmin(source.getPlayer().getUUID()))
                    .then(Commands.literal("get")
                        .executes(ctx -> {
                            showPermissions(ctx.getSource());
                            return 1;
                        }))
                    .then(Commands.literal("set")
                        .then(permissionSetNode("addremove", CreativeMenuPermissionConfig::setAddRemove))
                        .then(permissionSetNode("showhide", CreativeMenuPermissionConfig::setShowHide))
                        .then(permissionSetNode("sort", CreativeMenuPermissionConfig::setSort))))
        );

        dispatcher.register(Commands.literal("crm").redirect(main));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> permissionSetNode(
            String name, java.util.function.IntConsumer setter) {
        return Commands.literal(name)
            .then(Commands.argument("level", IntegerArgumentType.integer(0, 4))
                .executes(ctx -> {
                    setter.accept(IntegerArgumentType.getInteger(ctx, "level"));
                    showPermissions(ctx.getSource());
                    return 1;
                }));
    }

    private static void showPermissions(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(String.format(
            "§aCreativeMenu Berechtigungsschwellen: Hinzufügen/Entfernen=%d, Ein-/Ausblenden=%d, Sortieren=%d",
            CreativeMenuPermissionConfig.minOpLevelAddRemove(),
            CreativeMenuPermissionConfig.minOpLevelShowHide(),
            CreativeMenuPermissionConfig.minOpLevelSort())), true);
    }

    private static void grantAdmin(CommandSourceStack source, String targetName) {
        if (source.getServer() == null) return;
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            source.sendFailure(Component.literal("§cPlayer '§f" + targetName + "§c' not found (must be online)."));
            return;
        }
        CreativeMenuAdminManager.grant(target.getUUID());
        source.sendSuccess(() -> Component.literal("§a" + targetName + " can now edit the server-prescribed CreativeMenu settings."), true);
    }

    private static void openEditor(CommandSourceStack source, boolean serverMode) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new OpenEditorPacket(serverMode));
    }

    /** Wie openEditor(true), schickt aber zusätzlich den vollen (ungefilterten) Vorschriften-Satz für den Admin-Editor. */
    private static void openServerEditor(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, ServerPrescriptionAdminSyncPacket.of(ServerMenuConfigManager.fullSet()));
        PacketDistributor.sendToPlayer(player, new OpenEditorPacket(true));
    }

    private static void resetLocal(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new ResetLocalConfigPacket());
        source.sendSuccess(() -> Component.literal("§aYour local CreativeMenu settings were reset."), true);
    }

    private static void resetServer(CommandSourceStack source) {
        ServerMenuConfigManager.resetAll();
        source.sendSuccess(() -> Component.literal("§aThe server-prescribed CreativeMenu settings were reset."), true);
    }
}
