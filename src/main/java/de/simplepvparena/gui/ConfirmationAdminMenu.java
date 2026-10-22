package de.simplepvparena.gui;

import de.simplepvparena.message.ConfirmationMessage;
import de.simplepvparena.message.Messages;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ConfirmationAdminMenu implements AdminMenu {

    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;

    private final AdminMenuNavigation navigation;
    private final Map<ConfirmationRequest, AdminMenuConfirmation> confirmationsByRequest =
            new EnumMap<>(ConfirmationRequest.class);

    ConfirmationAdminMenu(AdminMenuNavigation navigation) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
    }

    void register(ConfirmationRequest request, AdminMenuConfirmation confirmation) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(confirmation, "confirmation");
        if (confirmationsByRequest.containsKey(request)) {
            throw new IllegalStateException("the " + request + " confirmation is registered already");
        }
        confirmationsByRequest.put(request, confirmation);
    }

    void requireEveryConfirmationRegistered() {
        Set<ConfirmationRequest> missingRequests = EnumSet.allOf(ConfirmationRequest.class);
        missingRequests.removeAll(confirmationsByRequest.keySet());
        if (!missingRequests.isEmpty()) {
            throw new IllegalStateException("no confirmation is registered for " + missingRequests);
        }
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return holder.getConfirmationRequest().orElseThrow().getTitle(holder.getSubject());
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        ConfirmationRequest request = holder.getConfirmationRequest().orElseThrow();
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(request.getTitle(holder.getSubject()),
                buildConfirmationLore(request, holder)));
        canvas.set(CONFIRM_YES_SLOT, MenuItems.create(MenuItems.CONFIRM_MATERIAL,
                Messages.text(ConfirmationMessage.CONFIRM_TITLE), Messages.lines(ConfirmationMessage.CONFIRM_LORE)));
        canvas.set(CONFIRM_NO_SLOT, MenuItems.create(MenuItems.REFUSE_MATERIAL,
                Messages.text(ConfirmationMessage.REFUSE_TITLE), Messages.lines(ConfirmationMessage.REFUSE_LORE)));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        if (slot != CONFIRM_YES_SLOT && slot != CONFIRM_NO_SLOT) {
            return;
        }
        if (slot == CONFIRM_YES_SLOT) {
            carryOutConfirmedAction(player, holder);
        }
        openReturnMenu(player, holder);
    }

    private void carryOutConfirmedAction(Player player, AdminMenuHolder holder) {
        confirmationOf(holder.getConfirmationRequest().orElseThrow()).carryOut(player, holder);
    }

    private void openReturnMenu(Player player, AdminMenuHolder holder) {
        navigation.open(player, confirmationOf(holder.getConfirmationRequest().orElseThrow()).returnMenu(holder));
    }

    private List<String> buildConfirmationLore(ConfirmationRequest request, AdminMenuHolder holder) {
        return confirmationOf(request).describe(holder);
    }

    private AdminMenuConfirmation confirmationOf(ConfirmationRequest request) {
        AdminMenuConfirmation confirmation = confirmationsByRequest.get(request);
        if (confirmation == null) {
            throw new IllegalStateException("the " + request + " confirmation is not registered");
        }
        return confirmation;
    }
}
