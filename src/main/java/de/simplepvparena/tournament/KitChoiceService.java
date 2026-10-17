package de.simplepvparena.tournament;

import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.LibraryKit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class KitChoiceService {

    public static final int CHOICE_SECONDS = 30;
    public static final int REMINDER_SECONDS_LEFT = 10;
    public static final int MINIMUM_KIT_COUNT_FOR_A_CHOICE = 2;

    static final String CANCELLED_MESSAGE = "The kit choice was cancelled";

    private static final long TICKS_PER_SECOND = 20L;
    private static final String NO_DEFAULT_KIT_NAME = "none";

    private final Plugin plugin;
    private final KitLibrary kitLibrary;
    private final AdminNotifier adminNotifier;
    private final Clock clock;
    private final List<BukkitTask> pendingTasks = new ArrayList<>();

    private KitChoicePresenter presenter = KitChoicePresenter.NONE;
    private PendingKitChoice pendingKitChoice;
    private Runnable pendingMatchStart;

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

    public void begin(String matchId, String teamsDescription, Runnable startMatch) {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(teamsDescription, "teamsDescription");
        Objects.requireNonNull(startMatch, "startMatch");
        if (pendingKitChoice != null) {
            throw new IllegalStateException("the kit for match " + pendingKitChoice.matchId()
                    + " is still being chosen, match " + matchId + " cannot begin a second kit choice");
        }
        if (kitLibrary == null) {
            startMatch.run();
            return;
        }
        if (kitLibrary.getKitCount() < MINIMUM_KIT_COUNT_FOR_A_CHOICE) {
            kitLibrary.activateDefaultKit();
            startMatch.run();
            return;
        }
        String defaultKitName = findDefaultKitName();
        if (!presenter.hasAudience()) {
            plugin.getLogger().info("No admin is online to choose the kit for match " + matchId
                    + ", the default kit " + defaultKitName + " is used");
            kitLibrary.activateDefaultKit();
            startMatch.run();
            return;
        }
        PendingKitChoice choice = new PendingKitChoice(matchId, teamsDescription, defaultKitName,
                clock.instant().plusSeconds(CHOICE_SECONDS));
        pendingKitChoice = choice;
        pendingMatchStart = startMatch;
        schedule(() -> remind(choice), CHOICE_SECONDS - REMINDER_SECONDS_LEFT);
        schedule(() -> expire(choice), CHOICE_SECONDS);
        adminNotifier.notifyAdmins("Match " + matchId + " (" + teamsDescription + "): choose the kit within "
                + CHOICE_SECONDS + " seconds, otherwise the default kit " + defaultKitName + " is used");
        presenter.show(choice);
    }

    public boolean choose(String kitName, String chooserName) {
        Objects.requireNonNull(chooserName, "chooserName");
        if (pendingKitChoice == null) {
            return false;
        }
        Optional<LibraryKit> kit = kitLibrary.findKit(kitName);
        if (kit.isEmpty()) {
            return false;
        }
        PendingKitChoice choice = pendingKitChoice;
        Runnable startMatch = finishPendingChoice();
        kitLibrary.activate(kit.get().name());
        presenter.close(chooserName + " chose the kit " + kit.get().name() + " for match " + choice.matchId());
        startMatch.run();
        return true;
    }

    public void cancel() {
        if (pendingKitChoice == null) {
            return;
        }
        finishPendingChoice();
        presenter.close(CANCELLED_MESSAGE);
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
        adminNotifier.notifyAdmins(REMINDER_SECONDS_LEFT + " seconds left to choose the kit for match "
                + choice.matchId());
    }

    private void expire(PendingKitChoice choice) {
        if (pendingKitChoice != choice) {
            return;
        }
        Runnable startMatch = finishPendingChoice();
        kitLibrary.activateDefaultKit();
        presenter.close("No kit was chosen for match " + choice.matchId() + ", the default kit "
                + findDefaultKitName() + " is used");
        startMatch.run();
    }

    private Runnable finishPendingChoice() {
        Runnable startMatch = pendingMatchStart;
        pendingKitChoice = null;
        pendingMatchStart = null;
        pendingTasks.forEach(BukkitTask::cancel);
        pendingTasks.clear();
        return startMatch;
    }

    private void schedule(Runnable task, int delaySeconds) {
        pendingTasks.add(plugin.getServer().getScheduler().runTaskLater(plugin, task,
                delaySeconds * TICKS_PER_SECOND));
    }

    private String findDefaultKitName() {
        return kitLibrary.getDefaultKitName().orElse(NO_DEFAULT_KIT_NAME);
    }
}
