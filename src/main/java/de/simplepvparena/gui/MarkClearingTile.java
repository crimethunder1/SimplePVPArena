package de.simplepvparena.gui;

import de.simplepvparena.arena.MarkClearing;
import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

enum MarkClearingTile {

    BORDER_MARKS(Material.TNT_MINECART, ConfirmationRequest.CLEAR_BORDER_MARKS,
            ArenaMenuMessage.CLEAR_BORDER_MARKS_TITLE, ArenaMenuMessage.CLEAR_BORDER_MARKS_LORE,
            ArenaMenuMessage.CLEAR_BORDER_MARKS_MARKED, ArenaMenuMessage.CLEAR_BORDER_MARKS_NONE_MARKED,
            ArenaMenuMessage.CLEAR_BORDER_MARKS_CONFIRMATION),
    START_GATE_MARKS(Material.IRON_CHAIN, ConfirmationRequest.CLEAR_START_GATE_MARKS,
            ArenaMenuMessage.CLEAR_START_GATE_MARKS_TITLE, ArenaMenuMessage.CLEAR_START_GATE_MARKS_LORE,
            ArenaMenuMessage.CLEAR_START_GATE_MARKS_MARKED, ArenaMenuMessage.CLEAR_START_GATE_MARKS_NONE_MARKED,
            ArenaMenuMessage.CLEAR_START_GATE_MARKS_CONFIRMATION);

    private final Material material;
    private final ConfirmationRequest confirmationRequest;
    private final MessageKey title;
    private final MessageKey lore;
    private final MessageKey markedCount;
    private final MessageKey noneMarked;
    private final MessageKey forgottenCount;

    MarkClearingTile(Material material, ConfirmationRequest confirmationRequest, MessageKey title, MessageKey lore,
                     MessageKey markedCount, MessageKey noneMarked, MessageKey forgottenCount) {
        this.material = material;
        this.confirmationRequest = confirmationRequest;
        this.title = title;
        this.lore = lore;
        this.markedCount = markedCount;
        this.noneMarked = noneMarked;
        this.forgottenCount = forgottenCount;
    }

    ConfirmationRequest getConfirmationRequest() {
        return confirmationRequest;
    }

    MenuTile build(MarkClearing clearing) {
        String displayName = Messages.text(title);
        Optional<String> rejection = clearing.findRejection();
        if (rejection.isPresent()) {
            return MenuItems.unavailable(displayName, rejection.get());
        }
        List<String> loreLines = new ArrayList<>(Messages.lines(lore));
        loreLines.add(Messages.plural(markedCount, clearing.countMarks()));
        loreLines.add(Messages.text(CommonMessage.ASKS_FOR_CONFIRMATION));
        return MenuItems.create(material, displayName, loreLines);
    }

    List<String> describeConfirmation(MarkClearing clearing) {
        int markCount = clearing.countMarks();
        return List.of(markCount == 0 ? Messages.text(noneMarked) : Messages.plural(forgottenCount, markCount),
                Messages.text(ArenaMenuMessage.CLEAR_MARKS_CONFIRMATION_BLOCKS_STAY));
    }
}
