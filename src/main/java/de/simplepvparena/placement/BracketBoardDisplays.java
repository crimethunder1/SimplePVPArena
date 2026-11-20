package de.simplepvparena.placement;

import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static de.simplepvparena.placement.BracketBoardGeometry.DIVIDER_BOTTOM;
import static de.simplepvparena.placement.BracketBoardGeometry.FRAME;
import static de.simplepvparena.placement.BracketBoardGeometry.HALF_WIDTH;
import static de.simplepvparena.placement.BracketBoardGeometry.HEADER_SCALE;
import static de.simplepvparena.placement.BracketBoardGeometry.HEADER_TOP;
import static de.simplepvparena.placement.BracketBoardGeometry.HEIGHT;
import static de.simplepvparena.placement.BracketBoardGeometry.TREE_BOTTOM;
import static de.simplepvparena.placement.BracketBoardGeometry.TREE_TOP;

@NullMarked
public final class BracketBoardDisplays {

    public static final String MARKER_KEY_NAME = "bracket_board_display";

    private static final byte MARKER_VALUE = 1;
    private static final int CHUNK_SHIFT = 4;
    private static final Display.Brightness FULL_BRIGHTNESS = new Display.Brightness(15, 15);
    private static final Color NO_BACKGROUND = Color.fromARGB(0, 0, 0, 0);
    private static final int HEADER_LINE_WIDTH = 400;

    private static final String PART_METAL = "metal";
    private static final String PART_TRIM = "trim";
    private static final String PART_PANEL = "panel";
    private static final String PART_GLASS = "glass";
    private static final String PART_LAMP = "lamp";

    private static final float BACK = -1.0F;
    private static final float PANEL_FRONT = -0.82F;
    private static final float GLASS_FRONT = PANEL_FRONT + 0.02F;
    private static final float TRIM_FRONT = GLASS_FRONT + 0.08F;
    private static final float TEXT_Z = GLASS_FRONT + 0.01F;
    private static final float LAMP_SIZE = 0.16F;

    private final NamespacedKey markerKey;
    private final BracketBoards boards;
    private final Function<String, World> worldLookup;
    private final Supplier<BracketBoardView> viewSource;
    private final Function<String, Optional<ItemStack>> partLook;
    private final Map<BracketBoard, BoardDisplay> displays = new LinkedHashMap<>();

    public BracketBoardDisplays(Plugin plugin, BracketBoards boards, Function<String, World> worldLookup,
                                Supplier<BracketBoardView> viewSource,
                                Function<String, Optional<ItemStack>> partLook) {
        this.markerKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), MARKER_KEY_NAME);
        this.boards = Objects.requireNonNull(boards, "boards");
        this.worldLookup = Objects.requireNonNull(worldLookup, "worldLookup");
        this.viewSource = Objects.requireNonNull(viewSource, "viewSource");
        this.partLook = Objects.requireNonNull(partLook, "partLook");
    }

    public void showAll() {
        boards.boards().forEach(this::show);
    }

    public void hideAll() {
        List.copyOf(displays.keySet()).forEach(this::hide);
    }

    public void show(BracketBoard board) {
        hide(board);
        World world = worldLookup.apply(board.worldName());
        if (world == null || !world.isChunkLoaded(board.blockX() >> CHUNK_SHIFT, board.blockZ() >> CHUNK_SHIFT)) {
            return;
        }
        spawn(board, world, viewSource.get());
    }

    public void hide(BracketBoard board) {
        BoardDisplay display = displays.remove(board);
        if (display != null) {
            display.removeAll();
        }
    }

    public void showChunk(Chunk chunk) {
        Set<UUID> own = new HashSet<>();
        displays.values().forEach(display -> display.allEntities().forEach(entity -> own.add(entity.getUniqueId())));
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(markerKey) && !own.contains(entity.getUniqueId())) {
                entity.remove();
            }
        }
        boardsIn(chunk).forEach(this::show);
    }

    public void hideChunk(Chunk chunk) {
        boardsIn(chunk).forEach(this::hide);
    }

    public void refresh() {
        if (displays.isEmpty()) {
            return;
        }
        BracketBoardView view = viewSource.get();
        for (Map.Entry<BracketBoard, BoardDisplay> entry : displays.entrySet()) {
            BoardDisplay display = entry.getValue();
            if (view.equals(display.shownView)) {
                continue;
            }
            Quaternionf turn = turnOf(entry.getKey().facing());
            if (display.header.isValid()) {
                display.header.text(view.header());
                display.header.setTransformation(headerTransformation(turn, view));
            }
            if (display.columns.size() != view.columns().size()
                    || display.columns.stream().anyMatch(column -> !column.isValid())) {
                display.columns.forEach(Entity::remove);
                display.columns.clear();
                display.columns.addAll(spawnColumns(display.origin, turn, view));
            } else {
                for (int index = 0; index < display.columns.size(); index++) {
                    TextDisplay column = display.columns.get(index);
                    column.text(view.columns().get(index));
                    column.setLineWidth(BracketBoardGeometry.lineWidthPixels(view.columns().size(),
                            view.columnLineCount()));
                    column.setTransformation(columnTransformation(turn, index, view));
                }
            }
            display.shownView = view;
        }
    }

    private void spawn(BracketBoard board, World world, BracketBoardView view) {
        Quaternionf turn = turnOf(board.facing());
        Location origin = new Location(world, board.frontCentreX(), board.blockY(), board.frontCentreZ());
        List<Entity> frame = new ArrayList<>();

        frame.add(block(origin, Material.NETHERITE_BLOCK, PART_METAL, box(turn,
                new Vector3f(-HALF_WIDTH + 0.01F, 0.01F, BACK + 0.01F),
                new Vector3f(2 * HALF_WIDTH - 0.02F, HEIGHT - 0.02F, PANEL_FRONT - BACK - 0.01F)), false));
        frame.add(block(origin, Material.BLACK_CONCRETE, PART_GLASS, box(turn,
                new Vector3f(-HALF_WIDTH + FRAME, FRAME, PANEL_FRONT),
                new Vector3f(2 * (HALF_WIDTH - FRAME), HEIGHT - 2 * FRAME, GLASS_FRONT - PANEL_FRONT)), false));
        for (float bottom : new float[] {0.0F, HEIGHT - FRAME}) {
            frame.add(block(origin, Material.IRON_BLOCK, PART_TRIM, box(turn,
                    new Vector3f(-HALF_WIDTH, bottom, BACK), new Vector3f(2 * HALF_WIDTH, FRAME, TRIM_FRONT - BACK)),
                    false));
        }
        for (float left : new float[] {-HALF_WIDTH, HALF_WIDTH - FRAME}) {
            frame.add(block(origin, Material.IRON_BLOCK, PART_TRIM, box(turn,
                    new Vector3f(left, FRAME, BACK), new Vector3f(FRAME, HEIGHT - 2 * FRAME, TRIM_FRONT - BACK)),
                    false));
        }
        frame.add(block(origin, Material.SEA_LANTERN, PART_LAMP, box(turn,
                new Vector3f(-HALF_WIDTH + 0.3F, DIVIDER_BOTTOM, GLASS_FRONT),
                new Vector3f(2 * (HALF_WIDTH - 0.3F), 0.03F, 0.01F)), true));
        frame.add(block(origin, Material.POLISHED_BLACKSTONE, PART_PANEL, box(turn,
                new Vector3f(-1.2F, HEIGHT, BACK + 0.05F), new Vector3f(2.4F, 0.18F, 0.3F)), false));
        for (float lampX : new float[] {-HALF_WIDTH + 0.1F, HALF_WIDTH - 0.1F - LAMP_SIZE}) {
            frame.add(block(origin, Material.SEA_LANTERN, PART_LAMP, box(turn,
                    new Vector3f(lampX, HEIGHT, TRIM_FRONT - LAMP_SIZE - 0.02F),
                    new Vector3f(LAMP_SIZE, LAMP_SIZE, LAMP_SIZE)), true));
        }

        TextDisplay header = world.spawn(origin, TextDisplay.class, text -> {
            prepareText(text);
            text.setAlignment(TextDisplay.TextAlignment.CENTER);
            text.setLineWidth(HEADER_LINE_WIDTH);
            text.text(view.header());
            text.setTransformation(headerTransformation(turn, view));
        });
        List<TextDisplay> columns = new ArrayList<>(spawnColumns(origin, turn, view));
        displays.put(board, new BoardDisplay(origin, List.copyOf(frame), header, columns, view));
    }

    private List<TextDisplay> spawnColumns(Location origin, Quaternionf turn, BracketBoardView view) {
        List<TextDisplay> columns = new ArrayList<>();
        int lineWidth = BracketBoardGeometry.lineWidthPixels(view.columns().size(), view.columnLineCount());
        for (int index = 0; index < view.columns().size(); index++) {
            Component columnText = view.columns().get(index);
            Transformation transformation = columnTransformation(turn, index, view);
            columns.add(origin.getWorld().spawn(origin, TextDisplay.class, text -> {
                prepareText(text);
                text.setAlignment(TextDisplay.TextAlignment.LEFT);
                text.setLineWidth(lineWidth);
                text.text(columnText);
                text.setTransformation(transformation);
            }));
        }
        return columns;
    }

    private void prepareText(TextDisplay text) {
        mark(text);
        text.setBillboard(Display.Billboard.FIXED);
        text.setBackgroundColor(NO_BACKGROUND);
        text.setShadowed(true);
        text.setBrightness(FULL_BRIGHTNESS);
    }

    static Transformation headerTransformation(Quaternionf turn, BracketBoardView view) {
        float height = BracketBoardGeometry.textHeight(view.headerLineCount(), HEADER_SCALE);
        Vector3f translation = turn.transform(new Vector3f(0.0F, HEADER_TOP - height, TEXT_Z));
        return new Transformation(translation, new Quaternionf(turn), new Vector3f(HEADER_SCALE), new Quaternionf());
    }

    static Transformation columnTransformation(Quaternionf turn, int column, BracketBoardView view) {
        float scale = BracketBoardGeometry.treeScale(view.columnLineCount());
        float height = BracketBoardGeometry.textHeight(view.columnLineCount(), scale);
        float bottom = TREE_BOTTOM + (TREE_TOP - TREE_BOTTOM - height) / 2.0F;
        float centre = BracketBoardGeometry.columnCentre(column, view.columns().size());
        Vector3f translation = turn.transform(new Vector3f(centre, bottom, TEXT_Z));
        return new Transformation(translation, new Quaternionf(turn), new Vector3f(scale), new Quaternionf());
    }

    private Entity block(Location origin, Material material, String part, Transformation corner, boolean bright) {
        Optional<ItemStack> look = partLook.apply(part);
        if (look.isPresent()) {
            return origin.getWorld().spawn(origin, ItemDisplay.class, display -> {
                mark(display);
                display.setItemStack(look.get());
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                display.setTransformation(centred(corner));
                if (bright) {
                    display.setBrightness(FULL_BRIGHTNESS);
                }
            });
        }
        return origin.getWorld().spawn(origin, BlockDisplay.class, display -> {
            mark(display);
            display.setBlock(material.createBlockData());
            display.setTransformation(corner);
            if (bright) {
                display.setBrightness(FULL_BRIGHTNESS);
            }
        });
    }

    private static Transformation box(Quaternionf turn, Vector3f corner, Vector3f size) {
        return new Transformation(turn.transform(new Vector3f(corner)), new Quaternionf(turn), size,
                new Quaternionf());
    }

    private static Transformation centred(Transformation corner) {
        Vector3f halfSize = new Vector3f(corner.getScale()).mul(0.5F);
        Vector3f translation = new Vector3f(corner.getTranslation())
                .add(new Quaternionf(corner.getLeftRotation()).transform(halfSize));
        return new Transformation(translation, corner.getLeftRotation(), corner.getScale(),
                corner.getRightRotation());
    }

    static Quaternionf turnOf(BlockFace facing) {
        float degrees = switch (facing) {
            case WEST -> -90.0F;
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
        return new Quaternionf().rotationY((float) Math.toRadians(degrees));
    }

    private List<BracketBoard> boardsIn(Chunk chunk) {
        String worldName = chunk.getWorld().getName();
        List<BracketBoard> inChunk = new ArrayList<>();
        for (BracketBoard board : boards.boards()) {
            if (board.worldName().equals(worldName) && board.blockX() >> CHUNK_SHIFT == chunk.getX()
                    && board.blockZ() >> CHUNK_SHIFT == chunk.getZ()) {
                inChunk.add(board);
            }
        }
        return inChunk;
    }

    private void mark(Entity entity) {
        entity.setPersistent(false);
        entity.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, MARKER_VALUE);
    }

    private static final class BoardDisplay {

        private final Location origin;
        private final List<Entity> frame;
        private final TextDisplay header;
        private final List<TextDisplay> columns;

        private @Nullable BracketBoardView shownView;

        private BoardDisplay(Location origin, List<Entity> frame, TextDisplay header, List<TextDisplay> columns,
                             BracketBoardView shownView) {
            this.origin = origin;
            this.frame = frame;
            this.header = header;
            this.columns = columns;
            this.shownView = shownView;
        }

        private List<Entity> allEntities() {
            List<Entity> entities = new ArrayList<>(frame);
            entities.add(header);
            entities.addAll(columns);
            return entities;
        }

        private void removeAll() {
            allEntities().forEach(Entity::remove);
        }
    }
}
