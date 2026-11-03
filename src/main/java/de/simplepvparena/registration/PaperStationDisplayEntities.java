package de.simplepvparena.registration;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
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
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

public final class PaperStationDisplayEntities implements StationDisplayEntities {

    public static final String MARKER_KEY_NAME = "registration_station_display";

    private static final byte MARKER_VALUE = 1;
    private static final float TEXT_SCALE = 0.45F;
    private static final int TEXT_LINE_WIDTH = 110;
    private static final Color TEXT_BACKGROUND = Color.fromARGB(190, 10, 28, 14);
    private static final float BASE_WIDTH = 0.9F;
    private static final float BASE_HEIGHT = 0.18F;
    private static final float CAP_WIDTH = 0.62F;
    private static final float CAP_HEIGHT = 0.2F;
    private static final float CAP_PRESSED_DEPTH = 0.14F;
    private static final int PRESS_TICKS = 3;
    private static final long RELEASE_DELAY_TICKS = 8L;
    private static final float CLICK_AREA_WIDTH = 1.0F;
    private static final float CLICK_AREA_HEIGHT = 0.5F;
    private static final Color CAP_GLOW = Color.fromRGB(255, 70, 70);
    private static final float COVER_SCALE = 0.8F;
    private static final float COVER_PRESSED_DEPTH = 0.06F;
    private static final double COVER_DISTANCE_FROM_THE_WALL = 0.14D;

    private final Plugin plugin;
    private final NamespacedKey markerKey;

    public PaperStationDisplayEntities(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.markerKey = new NamespacedKey(plugin, MARKER_KEY_NAME);
    }

    @Override
    public Entity spawnItemDisplay(Location location, ItemStack itemStack) {
        return location.getWorld().spawn(location, ItemDisplay.class, display -> {
            mark(display);
            display.setBillboard(Display.Billboard.CENTER);
            display.setItemStack(itemStack);
        });
    }

    @Override
    public Entity spawnTextDisplay(Location location, Component text) {
        return location.getWorld().spawn(location, TextDisplay.class, display -> {
            mark(display);
            display.setBillboard(Display.Billboard.VERTICAL);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setLineWidth(TEXT_LINE_WIDTH);
            display.setBackgroundColor(TEXT_BACKGROUND);
            display.setShadowed(true);
            display.setTransformation(scaled(new Vector3f(), TEXT_SCALE, TEXT_SCALE, TEXT_SCALE));
            display.text(text);
        });
    }

    @Override
    public List<Entity> spawnButton(Location location) {
        Entity base = location.getWorld().spawn(location, BlockDisplay.class, display -> {
            mark(display);
            display.setBlock(Material.POLISHED_BLACKSTONE.createBlockData());
            display.setTransformation(scaled(new Vector3f(-BASE_WIDTH / 2, 0.0F, -BASE_WIDTH / 2),
                    BASE_WIDTH, BASE_HEIGHT, BASE_WIDTH));
        });
        Entity cap = location.getWorld().spawn(location, BlockDisplay.class, display -> {
            mark(display);
            display.setBlock(Material.RED_CONCRETE.createBlockData());
            display.setTransformation(capTransformation(0.0F));
            display.setGlowing(true);
            display.setGlowColorOverride(CAP_GLOW);
        });
        return List.of(base, cap);
    }

    @Override
    public Entity spawnClickArea(Location location) {
        return location.getWorld().spawn(location, Interaction.class, interaction -> {
            mark(interaction);
            interaction.setInteractionWidth(CLICK_AREA_WIDTH);
            interaction.setInteractionHeight(CLICK_AREA_HEIGHT);
            interaction.setResponsive(true);
        });
    }

    @Override
    public Entity spawnButtonCover(Block buttonBlock, ItemStack image) {
        BlockFace facing = BlockFace.UP;
        FaceAttachable.AttachedFace attachedFace = FaceAttachable.AttachedFace.FLOOR;
        BlockData blockData = buttonBlock.getBlockData();
        if (blockData instanceof Directional directional) {
            facing = directional.getFacing();
        }
        if (blockData instanceof FaceAttachable faceAttachable) {
            attachedFace = faceAttachable.getAttachedFace();
        }
        BlockFace outwards = switch (attachedFace) {
            case FLOOR -> BlockFace.UP;
            case CEILING -> BlockFace.DOWN;
            case WALL -> facing;
        };
        Location location = buttonBlock.getLocation().add(0.5D, 0.5D, 0.5D)
                .subtract(outwards.getDirection().multiply(0.5D - COVER_DISTANCE_FROM_THE_WALL));
        location.setYaw(yawOf(facing));
        location.setPitch(outwards == BlockFace.UP ? -90.0F : outwards == BlockFace.DOWN ? 90.0F : 0.0F);
        return buttonBlock.getWorld().spawn(location, ItemDisplay.class, display -> {
            mark(display);
            display.setItemStack(image);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setTransformation(coverTransformation(0.0F));
        });
    }

    @Override
    public void changeText(Entity display, Component text) {
        if (display instanceof TextDisplay textDisplay) {
            textDisplay.text(text);
        }
    }

    @Override
    public void pressButton(Entity cap) {
        if (!(cap instanceof Display display) || !display.isValid()) {
            return;
        }
        boolean cover = display instanceof ItemDisplay;
        animate(display, cover ? coverTransformation(-COVER_PRESSED_DEPTH) : capTransformation(-CAP_PRESSED_DEPTH));
        display.getWorld().playSound(display.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
                1.0F, 1.2F);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (display.isValid()) {
                animate(display, cover ? coverTransformation(0.0F) : capTransformation(0.0F));
                display.getWorld().playSound(display.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }, RELEASE_DELAY_TICKS);
    }

    private static float yawOf(BlockFace facing) {
        return switch (facing) {
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }

    private static Transformation coverTransformation(float offsetZ) {
        return scaled(new Vector3f(0.0F, 0.0F, offsetZ), COVER_SCALE, COVER_SCALE, COVER_SCALE);
    }

    @Override
    public boolean isStationDisplay(Entity entity) {
        return (entity instanceof Display || entity instanceof Interaction)
                && entity.getPersistentDataContainer().has(markerKey);
    }

    private static void animate(Display display, Transformation transformation) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(PRESS_TICKS);
        display.setTransformation(transformation);
    }

    private static Transformation capTransformation(float offsetY) {
        return scaled(new Vector3f(-CAP_WIDTH / 2, BASE_HEIGHT + offsetY, -CAP_WIDTH / 2),
                CAP_WIDTH, CAP_HEIGHT, CAP_WIDTH);
    }

    private static Transformation scaled(Vector3f translation, float scaleX, float scaleY, float scaleZ) {
        return new Transformation(translation, new AxisAngle4f(), new Vector3f(scaleX, scaleY, scaleZ),
                new AxisAngle4f());
    }

    private void mark(Entity entity) {
        entity.setPersistent(false);
        entity.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, MARKER_VALUE);
    }
}
