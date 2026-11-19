package de.simplepvparena.placement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@NullMarked
public final class PaperPlacementBoardEntities implements PlacementBoardEntities {

    public static final String MARKER_KEY_NAME = "placement_board_display";
    public static final String PART_MARKER_KEY_NAME = "placement_board_part";
    public static final int LINE_WIDTH = 230;

    private static final byte MARKER_VALUE = 1;
    private static final double CENTRE_ABOVE_THE_LOCATION = 4.0D;
    private static final int SIDES = 4;
    private static final float HALF_WIDTH = 2.95F;
    private static final float HALF_HEIGHT = 1.95F;
    private static final float TRIM_HALF_WIDTH = 3.02F;
    private static final float TRIM_HEIGHT = 0.15F;
    private static final float SCREEN_BOTTOM = -1.8F;
    private static final float SCREEN_TOP = 1.8F;
    private static final float TEXT_SCALE = 1.0F;
    private static final float TEXT_PIXELS_PER_BLOCK = 40.0F;
    private static final int TEXT_LINE_PIXELS = 10;
    private static final int TEXT_PADDING_PIXELS = 2;
    private static final int SPIN_PERIOD_TICKS = 20;
    private static final float SPIN_DEGREES_PER_PERIOD = 9.0F;
    private static final double BOB_HEIGHT = 0.15D;
    private static final long FLAME_PERIOD_TICKS = 2L;
    private static final int SMOKE_EVERY_BURNS = 4;
    private static final float CORNER_THRUSTER = 2.3F;
    private static final float CORNER_THRUSTER_SIZE = 1.0F;
    private static final float CENTRE_THRUSTER_SIZE = 1.6F;
    private static final float MOUNT_BOTTOM = -2.2F;
    private static final float NOZZLE_HEIGHT = 0.45F;
    private static final float LIP_HEIGHT = 0.08F;
    private static final float GLOW_HEIGHT = 0.03F;
    private static final Display.Brightness FULL_BRIGHTNESS = new Display.Brightness(15, 15);
    private static final Color NO_BACKGROUND = Color.fromARGB(0, 0, 0, 0);

    private static final String PART_METAL = "metal";
    private static final String PART_TRIM = "trim";
    private static final String PART_GLASS = "glass";
    private static final String PART_LAMP = "lamp";
    private static final String PART_PANEL = "panel";
    private static final String PART_NOZZLE = "nozzle";
    private static final String PART_THRUSTER = "thruster";

    private final Plugin plugin;
    private final NamespacedKey markerKey;
    private final NamespacedKey partKey;
    private final Function<String, Optional<ItemStack>> partLook;
    private final Map<UUID, Cube> cubes = new HashMap<>();

    public PaperPlacementBoardEntities(Plugin plugin) {
        this(plugin, part -> Optional.empty());
    }

    public PaperPlacementBoardEntities(Plugin plugin, Function<String, Optional<ItemStack>> partLook) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.markerKey = new NamespacedKey(plugin, MARKER_KEY_NAME);
        this.partKey = new NamespacedKey(plugin, PART_MARKER_KEY_NAME);
        this.partLook = Objects.requireNonNull(partLook, "partLook");
    }

    @Override
    public Entity spawnBoard(Location location, Component text) {
        Location centre = location.clone().add(0.0D, CENTRE_ABOVE_THE_LOCATION, 0.0D);
        centre.setYaw(0.0F);
        centre.setPitch(0.0F);
        Quaternionf flat = new Quaternionf();
        List<Entity> parts = new ArrayList<>();
        Entity core = part(centre, Material.NETHERITE_BLOCK, PART_METAL, box(flat,
                new Vector3f(-HALF_WIDTH + 0.01F, -HALF_HEIGHT, -HALF_WIDTH + 0.01F),
                new Vector3f(2 * HALF_WIDTH - 0.02F, 2 * HALF_HEIGHT, 2 * HALF_WIDTH - 0.02F)), false, markerKey);
        parts.add(core);
        parts.add(part(centre, Material.POLISHED_BLACKSTONE, PART_PANEL, box(flat,
                new Vector3f(-0.8F, HALF_HEIGHT, -0.8F), new Vector3f(1.6F, 0.2F, 1.6F)), false, partKey));
        parts.add(part(centre, Material.NETHERITE_BLOCK, PART_METAL, box(flat,
                new Vector3f(-0.5F, HALF_HEIGHT + 0.2F, -0.5F), new Vector3f(1.0F, 0.15F, 1.0F)), false, partKey));
        for (float antennaX : new float[] {1.9F, -1.96F}) {
            parts.add(part(centre, Material.IRON_BLOCK, PART_TRIM, box(flat,
                    new Vector3f(antennaX, HALF_HEIGHT, -0.03F), new Vector3f(0.06F, 1.2F, 0.06F)), false, partKey));
            parts.add(part(centre, Material.SEA_LANTERN, PART_LAMP, box(flat,
                    new Vector3f(antennaX - 0.04F, HALF_HEIGHT + 1.2F, -0.07F), new Vector3f(0.14F, 0.14F, 0.14F)),
                    true, partKey));
        }
        addThruster(parts, centre, flat, 0.0F, CENTRE_THRUSTER_SIZE);
        List<TextDisplay> screens = new ArrayList<>();
        for (int side = 0; side < SIDES; side++) {
            Quaternionf turn = sideTurn(side);
            parts.add(part(centre, Material.BLACK_CONCRETE, PART_GLASS, box(turn,
                    new Vector3f(-HALF_WIDTH, SCREEN_BOTTOM, HALF_WIDTH),
                    new Vector3f(2 * HALF_WIDTH, SCREEN_TOP - SCREEN_BOTTOM, 0.06F)), false, partKey));
            parts.add(part(centre, Material.SEA_LANTERN, PART_LAMP, box(turn,
                    new Vector3f(-2.85F, -HALF_HEIGHT, 2.98F), new Vector3f(5.7F, 0.1F, 0.04F)), true, partKey));
            parts.add(part(centre, Material.IRON_BLOCK, PART_TRIM, box(turn,
                    new Vector3f(2.93F, -HALF_HEIGHT, 2.93F), new Vector3f(0.14F, 2 * HALF_HEIGHT, 0.14F)),
                    false, partKey));
            float rimLength = side % 2 == 0 ? 2 * TRIM_HALF_WIDTH : 2 * (TRIM_HALF_WIDTH - TRIM_HEIGHT);
            for (float rimBottom : new float[] {HALF_HEIGHT, -HALF_HEIGHT - TRIM_HEIGHT}) {
                parts.add(part(centre, Material.IRON_BLOCK, PART_TRIM, box(turn,
                        new Vector3f(-rimLength / 2, rimBottom, TRIM_HALF_WIDTH - TRIM_HEIGHT),
                        new Vector3f(rimLength, TRIM_HEIGHT, TRIM_HEIGHT)), false, partKey));
            }
            parts.add(part(centre, Material.SEA_LANTERN, PART_LAMP, box(turn,
                    new Vector3f(2.5F, HALF_HEIGHT, 2.5F), new Vector3f(0.22F, 0.2F, 0.22F)), true, partKey));
            addThruster(parts, centre, turn, CORNER_THRUSTER, CORNER_THRUSTER_SIZE);
            TextDisplay screen = centre.getWorld().spawn(centre, TextDisplay.class, display -> {
                mark(display, partKey);
                display.setBillboard(Display.Billboard.FIXED);
                display.setAlignment(TextDisplay.TextAlignment.CENTER);
                display.setLineWidth(LINE_WIDTH);
                display.setBackgroundColor(NO_BACKGROUND);
                display.setShadowed(true);
                display.setBrightness(FULL_BRIGHTNESS);
                display.text(text);
            });
            screen.setTransformation(screenTransformation(turn, text));
            screens.add(screen);
            parts.add(screen);
        }
        for (Entity entity : parts) {
            if (entity instanceof Display display) {
                display.setTeleportDuration(SPIN_PERIOD_TICKS);
            }
        }
        Cube cube = new Cube(parts, screens, centre);
        cube.spin = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> spin(cube), SPIN_PERIOD_TICKS,
                SPIN_PERIOD_TICKS);
        cube.flames = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> burn(cube), FLAME_PERIOD_TICKS,
                FLAME_PERIOD_TICKS);
        cubes.put(core.getUniqueId(), cube);
        return core;
    }

    @Override
    public void changeText(Entity board, Component text) {
        Cube cube = cubes.get(board.getUniqueId());
        if (cube == null) {
            return;
        }
        for (int side = 0; side < cube.screens.size(); side++) {
            TextDisplay screen = cube.screens.get(side);
            screen.text(text);
            screen.setTransformation(screenTransformation(sideTurn(side), text));
        }
    }

    @Override
    public boolean isBoard(Entity entity) {
        return entity.getPersistentDataContainer().has(markerKey);
    }

    @Override
    public void removeBoard(Entity board) {
        Cube cube = cubes.remove(board.getUniqueId());
        if (cube == null) {
            board.remove();
            return;
        }
        if (cube.spin != null) {
            cube.spin.cancel();
        }
        if (cube.flames != null) {
            cube.flames.cancel();
        }
        cube.parts.forEach(Entity::remove);
    }

    private void spin(Cube cube) {
        cube.yaw = (cube.yaw + SPIN_DEGREES_PER_PERIOD) % 360.0F;
        cube.up = !cube.up;
        double height = cube.centre.getY() + (cube.up ? BOB_HEIGHT : -BOB_HEIGHT);
        for (Entity entity : cube.parts) {
            if (entity.isValid()) {
                Location location = entity.getLocation();
                location.setY(height);
                location.setYaw(cube.yaw);
                entity.teleport(location);
            }
        }
    }

    private void burn(Cube cube) {
        World world = cube.centre.getWorld();
        if (world == null) {
            return;
        }
        boolean smoke = cube.burns++ % SMOKE_EVERY_BURNS == 0;
        Quaternionf turn = new Quaternionf().rotationY((float) Math.toRadians(-cube.yaw));
        List<Vector3f> exits = new ArrayList<>();
        exits.add(new Vector3f(0.0F, exitHeight(CENTRE_THRUSTER_SIZE), 0.0F));
        for (int side = 0; side < SIDES; side++) {
            exits.add(sideTurn(side).transform(new Vector3f(CORNER_THRUSTER, exitHeight(CORNER_THRUSTER_SIZE),
                    CORNER_THRUSTER)));
        }
        for (Vector3f exit : exits) {
            Vector3f turned = turn.transform(exit);
            Location location = cube.centre.clone().add(turned.x, turned.y, turned.z);
            world.spawnParticle(Particle.FLAME, location, 0, 0.0D, -1.0D, 0.0D, 0.18D, null, true);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 0, 0.0D, -1.0D, 0.0D, 0.1D, null, true);
            if (smoke) {
                world.spawnParticle(Particle.SMOKE, location.clone().add(0.0D, -0.6D, 0.0D), 2, 0.1D, 0.1D, 0.1D,
                        0.01D, null, true);
            }
        }
    }

    private void addThruster(List<Entity> parts, Location centre, Quaternionf turn, float offset, float size) {
        float nozzleBottom = MOUNT_BOTTOM - NOZZLE_HEIGHT * size;
        float lipBottom = nozzleBottom - LIP_HEIGHT;
        parts.add(part(centre, Material.NETHERITE_BLOCK, PART_METAL, box(turn,
                new Vector3f(offset - 0.4F * size, MOUNT_BOTTOM, offset - 0.4F * size),
                new Vector3f(0.8F * size, -HALF_HEIGHT - MOUNT_BOTTOM, 0.8F * size)), false, partKey));
        parts.add(part(centre, Material.POLISHED_BLACKSTONE, PART_NOZZLE, box(turn,
                new Vector3f(offset - 0.3F * size, nozzleBottom, offset - 0.3F * size),
                new Vector3f(0.6F * size, NOZZLE_HEIGHT * size, 0.6F * size)), false, partKey));
        parts.add(part(centre, Material.IRON_BLOCK, PART_TRIM, box(turn,
                new Vector3f(offset - 0.35F * size, lipBottom, offset - 0.35F * size),
                new Vector3f(0.7F * size, LIP_HEIGHT, 0.7F * size)), false, partKey));
        parts.add(part(centre, Material.SHROOMLIGHT, PART_THRUSTER, box(turn,
                new Vector3f(offset - 0.25F * size, lipBottom - GLOW_HEIGHT, offset - 0.25F * size),
                new Vector3f(0.5F * size, GLOW_HEIGHT, 0.5F * size)), true, partKey));
    }

    private static float exitHeight(float size) {
        return MOUNT_BOTTOM - NOZZLE_HEIGHT * size - LIP_HEIGHT - GLOW_HEIGHT - 0.05F;
    }

    private static Quaternionf sideTurn(int side) {
        return new Quaternionf().rotationY((float) Math.toRadians(90.0D * side));
    }

    private static Transformation screenTransformation(Quaternionf turn, Component text) {
        String plain = PlainTextComponentSerializer.plainText().serialize(text);
        int lines = (int) plain.chars().filter(character -> character == '\n').count() + 1;
        float height = (lines * TEXT_LINE_PIXELS + TEXT_PADDING_PIXELS) * TEXT_SCALE / TEXT_PIXELS_PER_BLOCK;
        Vector3f translation = turn.transform(new Vector3f(0.0F, SCREEN_TOP - 0.1F - height, HALF_WIDTH + 0.07F));
        return new Transformation(translation, new Quaternionf(turn), new Vector3f(TEXT_SCALE),
                new Quaternionf());
    }

    private Entity part(Location centre, Material fallback, String look, Transformation corner, boolean bright,
                        NamespacedKey key) {
        Optional<ItemStack> item = partLook.apply(look);
        if (item.isPresent()) {
            return centre.getWorld().spawn(centre, ItemDisplay.class, display -> {
                mark(display, key);
                display.setItemStack(item.get());
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                display.setTransformation(centred(corner));
                if (bright) {
                    display.setBrightness(FULL_BRIGHTNESS);
                }
            });
        }
        return centre.getWorld().spawn(centre, BlockDisplay.class, display -> {
            mark(display, key);
            display.setBlock(fallback.createBlockData());
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

    private static void mark(Entity entity, NamespacedKey key) {
        entity.setPersistent(false);
        entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, MARKER_VALUE);
    }

    private static final class Cube {

        private final List<Entity> parts;
        private final List<TextDisplay> screens;
        private final Location centre;

        private @Nullable BukkitTask spin;
        private @Nullable BukkitTask flames;
        private float yaw;
        private boolean up;
        private int burns;

        private Cube(List<Entity> parts, List<TextDisplay> screens, Location centre) {
            this.parts = parts;
            this.screens = screens;
            this.centre = centre;
        }
    }
}
