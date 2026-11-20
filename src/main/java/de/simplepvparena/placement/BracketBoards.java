package de.simplepvparena.placement;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class BracketBoards {

    private final List<BracketBoard> boards = new ArrayList<>();

    public List<BracketBoard> boards() {
        return List.copyOf(boards);
    }

    public Optional<BracketBoard> findAt(String worldName, int x, int y, int z) {
        return boards.stream().filter(board -> board.contains(worldName, x, y, z)).findFirst();
    }

    public boolean overlapsAny(BracketBoard candidate) {
        return candidate.blocks().stream()
                .anyMatch(block -> findAt(candidate.worldName(), block[0], block[1], block[2]).isPresent());
    }

    public boolean add(BracketBoard board) {
        Objects.requireNonNull(board, "board");
        if (overlapsAny(board)) {
            return false;
        }
        return boards.add(board);
    }

    public boolean remove(BracketBoard board) {
        return boards.remove(board);
    }

    public void replaceAll(List<BracketBoard> loadedBoards) {
        boards.clear();
        loadedBoards.forEach(this::add);
    }
}
