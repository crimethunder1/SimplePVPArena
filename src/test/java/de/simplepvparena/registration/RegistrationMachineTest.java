package de.simplepvparena.registration;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationMachineTest {

    @Test
    void aMachineFacingSouthReachesToTheWestAndTwoBlocksUp() {
        RegistrationMachine machine = new RegistrationMachine("event", 10, 64, 20, BlockFace.SOUTH);

        assertEquals(BlockFace.WEST, machine.right());
        assertEquals(List.of("10/64/20", "10/65/20", "9/64/20", "9/65/20"), describe(machine));
        assertTrue(machine.contains("event", 9, 65, 20));
        assertFalse(machine.contains("event", 11, 64, 20));
        assertFalse(machine.contains("nether", 10, 64, 20));
        assertEquals(10.0D, machine.frontCentreX());
        assertEquals(21.0D, machine.frontCentreZ());
    }

    @Test
    void aMachineFacingEastHasItsFrontOnTheEastSide() {
        RegistrationMachine machine = new RegistrationMachine("event", 10, 64, 20, BlockFace.EAST);

        assertEquals(BlockFace.SOUTH, machine.right());
        assertEquals(List.of("10/64/20", "10/65/20", "10/64/21", "10/65/21"), describe(machine));
        assertEquals(11.0D, machine.frontCentreX());
        assertEquals(21.0D, machine.frontCentreZ());
    }

    @Test
    void refusesAFacingThatIsNotHorizontal() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegistrationMachine("event", 0, 64, 0, BlockFace.UP));
    }

    @Test
    void theCollectionRefusesOverlappingMachinesAndFindsEveryBlock() {
        RegistrationMachines machines = new RegistrationMachines();
        RegistrationMachine first = new RegistrationMachine("event", 10, 64, 20, BlockFace.NORTH);

        assertTrue(machines.add(first));
        assertFalse(machines.add(new RegistrationMachine("event", 11, 65, 20, BlockFace.NORTH)));
        assertEquals(first, machines.findAt("event", 11, 64, 20).orElseThrow());
        assertTrue(machines.remove(first));
        assertTrue(machines.findAt("event", 10, 64, 20).isEmpty());
    }

    @Test
    void theFileKeepsTheMachinesWithTheirFacing(@TempDir Path folder) throws IOException {
        FileRegistrationMachineStorage storage = new FileRegistrationMachineStorage(
                folder.resolve(FileRegistrationMachineStorage.FILE_NAME));
        List<RegistrationMachine> machines = List.of(
                new RegistrationMachine("event", 10, 64, 20, BlockFace.WEST),
                new RegistrationMachine("arena_flat", -3, -60, 7, BlockFace.NORTH));
        assertEquals(List.of(), storage.load());

        storage.save(machines);

        assertEquals(machines, storage.load());
    }

    private static List<String> describe(RegistrationMachine machine) {
        return machine.blocks().stream().map(block -> block[0] + "/" + block[1] + "/" + block[2]).toList();
    }
}
