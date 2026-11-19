package de.simplepvparena.tournament;

import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.LibraryKit;
import de.simplepvparena.message.KitChoiceMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class KitChoiceService {

    public static final int CHOICE_SECONDS = 30;
    public static final int REMINDER_SECONDS_LEFT = 10;
    public static final int MINIMUM_KIT_COUNT_FOR_A_CHOICE = 2;

    private static final long TICKS_PER_SECOND = 20L;
    private static final String NO_DEFAULT_KIT_NAME = "none";

    private final @Nullable Plugin plugin;
    private final @Nullable KitLibrary kitLibrary;
    private final @Nullable AdminNotifier adminNotifier;
    private final Clock clock;
    private final List<BukkitTask> pendingTasks = new ArrayList<>();

    private KitChoicePresenter presenter = KitChoicePresenter.NONE;
    private @Nullable PendingKitChoice pendingKitChoice;
    private @Nullable Runnable pendingMatchStart;

    public KitChoiceService() {
        this.plugin = null;
        this.kitLibrary = null;
        this.adminNotifier = null;
        this.clock = Clock.systemUTC();
    }

    public KitChoiceService(Plugin plugin, KitLibrary kitLibrary, AdminNotifier adminNotifier) {
        this(plugin, kitLibrary, adminNotifier, Clock.systemUTC());
    }

    public KitChoiceService(Plugin plugin, KitLibrary kitLibrary, AdminNotifier adminNotifier, Clock clock) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.kitLibrary = Objects.requireNonNull(kitLibrary, "kitLibrary");
        this.adminNotifier = Objects.requireNonNull(adminNotifier, "adminNotifier");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void setPresenter(KitChoicePresenter presenter) {
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    public void begin(String matchId, String firstTeamName, String secondTeamName, Runnable startMatch) {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(firstTeamName, "firstTeamName");
        Objects.requireNonNull(secondTeamName, "secondTeamName");
        Objects.requireNonNull(startMatch, "startMatch");
        if (pendingKitChoice != null) {
            throw new IllegalStateException("the kit for match " + pendingKitChoice.matchId()
                    + " is still being chosen, match " + matchId + " cannot begin a second kit choice");
        }
        KitLibrary library = kitLibrary;
        if (library == null) {
            startMatch.run();
            return;
        }
        if (library.getKitCount() < MINIMUM_KIT_COUNT_FOR_A_CHOICE) {
            library.activateDefaultKit();
            startMatch.run();
            return;
        }
        String defaultKitName = findDefaultKitName();
        if (!presenter.hasAudience()) {
            Objects.requireNonNull(plugin, "plugin").getLogger().info("No admin is online to choose the kit for match "
                    + matchId + ", the default kit " + defaultKitName + " is used");
            library.activateDefaultKit();
            startMatch.run();
            return;
        }
        PendingKitChoice choice = new PendingKitChoice(matchId, firstTeamName, secondTeamName, defaultKitName,
                clock.instant().plusSeconds(CHOICE_SECONDS));
        pendingKitChoice = choice;
        pendingMatchStart = startMatch;
        schedule(() -> remind(choice), CHOICE_SECONDS - REMINDER_SECONDS_LEFT);
        schedule(() -> expire(choice), CHOICE_SECONDS);
        Objects.requireNonNull(adminNotifier, "adminNotifier").notifyAdmins(Messages.plural(KitChoiceMessage.BEGIN,
                CHOICE_SECONDS, Placeholder.of("match", matchId), Placeholder.of("firstTeam", firstTeamName),
                Placeholder.of("secondTeam", secondTeamName), Placeholder.of("kit", defaultKitName)));
        presenter.show(choice);
    }

    public boolean choose(String kitName, String chooserName) {
        Objects.requireNonNull(chooserName, "chooserName");
        PendingKitChoice choice = pendingKitChoice;
        if (choice == null) {
            return false;
        }
        KitLibrary library = Objects.requireNonNull(kitLibrary, "kitLibrary");
        Optional<LibraryKit> kit = library.findKit(kitName);
        if (kit.isEmpty()) {
            return false;
        }
        Runnable startMatch = finishPendingChoice();
        library.activate(kit.get().name());
        presenter.close(Messages.text(KitChoiceMessage.CHOSEN, Placeholder.of("player", chooserName),
                Placeholder.of("kit", kit.get().name()), Placeholder.of("match", choice.matchId())));
        startMatch.run();
        return true;
    }

    public void cancel() {
        if (pendingKitChoice == null) {
            return;
        }
        finishPendingChoice();
        presenter.close(Messages.text(KitChoiceMessage.CANCELLED));
    }

    public void activateDefaultKit() {
        if (kitLibrary != null) {
            kitLibrary.activateDefaultKit();
        }
    }

    public boolean isPending() {
        return pendingKitChoice != null;
    }

    public Optional<PendingKitChoice> getPending() {
        return Optional.ofNullable(pendingKitChoice);
    }

    public long getSecondsLeft() {
        return getPending().map(choice -> choice.secondsLeftAt(clock.instant())).orElse(0L);
    }

    private void remind(PendingKitChoice choice) {
        if (pendingKitChoice != choice) {
            return;
        }
        Objects.requireNonNull(adminNotifier, "adminNotifier").notifyAdmins(Messages.plural(KitChoiceMessage.REMINDER,
                REMINDER_SECONDS_LEFT, Placeholder.of("match", choice.matchId())));
    }

    private void expire(PendingKitChoice choice) {
        if (pendingKitChoice != choice) {
            return;
        }
        Runnable startMatch = finishPendingChoice();
        Objects.requireNonNull(kitLibrary, "kitLibrary").activateDefaultKit();
        presenter.close(Messages.text(KitChoiceMessage.EXPIRED, Placeholder.of("match", choice.matchId()),
                Placeholder.of("kit", findDefaultKitName())));
        startMatch.run();
    }

    private Runnable finishPendingChoice() {
        Runnable startMatch = Objects.requireNonNull(pendingMatchStart, "pendingMatchStart");
        pendingKitChoice = null;
        pendingMatchStart = null;
        pendingTasks.forEach(BukkitTask::cancel);
        pendingTasks.clear();
        return startMatch;
    }

    private void schedule(Runnable task, int delaySeconds) {
        Plugin schedulingPlugin = Objects.requireNonNull(plugin, "plugin");
        pendingTasks.add(schedulingPlugin.getServer().getScheduler().runTaskLater(schedulingPlugin, task,
                delaySeconds * TICKS_PER_SECOND));
    }

    private String findDefaultKitName() {
        return Objects.requireNonNull(kitLibrary, "kitLibrary").getDefaultKitName().orElse(NO_DEFAULT_KIT_NAME);
    }
}
