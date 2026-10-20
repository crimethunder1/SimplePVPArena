package de.simplepvparena.diagnostics;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.HorizontalCellCodec;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.gate.StartGateMarks;
import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.kit.KitEquipmentSlot;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.TournamentState;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

final class DryRunFixture {

    static final String WORLD = "event";

    final TeamRegistry teamRegistry = new TeamRegistry();
    final ConfigurableDryRunArena arena = new ConfigurableDryRunArena();
    final KitService kitService = new KitService();
    final StartGateMarks gateMarks = new StartGateMarks();
    final StartGateDuration gateDuration = new StartGateDuration();

    TournamentState tournamentState = TournamentState.IDLE;
    boolean matchRunning;

    TournamentDryRun dryRun() {
        return new TournamentDryRun(() -> tournamentState, () -> matchRunning, teamRegistry, arena, kitService,
                gateMarks, gateDuration);
    }

    DryRunStep findStep(MessageKey stepKey) {
        return findStep(dryRun().run(), stepKey);
    }

    static DryRunStep findStep(DryRunReport report, MessageKey stepKey) {
        String stepName = Messages.text(stepKey);
        return report.getSteps().stream()
                .filter(step -> step.name().equals(stepName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the report holds no step " + stepName + ": "
                        + report.describe()));
    }

    void makeTheServerReadyForItsEvent() {
        arena.volume = shape();
        arena.firstTeamSpawn = true;
        arena.secondTeamSpawn = true;
        arena.baseline = true;
        arena.cleaningRejection = null;
        markGate(5, 3);
        giveKit(2, 3);
    }

    void markGate(int layerCount, int blocksPerLayer) {
        gateMarks.clear();
        for (int layer = 0; layer < layerCount; layer++) {
            for (int blockX = 0; blockX < blocksPerLayer; blockX++) {
                gateMarks.mark(WORLD, blockX, 64 + layer, 0, Material.BARRIER);
            }
        }
    }

    void giveKit(int wornItemCount, int inventoryItemCount) {
        Map<KitEquipmentSlot, ItemStack> wornItems = new EnumMap<>(KitEquipmentSlot.class);
        for (int slotIndex = 0; slotIndex < wornItemCount; slotIndex++) {
            wornItems.put(KitEquipmentSlot.values()[slotIndex], new FakeItemStack("worn " + slotIndex));
        }
        Map<Integer, ItemStack> inventoryItems = new TreeMap<>();
        for (int slotIndex = 0; slotIndex < inventoryItemCount; slotIndex++) {
            inventoryItems.put(slotIndex, new FakeItemStack("carried " + slotIndex));
        }
        kitService.setKitDefinition(new KitDefinition(wornItems, inventoryItems));
    }

    void registerRealTeam(String teamName) {
        teamRegistry.registerTeam(teamName, List.of(
                new TeamMember(UUID.randomUUID(), teamName + "-first"),
                new TeamMember(UUID.randomUUID(), teamName + "-second")));
    }

    private static ArenaShape shape() {
        return new ArenaShape(WORLD, 64, 64, 67, new long[] {
                HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(0, 1),
                HorizontalCellCodec.encode(1, 0), HorizontalCellCodec.encode(1, 1)});
    }

    static final class ConfigurableDryRunArena implements DryRunArena {

        ArenaShape volume;
        boolean firstTeamSpawn;
        boolean secondTeamSpawn;
        boolean baseline;
        String cleaningRejection;

        @Override
        public boolean isReady() {
            return volume != null;
        }

        @Override
        public List<String> getMissingConfiguration() {
            return isReady() ? List.of() : new Arena().getMissingConfiguration();
        }

        @Override
        public Optional<ArenaShape> getVolume() {
            return Optional.ofNullable(volume);
        }

        @Override
        public boolean hasFirstTeamSpawn() {
            return firstTeamSpawn;
        }

        @Override
        public boolean hasSecondTeamSpawn() {
            return secondTeamSpawn;
        }

        @Override
        public boolean hasBaseline() {
            return baseline;
        }

        @Override
        public Optional<ArenaShape> findCleanedVolume() {
            return getVolume().map(shape -> baseline
                    ? shape.withFloorLoweredBy(ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR)
                    : shape);
        }

        @Override
        public long countCleaningBlocks() {
            return findCleanedVolume().map(ArenaShape::getBlockCount).orElse(0L);
        }

        @Override
        public Optional<String> findCleaningRejection() {
            return Optional.ofNullable(cleaningRejection);
        }
    }
}
