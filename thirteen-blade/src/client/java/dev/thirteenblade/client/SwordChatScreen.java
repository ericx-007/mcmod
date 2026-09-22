package dev.thirteenblade.client;

import dev.thirteenblade.BladeData;
import dev.thirteenblade.SoulPower;
import dev.thirteenblade.BladeInventory;
import dev.thirteenblade.ThirteenBlade;
import dev.thirteenblade.ThirteenBladeItem;
import dev.thirteenblade.chat.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Private client screen: nothing typed here is sent to Minecraft chat. */
public final class SwordChatScreen extends Screen {
    private final String swordId;
    private final SwordChat.Session session;
    private ItemStack sword;
    private TextFieldWidget input;
    private ButtonWidget send;
    private int left, right, top, bottom, chatLeft, chatTop, chatBottom;
    private int scroll;
    private int maxScroll;
    private int lastMessageCount;
    private boolean sidebar;
    private int statScroll, maxStatScroll;
    private record Line(OrderedText text, int color) {}

    public SwordChatScreen(ItemStack sword) {
        super(Text.translatable("chat.thirteenblade.title"));
        this.sword = sword.copy();
        this.swordId = BladeData.identity(sword);
        this.session = SwordChat.session(sword);
        SwordChat.reload();
    }

    @Override protected void init() {
        String draft = input == null ? "" : input.getText();
        left = Math.max(10, (width - 720) / 2);
        right = width - left;
        top = 18;
        bottom = height - 18;
        sidebar = right - left >= 540;
        chatLeft = left + (sidebar ? 168 : 14);
        chatTop = top + 73;
        chatBottom = bottom - 64;
        input = new TextFieldWidget(textRenderer, chatLeft + 2, bottom - 49,
                right - chatLeft - 82, 20, Text.translatable("chat.thirteenblade.input"));
        input.setMaxLength(512);
        input.setPlaceholder(Text.translatable("chat.thirteenblade.input"));
        input.setText(draft);
        addDrawableChild(input);
        send = addDrawableChild(ButtonWidget.builder(Text.translatable("chat.thirteenblade.send"), button -> submit())
                .dimensions(right - 72, bottom - 49, 58, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("chat.thirteenblade.reload"), button -> SwordChat.reload())
                .dimensions(right - 135, top + 13, 84, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("×"), button -> close())
                .dimensions(right - 43, top + 13, 28, 20).build());
        setInitialFocus(input);
        lastMessageCount = session.messages.size();
    }

    private void submit() {
        if (!send.active) return;
        SwordChat.send(session, sword, input.getText());
        input.setText("");
        scroll = 0;
        setFocused(input);
    }

    @Override public void tick() {
        input.tick();
        if (client == null || client.player == null) { close(); return; }
        ItemStack current = BladeInventory.activeSword(client.player);
        if (!ThirteenBlade.isSword(current) || !BladeData.identity(current).equals(swordId)) { close(); return; }
        sword = current.copy();
        send.active = !session.busy && !input.getText().isBlank() && System.currentTimeMillis() >= session.nextSendAt;
        if (session.messages.size() != lastMessageCount) { scroll = 0; lastMessageCount = session.messages.size(); }
    }

    @Override public boolean shouldPause() { return false; }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && input.isFocused()) {
            submit();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (sidebar && mouseX >= left + 12 && mouseX < chatLeft && mouseY >= chatTop && mouseY <= bottom - 20) {
            statScroll = Math.max(0, Math.min(maxStatScroll, statScroll - (int) (amount * 24)));
            return true;
        }
        if (mouseY >= chatTop && mouseY <= chatBottom && mouseX >= chatLeft && mouseX <= right - 12) {
            scroll = Math.max(0, Math.min(maxScroll, scroll + (int) (amount * 3)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.fillGradient(left, top, right, bottom, 0xF5131C2A, 0xF50C111B);
        context.fill(left, top, right, top + 2, 0xFF76E3E3);
        context.fill(left + 12, top + 57, right - 12, top + 58, 0xFF304457);
        context.drawItem(sword, left + 14, top + 14);
        context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(title.getString(), right - left - 180),
                left + 37, top + 17, 0xE6F5FF);
        String modeKey = SwordChat.configError ? "chat.thirteenblade.config_error"
                : SwordChat.settings.enabled ? "chat.thirteenblade.api_mode" : "chat.thirteenblade.offline_mode";
        context.drawText(textRenderer, textRenderer.trimToWidth(Text.translatable(modeKey).getString(), right - left - 30),
                left + 15, top + 41, SwordChat.configError ? 0xFFAB91 : 0x93B5C9, false);
        if (sidebar) renderStats(context);

        List<Line> lines = new ArrayList<>();
        int wrapWidth = right - chatLeft - 34;
        if (session.messages.isEmpty()) {
            addLines(lines, Text.translatable("chat.thirteenblade.welcome"), wrapWidth, 0xC4B2EF);
            addLines(lines, Text.literal(""), wrapWidth, 0xFFFFFF);
            addLines(lines, Text.translatable("chat.thirteenblade.suggestions"), wrapWidth, 0x829BAF);
        } else {
            for (ChatMessage message : session.messages) {
                boolean user = message.role().equals("user");
                addLines(lines, Text.translatable(user ? "chat.thirteenblade.you" : "chat.thirteenblade.spirit"), wrapWidth,
                        user ? 0x75DCD3 : 0xCAA2FF);
                addLines(lines, Text.literal(message.content()), wrapWidth, user ? 0xC9DFE7 : 0xE3DBF4);
                lines.add(new Line(Text.empty().asOrderedText(), 0));
            }
        }
        int visible = Math.max(1, (chatBottom - chatTop) / 12);
        maxScroll = Math.max(0, lines.size() - visible);
        scroll = Math.min(scroll, maxScroll);
        int start = Math.max(0, lines.size() - visible - scroll);
        context.enableScissor(chatLeft, chatTop, right - 14, chatBottom);
        for (int i = start; i < Math.min(lines.size(), start + visible); i++)
            context.drawText(textRenderer, lines.get(i).text, chatLeft + 2, chatTop + (i - start) * 12, lines.get(i).color, false);
        context.disableScissor();
        if (maxScroll > 0) {
            int track = chatBottom - chatTop;
            int thumb = Math.max(12, track * visible / lines.size());
            int y = chatTop + (track - thumb) * (maxScroll - scroll) / maxScroll;
            context.fill(right - 17, chatTop, right - 15, chatBottom, 0xFF263548);
            context.fill(right - 17, y, right - 15, y + thumb, 0xFF8BA4C0);
        }
        Text status = session.busy ? Text.translatable("chat.thirteenblade.thinking")
                : !session.notice.isEmpty() ? Text.literal(session.notice) : Text.translatable("chat.thirteenblade.footer");
        context.drawText(textRenderer, textRenderer.trimToWidth(status.getString(), right - chatLeft - 18),
                chatLeft + 2, bottom - 19, session.notice.isEmpty() ? 0x8098AD : 0xFFB294, false);
        super.render(context, mouseX, mouseY, delta);
    }

    private void addLines(List<Line> lines, Text text, int width, int color) {
        for (OrderedText line : textRenderer.wrapLines(text, width)) lines.add(new Line(line, color));
    }

    private void renderStats(DrawContext context) {
        int x = left + 15;
        int y = chatTop;
        context.fill(left + 151, chatTop - 4, left + 152, bottom - 17, 0xFF28374A);
        List<Text> rows = new ArrayList<>(List.of(
                Text.translatable("chat.thirteenblade.stats"),
                Text.translatable("chat.thirteenblade.stat_level", BladeData.level(sword)),
                Text.translatable("chat.thirteenblade.stat_kills", BladeData.kills(sword)),
                Text.translatable("chat.thirteenblade.stat_damage", ThirteenBladeItem.number(BladeData.baseAttack(sword) + BladeData.damageBonus(sword))),
                Text.translatable("chat.thirteenblade.stat_health", ThirteenBladeItem.number(BladeData.healthBonus(sword))),
                Text.empty(),
                Text.translatable("chat.thirteenblade.powers"),
                Text.translatable("tooltip.thirteenblade.toughness", ThirteenBladeItem.number(BladeData.toughnessBonus(sword)), BladeData.soulCount(sword)),
                Text.translatable("chat.thirteenblade.stolen_count", BladeData.stolenEffects(sword, System.currentTimeMillis()).size())
        ));
        for (SoulPower power : SoulPower.values())
            if (power.known(sword)) rows.add(Text.translatable(power.translation));
        context.enableScissor(x, y, left + 148, bottom - 20);
        y -= statScroll;
        for (int i = 0; i < rows.size(); i++) {
            for (OrderedText line : textRenderer.wrapLines(rows.get(i), 128)) {
                context.drawText(textRenderer, line, x, y, i == 0 || i == 6 ? 0x86DBD8 : 0xAABCCE, false);
                y += 12;
            }
            y += 4;
        }
        maxStatScroll = Math.max(0, y + statScroll - (bottom - 20));
        statScroll = Math.min(statScroll, maxStatScroll);
        context.disableScissor();
    }
}
