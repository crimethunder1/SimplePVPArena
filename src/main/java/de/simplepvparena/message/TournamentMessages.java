package de.simplepvparena.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class TournamentMessages {

    public static final String LINE_BREAK = "\n";

    public static final String PREFIX_NAME = "Tournament";

    public static final String PLAIN_PREFIX = "[" + PREFIX_NAME + "] ";

    public static final TextColor PREFIX_COLOUR = NamedTextColor.GOLD;
    public static final TextColor BRACKET_COLOUR = NamedTextColor.DARK_GRAY;
    public static final TextColor SUCCESS_COLOUR = NamedTextColor.GREEN;
    public static final TextColor ERROR_COLOUR = NamedTextColor.RED;
    public static final TextColor WARNING_COLOUR = NamedTextColor.YELLOW;
    public static final TextColor INFO_COLOUR = NamedTextColor.WHITE;

    public static final TextColor DETAIL_COLOUR = NamedTextColor.GRAY;

    private TournamentMessages() {
    }

    public static Component success(String text) {
        return prefixed(SUCCESS_COLOUR, text);
    }

    public static Component error(String text) {
        return prefixed(ERROR_COLOUR, text);
    }

    public static Component warning(String text) {
        return prefixed(WARNING_COLOUR, text);
    }

    public static Component info(String text) {
        return prefixed(INFO_COLOUR, text);
    }

    public static Component detail(String text) {
        return line(DETAIL_COLOUR, text);
    }

    public static Component prefix() {
        return Component.text()
                .decoration(TextDecoration.ITALIC, false)
                .append(line(BRACKET_COLOUR, "["))
                .append(line(PREFIX_COLOUR, PREFIX_NAME))
                .append(line(BRACKET_COLOUR, "] "))
                .build();
    }

    public static void sendSuccess(CommandSender receiver, String text) {
        receiver.sendMessage(success(text));
    }

    public static void sendError(CommandSender receiver, String text) {
        receiver.sendMessage(error(text));
    }

    public static void sendWarning(CommandSender receiver, String text) {
        receiver.sendMessage(warning(text));
    }

    public static void sendInfo(CommandSender receiver, String text) {
        receiver.sendMessage(info(text));
    }

    public static void sendDetail(CommandSender receiver, String text) {
        receiver.sendMessage(detail(text));
    }

    public static void sendActionBarSuccess(CommandSender receiver, String text) {
        receiver.sendActionBar(line(SUCCESS_COLOUR, text));
    }

    public static void sendActionBarWarning(CommandSender receiver, String text) {
        receiver.sendActionBar(line(WARNING_COLOUR, text));
    }

    public static void sendActionBarError(CommandSender receiver, String text) {
        receiver.sendActionBar(line(ERROR_COLOUR, text));
    }

    public static void sendBlock(CommandSender receiver, Component head, List<String> detailLines) {
        receiver.sendMessage(head);
        for (String detailLine : detailLines) {
            receiver.sendMessage(detail(detailLine));
        }
    }

    public static void sendInfoReport(CommandSender receiver, List<String> lines) {
        sendReport(receiver, INFO_COLOUR, lines);
    }

    public static void sendSuccessReport(CommandSender receiver, List<String> lines) {
        sendReport(receiver, SUCCESS_COLOUR, lines);
    }

    public static void sendErrorReport(CommandSender receiver, List<String> lines) {
        sendReport(receiver, ERROR_COLOUR, lines);
    }

    public static String toPlainText(Component message) {
        return PlainTextComponentSerializer.plainText().serialize(message);
    }

    public static String toPlainBody(Component message) {
        String plainText = toPlainText(message);
        return plainText.startsWith(PLAIN_PREFIX) ? plainText.substring(PLAIN_PREFIX.length()) : plainText;
    }

    private static void sendReport(CommandSender receiver, TextColor headColour, List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        sendBlock(receiver, prefixed(headColour, lines.get(0)), lines.subList(1, lines.size()));
    }

    private static Component prefixed(TextColor bodyColour, String text) {
        String[] textLines = text.split(LINE_BREAK, -1);
        TextComponent.Builder message = Component.text()
                .decoration(TextDecoration.ITALIC, false)
                .append(prefix())
                .append(line(bodyColour, textLines[0]));
        for (int lineIndex = 1; lineIndex < textLines.length; lineIndex++) {
            message.append(line(DETAIL_COLOUR, LINE_BREAK)).append(line(DETAIL_COLOUR, textLines[lineIndex]));
        }
        return message.build();
    }

    private static Component line(TextColor colour, String text) {
        return Component.text(text, colour).decoration(TextDecoration.ITALIC, false);
    }
}
