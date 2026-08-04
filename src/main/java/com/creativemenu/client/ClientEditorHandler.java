package com.creativemenu.client;

import com.creativemenu.client.gui.CreativeMenuEditorScreen;
import com.creativemenu.client.gui.ServerPrescriptionEditorScreen;
import net.minecraft.client.Minecraft;

public class ClientEditorHandler {

    public static void openEditor(boolean serverMode) {
        Minecraft.getInstance().setScreen(serverMode ? new ServerPrescriptionEditorScreen() : new CreativeMenuEditorScreen());
    }
}
