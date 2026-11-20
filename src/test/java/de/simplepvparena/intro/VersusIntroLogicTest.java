package de.simplepvparena.intro;

import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.kit.KitEquipmentSlot;
import de.simplepvparena.support.RecordingItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersusIntroLogicTest {

    @Test
    void placesTheTeamsOnBothSidesFacingTowardsTheCentre() {
        List<VersusStageLayout.Spot> left = VersusStageLayout.spotsOf(VersusStageLayout.Side.LEFT, 3);
        List<VersusStageLayout.Spot> right = VersusStageLayout.spotsOf(VersusStageLayout.Side.RIGHT, 3);

        assertEquals(3, left.size());
        for (int index = 0; index < 3; index++) {
            assertTrue(left.get(index).right() < 0.0D, "the first team stands on the left");
            assertTrue(right.get(index).right() > 0.0D, "the second team stands on the right");
            assertEquals(-left.get(index).right(), right.get(index).right(), 1.0E-9D, "mirrored");
            assertEquals(-left.get(index).yawOffset(), right.get(index).yawOffset(), 1.0E-6F);
        }
        assertEquals(List.of(VersusStageLayout.Pose.SWORD, VersusStageLayout.Pose.BOW_JUMP,
                VersusStageLayout.Pose.SHIELD), left.stream().map(VersusStageLayout.Spot::pose).toList());
        assertTrue(left.get(1).up() > 0.0D, "the archer is shown in the middle of a jump");
        assertEquals(0.0D, left.get(0).up(), 1.0E-9D);
    }

    @Test
    void showsAtMostFourFightersPerSideAndWidensTheStageForThem() {
        assertEquals(VersusStageLayout.MAXIMUM_FIGHTERS_PER_SIDE,
                VersusStageLayout.spotsOf(VersusStageLayout.Side.LEFT, 8).size());
        assertEquals(1, VersusStageLayout.spotsOf(VersusStageLayout.Side.LEFT, 1).size());
        assertEquals(VersusStageLayout.MINIMUM_STAGE_HALF_WIDTH, VersusStageLayout.halfWidthFor(1), 1.0E-9D);
        double widest = VersusStageLayout.halfWidthFor(4);
        double outermost = Math.abs(VersusStageLayout.spotsOf(VersusStageLayout.Side.LEFT, 4).get(3).right());
        assertTrue(widest > outermost, "every fighter stands on the stage");
    }

    @Test
    void theCameraFliesInAndEndsStraightInFrontOfTheStage() {
        double halfWidth = VersusStageLayout.halfWidthFor(2);
        VersusCameraPath.Point start = VersusCameraPath.at(0, halfWidth);
        VersusCameraPath.Point end = VersusCameraPath.at(VersusCameraPath.FLIGHT_TICKS, halfWidth);
        VersusCameraPath.Point held = VersusCameraPath.at(VersusCameraPath.TOTAL_TICKS, halfWidth);

        assertTrue(start.forward() > end.forward(), "the camera comes closer");
        assertTrue(start.up() > end.up(), "and comes down");
        assertTrue(start.pitch() > 0.0F, "looking down at the stage at first");
        assertEquals(0.0D, end.right(), 1.0E-9D, "it ends in the middle");
        assertEquals(0.0F, end.yawOffset(), 1.0E-6F);
        assertEquals(end, held, "and holds still until the end");
        assertTrue(end.forward() > halfWidth, "far enough away to see the whole stage");
    }

    @Test
    void takesTheArmourAndTheWeaponsFromTheKit() {
        RecordingItemStack helmet = new RecordingItemStack(Material.IRON_HELMET, 1);
        RecordingItemStack shield = new RecordingItemStack(Material.SHIELD, 1);
        RecordingItemStack sword = new RecordingItemStack(Material.DIAMOND_SWORD, 1);
        RecordingItemStack bow = new RecordingItemStack(Material.BOW, 1);
        RecordingItemStack apple = new RecordingItemStack(Material.GOLDEN_APPLE, 1);
        KitDefinition kit = new KitDefinition(Map.of(KitEquipmentSlot.HEAD, helmet, KitEquipmentSlot.OFF_HAND, shield),
                Map.of(0, apple, 1, sword, 2, bow));

        VersusLoadout loadout = VersusLoadout.of(kit);

        assertEquals(Material.IRON_HELMET, loadout.armour().get(EquipmentSlot.HEAD).getType());
        assertEquals(1, loadout.armour().size(), "the shield is not armour");
        assertEquals(Material.DIAMOND_SWORD, loadout.weapon().orElseThrow().getType());
        assertEquals(Material.BOW, loadout.rangedWeapon().orElseThrow().getType());
        assertEquals(Material.SHIELD, loadout.shield().orElseThrow().getType());
    }

    @Test
    void fallsBackToTheFirstItemAndToTheCrossbowWhenTheKitHasNoSwordAndNoBow() {
        KitDefinition kit = new KitDefinition(Map.of(), Map.of(
                3, new RecordingItemStack(Material.CROSSBOW, 1),
                1, new RecordingItemStack(Material.STICK, 1)));

        VersusLoadout loadout = VersusLoadout.of(kit);

        assertEquals(Material.STICK, loadout.weapon().orElseThrow().getType());
        assertEquals(Material.CROSSBOW, loadout.rangedWeapon().orElseThrow().getType());
        assertTrue(loadout.shield().isEmpty());
        assertTrue(VersusLoadout.isMeleeWeapon(Material.NETHERITE_AXE));
        assertTrue(VersusLoadout.isMeleeWeapon(Material.MACE));
    }

    @Test
    void anEmptyKitLeavesTheFightersEmptyHanded() {
        VersusLoadout loadout = VersusLoadout.of(KitDefinition.empty());

        assertTrue(loadout.weapon().isEmpty());
        assertTrue(loadout.rangedWeapon().isEmpty());
        assertTrue(loadout.armour().isEmpty());
    }
}
