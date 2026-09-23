package dev.thirteenblade.client;

import dev.thirteenblade.*;
import dev.thirteenblade.chat.ChatMessage;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Private client GUI; ordinary chat packets are never sent by this screen. */
public final class SwordChatScreen extends GuiScreen {
    private final ItemStack sword; private final SwordChat.Session session;
    private GuiTextField input; private int scroll;
    public SwordChatScreen(ItemStack sword) { this.sword = sword.copy(); session = SwordChat.session(sword); }
    @Override public void initGui() {
        Keyboard.enableRepeatEvents(true); buttonList.clear();
        input = new GuiTextField(0, fontRenderer, 16, height - 44, Math.max(50, width - 116), 20); input.setMaxStringLength(512); input.setFocused(true);
        buttonList.add(new GuiButton(1, width - 92, height - 44, 76, 20, I18n.format("chat.thirteenblade.send")));
        buttonList.add(new GuiButton(2, width - 92, 10, 76, 20, I18n.format("chat.thirteenblade.reload")));
    }
    private void send() { if (session.busy || input.getText().trim().isEmpty()) return; SwordChat.send(session, sword, input.getText()); input.setText(""); scroll = 0; }
    @Override protected void actionPerformed(GuiButton button) { if (button.id == 1) send(); else if (button.id == 2) SwordChat.reload(); }
    @Override protected void keyTyped(char character, int code) throws IOException {
        if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) send();
        else if (!input.textboxKeyTyped(character, code)) super.keyTyped(character, code);
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException { super.mouseClicked(x, y, button); input.mouseClicked(x, y, button); }
    @Override public void handleMouseInput() throws IOException { super.handleMouseInput(); int delta = Mouse.getEventDWheel(); if (delta != 0) scroll = Math.max(0, scroll + (delta > 0 ? 3 : -3)); }
    @Override public void updateScreen() { input.updateCursorCounter(); }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground(); drawRect(8, 7, width - 8, height - 8, 0xDD141124);
        fontRenderer.drawStringWithShadow(I18n.format("chat.thirteenblade.title"), 16, 16, 0xD9BCFF);
        String notice = SwordChat.configError ? "chat.thirteenblade.config_error" : SwordChat.settings.enabled ? "chat.thirteenblade.api_mode" : "chat.thirteenblade.offline_mode";
        fontRenderer.drawSplitString(I18n.format(notice), 16, 36, width - 32, 0xA8A2BE);
        fontRenderer.drawString(I18n.format("hud.thirteenblade.level", BladeData.level(sword), BladeData.kills(sword)), 16, 59, 0xD9BCFF);
        List<String> lines = new ArrayList<>();
        if (session.messages.isEmpty()) lines.addAll(fontRenderer.listFormattedStringToWidth(I18n.format("chat.thirteenblade.welcome"), width - 40));
        for (ChatMessage message : session.messages) lines.addAll(fontRenderer.listFormattedStringToWidth(I18n.format("user".equals(message.role) ? "chat.thirteenblade.you" : "chat.thirteenblade.spirit") + ": " + message.content, width - 40));
        if (session.busy) lines.add(I18n.format("chat.thirteenblade.thinking"));
        if (!session.notice.isEmpty()) lines.addAll(fontRenderer.listFormattedStringToWidth(session.notice, width - 40));
        int visible = Math.max(1, (height - 130) / 11); scroll = Math.min(scroll, Math.max(0, lines.size() - visible));
        int first = Math.max(0, lines.size() - visible - scroll);
        for (int i = first; i < Math.min(lines.size(), first + visible); i++) fontRenderer.drawString(lines.get(i), 18, 76 + (i - first) * 11, 0xE7E1F2);
        input.drawTextBox(); fontRenderer.drawString(I18n.format("chat.thirteenblade.footer"), 16, height - 18, 0x9992A9);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
