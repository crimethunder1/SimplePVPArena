package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.RegistrationMessage;
import de.simplepvparena.message.TournamentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
import java.util.function.IntSupplier;

public final class RegistrationMachineDisplays {

    public static final String MARKER_KEY_NAME = "registration_machine_display";

    private static final byte MARKER_VALUE = 1;
    private static final int CHUNK_SHIFT = 4;
    private static final Display.Brightness FULL_BRIGHTNESS = new Display.Brightness(15, 15);

    private static final String PART_METAL = "metal";
    private static final String PART_TRIM = "trim";
    private static final String PART_PANEL = "panel";
    private static final String PART_GLASS = "glass";
    private static final String PART_BUTTON = "button";
    private static final String PART_LAMP = "lamp";
    private static final Material CASING = Material.NETHERITE_BLOCK;
    private static final Material TRIM = Material.IRON_BLOCK;
    private static final Material CONSOLE = Material.POLISHED_DEEPSLATE;
    private static final Material DESK = Material.SMOOTH_STONE;
    private static final Material BEZEL = Material.BLACK_CONCRETE;
    private static final Material BUTTON = Material.RED_CONCRETE;
    private static final Material LAMP = Material.SEA_LANTERN;
    private static final Color BUTTON_GLOW = Color.fromRGB(255, 60, 60);
    private static final Color SCREEN_BACKGROUND = Color.fromARGB(0, 0, 0, 0);

    private static final float INSET = 0.01F;
    private static final float DESK_TILT_DEGREES = 30.0F;
    private static final Vector3f DESK_PIVOT = new Vector3f(0.0F, 0.95F, 0.0F);
    private static final float BUTTON_HALF_WIDTH = 0.16F;
    private static final float BUTTON_HEIGHT = 0.12F;
    private static final float BUTTON_PRESSED_DEPTH = 0.08F;
    private static final float DESK_THICKNESS = 0.08F;
    private static final float DESK_DEPTH = 0.5F;
    private static final float DESK_BODY_THICKNESS = 0.4F;
    private static final float BUTTON_DESK_Z = 0.25F;
    private static final float SCREEN_SCALE = 0.5F;
    private static final int SCREEN_LINE_WIDTH = 170;
    private static final float CLICK_AREA_SIZE = 0.45F;
    private static final int PRESS_TICKS = 3;
    private static final long RELEASE_DELAY_TICKS = 8L;

    private final Plugin plugin;
    private final NamespacedKey markerKey;
    private final RegistrationMachines machines;
    private final Function<String, World> worldLookup;
    private final IntSupplier registeredCount;
    private final Function<String, Optional<ItemStack>> partLook;
    private final Map<RegistrationMachine, MachineDisplay> displays = new LinkedHashMap<>();

    public RegistrationMachineDisplays(Plugin plugin, RegistrationMachines machines,
                                       Function<String, World> worldLookup, IntSupplier registeredCount,
                                       Function<String, Optional<ItemStack>> partLook) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.markerKey = new NamespacedKey(plugin, MARKER_KEY_NAME);
        this.machines = Objects.requireNonNull(machines, "machines");
        this.worldLookup = Objects.requireNonNull(worldLookup, "worldLookup");
        this.registeredCount = Objects.requireNonNull(registeredCount, "registeredCount");
        this.partLook = Objects.requireNonNull(partLook, "partLook");
    }

    public void showAll() {
        machines.machines().forEach(this::show);
    }

    public void hideAll() {
        List.copyOf(displays.keySet()).forEach(this::hide);
    }

    public void show(RegistrationMachine machine) {
        hide(machine);
        World world = worldLookup.apply(machine.worldName());
        if (world == null || !world.isChunkLoaded(machine.blockX() >> CHUNK_SHIFT, machine.blockZ() >> CHUNK_SHIFT)) {
            return;
        }
        spawn(machine, world);
    }

    public void hide(RegistrationMachine machine) {
        MachineDisplay display = displays.remove(machine);
        if (display != null) {
            display.entities.forEach(Entity::remove);
        }
    }

    public void showChunk(Chunk chunk) {
        Set<UUID> own = new HashSet<>();
        displays.values().forEach(display -> display.entities.forEach(entity -> own.add(entity.getUniqueId())));
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(markerKey) && !own.contains(entity.getUniqueId())) {
                entity.remove();
            }
        }
        for (RegistrationMachine machine : machinesIn(chunk)) {
            show(machine);
        }
    }

    public void hideChunk(Chunk chunk) {
        machinesIn(chunk).forEach(this::hide);
    }

    public void updateCounters() {
        int count = registeredCount.getAsInt();
        for (MachineDisplay display : displays.values()) {
            if (display.shownCount != count && display.screen instanceof TextDisplay screen) {
                display.shownCount = count;
                screen.text(screenText(count));
            }
        }
    }

    public Optional<RegistrationMachine> findMachineOfClickArea(Entity entity) {
        for (Map.Entry<RegistrationMachine, MachineDisplay> entry : displays.entrySet()) {
            if (entry.getValue().clickArea.getUniqueId().equals(entity.getUniqueId())) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public void press(RegistrationMachine machine) {
        MachineDisplay display = displays.get(machine);
        if (display == null || !(display.button instanceof Display button) || !button.isValid()) {
            return;
        }
        Quaternionf turn = turnOf(machine.facing());
        boolean custom = button instanceof ItemDisplay;
        animate(button, shaped(buttonTransformation(turn, -BUTTON_PRESSED_DEPTH), custom));
        button.getWorld().playSound(button.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
                1.0F, 1.2F);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (button.isValid()) {
                animate(button, shaped(buttonTransformation(turn, 0.0F), custom));
                button.getWorld().playSound(button.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }, RELEASE_DELAY_TICKS);
    }

    public static Component screenText(int count) {
        return Component.text()
                .append(Component.text(Messages.text(RegistrationMessage.DISPLAY_TITLE),
                        TournamentMessages.PREFIX_COLOUR, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(Messages.text(RegistrationMessage.DISPLAY_HINT), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text(Messages.plural(RegistrationMessage.DISPLAY_COUNT, count),
                        NamedTextColor.GREEN))
                .build();
    }

    private void spawn(RegistrationMachine machine, World world) {
        int count = registeredCount.getAsInt();
        Quaternionf turn = turnOf(machine.facing());
        Location origin = new Location(world, machine.frontCentreX(), machine.blockY(), machine.frontCentreZ());
        Quaternionf flat = new Quaternionf();
        Quaternionf tilt = deskTilt();
        List<Entity> entities = new ArrayList<>();

        entities.add(block(origin, CASING, PART_METAL, part(turn, new Vector3f(), flat,
                new Vector3f(-1.0F + INSET, 0.0F, -1.0F + INSET),
                new Vector3f(2.0F - 2 * INSET, 2.0F - INSET, 1.0F - INSET)), false));
        entities.add(block(origin, TRIM, PART_TRIM, part(turn, new Vector3f(), flat,
                new Vector3f(-1.0F, 1.9F, -1.0F), new Vector3f(2.0F, 0.1F, 1.02F)), false));
        entities.add(block(origin, TRIM, PART_TRIM, part(turn, new Vector3f(), flat,
                new Vector3f(-1.0F, 0.0F, -1.0F), new Vector3f(2.0F, 0.1F, 1.02F)), false));
        entities.add(block(origin, TRIM, PART_TRIM, part(turn, new Vector3f(), flat,
                new Vector3f(-1.0F, 0.1F, -0.02F), new Vector3f(0.08F, 1.8F, 0.04F)), false));
        entities.add(block(origin, TRIM, PART_TRIM, part(turn, new Vector3f(), flat,
                new Vector3f(0.92F, 0.1F, -0.02F), new Vector3f(0.08F, 1.8F, 0.04F)), false));
        entities.add(block(origin, BEZEL, PART_GLASS, part(turn, new Vector3f(), flat,
                new Vector3f(-0.82F, 1.08F, -0.01F), new Vector3f(1.64F, 0.8F, 0.04F)), false));
        entities.add(block(origin, CONSOLE, PART_PANEL, part(turn, new Vector3f(), flat,
                new Vector3f(-0.75F, 0.1F, -0.01F), new Vector3f(1.5F, 0.62F, 0.44F)), false));
        entities.add(block(origin, DESK, PART_PANEL, part(turn, DESK_PIVOT, tilt,
                new Vector3f(-0.75F, DESK_THICKNESS - DESK_BODY_THICKNESS, 0.0F),
                new Vector3f(1.5F, DESK_BODY_THICKNESS, DESK_DEPTH)), false));
        entities.add(block(origin, LAMP, PART_LAMP, part(turn, DESK_PIVOT, tilt,
                new Vector3f(-0.6F, DESK_THICKNESS, 0.2F), new Vector3f(0.1F, 0.05F, 0.1F)), true));
        entities.add(block(origin, LAMP, PART_LAMP, part(turn, DESK_PIVOT, tilt,
                new Vector3f(0.5F, DESK_THICKNESS, 0.2F), new Vector3f(0.1F, 0.05F, 0.1F)), true));

        Entity button = block(origin, BUTTON, PART_BUTTON, buttonTransformation(turn, 0.0F), true);
        button.setGlowing(true);
        if (button instanceof Display display) {
            display.setGlowColorOverride(BUTTON_GLOW);
        }
        entities.add(button);

        Entity screen = world.spawn(origin, TextDisplay.class, display -> {
            mark(display);
            display.setBillboard(Display.Billboard.FIXED);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setLineWidth(SCREEN_LINE_WIDTH);
            display.setBackgroundColor(SCREEN_BACKGROUND);
            display.setShadowed(true);
            display.setBrightness(FULL_BRIGHTNESS);
            display.setTransformation(new Transformation(turn.transform(new Vector3f(0.0F, 1.3F, 0.035F)),
                    new Quaternionf(turn), new Vector3f(SCREEN_SCALE), new Quaternionf()));
            display.text(screenText(count));
        });
        entities.add(screen);

        Vector3f buttonCentre = turn.transform(new Vector3f(DESK_PIVOT).add(tilt.transform(
                new Vector3f(0.0F, DESK_THICKNESS + BUTTON_HEIGHT, BUTTON_DESK_Z))));
        Location clickLocation = origin.clone().add(buttonCentre.x, buttonCentre.y - CLICK_AREA_SIZE / 2,
                buttonCentre.z);
        Entity clickArea = world.spawn(clickLocation, Interaction.class, interaction -> {
            mark(interaction);
            interaction.setInteractionWidth(CLICK_AREA_SIZE);
            interaction.setInteractionHeight(CLICK_AREA_SIZE);
            interaction.setResponsive(true);
        });
        entities.add(clickArea);

        displays.put(machine, new MachineDisplay(List.copyOf(entities), screen, button, clickArea, count));
    }

    private Entity block(Location origin, Material material, String part, Transformation transformation,
                         boolean bright) {
        Optional<ItemStack> look = partLook.apply(part);
        if (look.isPresent()) {
            return origin.getWorld().spawn(origin, ItemDisplay.class, display -> {
                mark(display);
                display.setItemStack(look.get());
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                display.setTransformation(shaped(transformation, true));
                if (bright) {
                    display.setBrightness(FULL_BRIGHTNESS);
                }
            });
        }
        return origin.getWorld().spawn(origin, BlockDisplay.class, display -> {
            mark(display);
            display.setBlock(material.createBlockData());
            display.setTransformation(transformation);
            if (bright) {
                display.setBrightness(FULL_BRIGHTNESS);
            }
        });
    }

    private static Transformation shaped(Transformation cornerBased, boolean centred) {
        if (!centred) {
            return cornerBased;
        }
        Vector3f halfSize = new Vector3f(cornerBased.getScale()).mul(0.5F);
        Vector3f translation = new Vector3f(cornerBased.getTranslation())
                .add(new Quaternionf(cornerBased.getLeftRotation()).transform(halfSize));
        return new Transformation(translation, cornerBased.getLeftRotation(), cornerBased.getScale(),
                cornerBased.getRightRotation());
    }

    private static Transformation buttonTransformation(Quaternionf turn, float pressedDepth) {
        return part(turn, DESK_PIVOT, deskTilt(),
                new Vector3f(-BUTTON_HALF_WIDTH, DESK_THICKNESS + pressedDepth, BUTTON_DESK_Z - BUTTON_HALF_WIDTH),
                new Vector3f(BUTTON_HALF_WIDTH * 2, BUTTON_HEIGHT, BUTTON_HALF_WIDTH * 2));
    }

    private static Transformation part(Quaternionf turn, Vector3f pivot, Quaternionf tilt, Vector3f corner,
                                       Vector3f size) {
        Vector3f translation = turn.transform(new Vector3f(pivot).add(tilt.transform(new Vector3f(corner))));
        return new Transformation(translation, new Quaternionf(turn).mul(tilt), size, new Quaternionf());
    }

    private static Quaternionf deskTilt() {
        return new Quaternionf().rotationX((float) Math.toRadians(DESK_TILT_DEGREES));
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

    private static void animate(Display display, Transformation transformation) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(PRESS_TICKS);
        display.setTransformation(transformation);
    }

    private List<RegistrationMachine> machinesIn(Chunk chunk) {
        String worldName = chunk.getWorld().getName();
        List<RegistrationMachine> inChunk = new ArrayList<>();
        for (RegistrationMachine machine : machines.machines()) {
            if (machine.worldName().equals(worldName) && machine.blockX() >> CHUNK_SHIFT == chunk.getX()
                    && machine.blockZ() >> CHUNK_SHIFT == chunk.getZ()) {
                inChunk.add(machine);
            }
        }
        return inChunk;
    }

    private void mark(Entity entity) {
        entity.setPersistent(false);
        entity.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, MARKER_VALUE);
    }

    private static final class MachineDisplay {

        private final List<Entity> entities;
        private final Entity screen;
        private final Entity button;
        private final Entity clickArea;

        private int shownCount;

        private MachineDisplay(List<Entity> entities, Entity screen, Entity button, Entity clickArea,
                               int shownCount) {
            this.entities = entities;
            this.screen = screen;
            this.button = button;
            this.clickArea = clickArea;
            this.shownCount = shownCount;
        }
    }
}
