package de.simplepvparena.laser;

import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.match.LaserZoneSettings;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.SuddenDeathZone;
import de.simplepvparena.match.SuddenDeathZoneHost;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Shows the laser ring of sudden death and hurts the fighters who touch it. Poles rise out of the arena floor one
 * after another, lasers shoot from pole to pole as a grid, then the ring closes in slowly. Everything is made of
 * display entities anchored in the arena centre, so the shrinking is a smooth client side interpolation and the
 * fighters are never moved, blinded or interrupted. With ItemsAdder the poles and lasers are the item models
 * laser_pole, laser_pole_glow and laser_beam of the bundled content, without them they are built from blocks.
 */
@NullMarked
public final class LaserZoneService implements SuddenDeathZone {

    public static final String ENTITY_MARKER_KEY_NAME = "laser_zone_entity";
    public static final String POLE_MODEL = "laser_pole";
    public static final String POLE_GLOW_MODEL = "laser_pole_glow";
    public static final String BEAM_MODEL = "laser_beam";
    /** Matches POLE_SCALE of CraftCoreTools/simplepvparena_laser_gen.py, the foot of the model is its anchor. */
    public static final float POLE_MODEL_SCALE = 4.0F;

    private static final byte MARKER_VALUE = 1;
    private static final Display.Brightness FULL_BRIGHTNESS = new Display.Brightness(15, 15);
    private static final float VIEW_RANGE = 2.5F;

    private static final float POLE_HEIGHT = 3.4F;
    private static final float POLE_WIDTH = 0.3F;
    private static final float SOCKET_WIDTH = 0.5F;
    private static final float SOCKET_HEIGHT = 0.25F;
    private static final float CAP_WIDTH = 0.4F;
    private static final float CAP_HEIGHT = 0.2F;
    private static final float STRIP_DEPTH = 0.05F;
    private static final float STRIP_WIDTH = 0.1F;
    private static final float BEAM_THICKNESS = 0.06F;
    private static final float COLLAPSED_BEAM_LENGTH = 0.001F;
    private static final float HIDDEN_DEPTH = POLE_HEIGHT + CAP_HEIGHT + 0.3F;
    private static final float[] BEAM_HEIGHTS = {0.4F, 1.2F, 2.0F, 2.8F};
    private static final float LOW_DIAGONAL_HEIGHT = 0.4F;
    private static final float HIGH_DIAGONAL_HEIGHT = 2.8F;

    private static final int RISE_START_TICK = 2;
    private static final int RISE_SPREAD_TICKS = 24;
    private static final int RISE_DURATION_TICKS = 18;
    private static final int BEAM_SPAWN_TICK = RISE_START_TICK + RISE_SPREAD_TICKS + RISE_DURATION_TICKS + 2;
    private static final int IGNITE_START_TICK = BEAM_SPAWN_TICK + 3;
    private static final int IGNITE_SPREAD_TICKS = 12;
    private static final int IGNITE_DURATION_TICKS = 6;
    static final int ARMED_TICK = IGNITE_START_TICK + IGNITE_SPREAD_TICKS + IGNITE_DURATION_TICKS + 4;
    private static final int SHRINK_UPDATE_PERIOD_TICKS = 10;
    private static final int DAMAGE_CHECK_PERIOD_TICKS = 5;
    private static final int AMBIENT_PARTICLES_PER_TICK = 4;
    private static final int HUM_PERIOD_TICKS = 60;

    private static final Particle.DustOptions LASER_DUST = new Particle.DustOptions(Color.fromRGB(255, 40, 40), 0.8F);
    private static final Particle.DustOptions HIT_DUST = new Particle.DustOptions(Color.fromRGB(255, 90, 60), 1.2F);

    private final Plugin plugin;
    private final Function<String, Optional<ItemStack>> modelLook;
    private final NamespacedKey markerKey;
    private final List<Pole> poles = new ArrayList<>();
    private final List<Beam> beams = new ArrayList<>();
    private final Map<UUID, Long> nextHitTicks = new HashMap<>();

    private @Nullable BukkitTask task;
    private @Nullable SuddenDeathZoneHost host;
    private @Nullable LaserRing ring;
    private @Nullable Location anchor;
    private @Nullable Models models;
    private long elapsedTicks;

    public LaserZoneService(Plugin plugin) {
        this(plugin, model -> Optional.empty());
    }

    public LaserZoneService(Plugin plugin, Function<String, Optional<ItemStack>> modelLook) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.modelLook = Objects.requireNonNull(modelLook, "modelLook");
        this.markerKey = new NamespacedKey(plugin, ENTITY_MARKER_KEY_NAME);
    }

    public boolean isRunning() {
        return task != null;
    }

    @Override
    public boolean start(Match match, LaserZoneSettings settings, SuddenDeathZoneHost host) {
        stop();
        Optional<ArenaShape> shape = match.getArena().getShape();
        if (shape.isEmpty()) {
            plugin.getLogger().info("The laser zone of match " + match.getMatchId()
                    + " is skipped: the arena has no calculated shape");
            return false;
        }
        World world = plugin.getServer().getWorld(shape.get().getWorldName());
        if (world == null) {
            plugin.getLogger().info("The laser zone of match " + match.getMatchId()
                    + " is skipped: the world of the arena is not loaded");
            return false;
        }
        PositionSnapshot centre = shape.get().getCenterPosition();
        double insideRadius = LaserRing.largestRadiusInside(shape.get(), centre.x(), centre.z());
        if (insideRadius < settings.minimumRadius()) {
            plugin.getLogger().info("The laser zone of match " + match.getMatchId() + " is skipped: no circle of "
                    + settings.minimumRadius() + " blocks fits around the arena centre");
            return false;
        }
        removeLeftovers(world);
        LaserRing laserRing = new LaserRing(centre.x(), centre.z(), insideRadius, settings);
        Location ringAnchor = new Location(world, centre.x(), shape.get().getReferenceY(), centre.z());
        this.ring = laserRing;
        this.anchor = ringAnchor;
        this.host = host;
        this.elapsedTicks = 0L;
        this.models = findModels();
        spawnPoles(ringAnchor, LaserRing.poleCountFor(laserRing.startRadius()), laserRing.startRadius());
        playSound(ringAnchor, Sound.BLOCK_BEACON_POWER_SELECT, 3.0F, 0.6F);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickSafely, 1L, 1L);
        plugin.getLogger().info("The laser zone of match " + match.getMatchId() + " rises with " + poles.size()
                + " poles, radius " + Math.round(laserRing.startRadius() * 10.0D) / 10.0D + " shrinking to "
                + settings.minimumRadius() + " in " + settings.shrinkSeconds() + " seconds"
                + (models == null ? ", built from blocks" : ", with the ItemsAdder models"));
        return true;
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Pole pole : poles) {
            pole.parts().forEach(part -> removeEntity(part.display()));
        }
        for (Beam beam : beams) {
            removeEntity(beam.display());
        }
        poles.clear();
        beams.clear();
        nextHitTicks.clear();
        host = null;
        ring = null;
        anchor = null;
        models = null;
    }

    private @Nullable Models findModels() {
        try {
            Optional<ItemStack> pole = modelLook.apply(POLE_MODEL);
            Optional<ItemStack> poleGlow = modelLook.apply(POLE_GLOW_MODEL);
            Optional<ItemStack> beam = modelLook.apply(BEAM_MODEL);
            if (pole.isPresent() && poleGlow.isPresent() && beam.isPresent()) {
                return new Models(pole.get(), poleGlow.get(), beam.get());
            }
        } catch (RuntimeException | LinkageError failure) {
            plugin.getLogger().log(Level.WARNING, "The laser models could not be looked up, the laser zone is "
                    + "built from blocks", failure);
        }
        return null;
    }

    private void tickSafely() {
        SuddenDeathZoneHost currentHost = host;
        try {
            tick();
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING, "The laser zone failed, sudden death damages every fighter instead",
                    failure);
            stop();
            if (currentHost != null) {
                currentHost.zoneFailed();
            }
        }
    }

    private void tick() {
        SuddenDeathZoneHost currentHost = host;
        LaserRing currentRing = ring;
        Location currentAnchor = anchor;
        if (currentHost == null || currentRing == null || currentAnchor == null || currentHost.paused()) {
            return;
        }
        long now = ++elapsedTicks;
        if (now < ARMED_TICK) {
            animateTheRise(now, currentAnchor, currentRing.startRadius());
            return;
        }
        long shrunkTicks = now - ARMED_TICK;
        if (shrunkTicks % SHRINK_UPDATE_PERIOD_TICKS == 0L
                && shrunkTicks <= currentRing.shrinkTicks() + SHRINK_UPDATE_PERIOD_TICKS) {
            double nextRadius = currentRing.radiusAfter(shrunkTicks + SHRINK_UPDATE_PERIOD_TICKS);
            poles.forEach(pole -> placePole(pole, nextRadius, 0.0F, SHRINK_UPDATE_PERIOD_TICKS));
            beams.forEach(beam -> placeBeam(beam, nextRadius, 1.0F, SHRINK_UPDATE_PERIOD_TICKS));
        }
        double radius = currentRing.radiusAfter(shrunkTicks);
        showAmbience(currentAnchor, radius, shrunkTicks);
        if (shrunkTicks % DAMAGE_CHECK_PERIOD_TICKS == 0L) {
            hurtFightersInTheLasers(currentHost, currentRing, currentAnchor.getWorld(), radius, now);
        }
    }

    private void animateTheRise(long now, Location ringAnchor, double radius) {
        int poleCount = poles.size();
        for (int index = 0; index < poleCount; index++) {
            if (now == RISE_START_TICK + spread(index, poleCount, RISE_SPREAD_TICKS)) {
                Pole pole = poles.get(index);
                placePole(pole, radius, 0.0F, RISE_DURATION_TICKS);
                Location base = polePosition(ringAnchor, pole, radius, 0.0D);
                playSound(base, Sound.BLOCK_PISTON_EXTEND, 1.2F, 0.6F);
                dustFromTheFloor(base);
            }
        }
        if (now == BEAM_SPAWN_TICK) {
            spawnBeams(ringAnchor, radius);
        }
        if (now == IGNITE_START_TICK) {
            playSound(ringAnchor, Sound.BLOCK_BEACON_ACTIVATE, 3.0F, 1.4F);
        }
        for (int gap = 0; gap < poleCount; gap++) {
            if (now == IGNITE_START_TICK + spread(gap, poleCount, IGNITE_SPREAD_TICKS)) {
                for (Beam beam : beams) {
                    if (beam.fromPole() == gap) {
                        placeBeam(beam, radius, 1.0F, IGNITE_DURATION_TICKS);
                    }
                }
            }
        }
    }

    private static int spread(int index, int count, int spreadTicks) {
        return count <= 1 ? 0 : index * spreadTicks / (count - 1);
    }

    private void hurtFightersInTheLasers(SuddenDeathZoneHost currentHost, LaserRing currentRing, World world,
                                         double radius, long now) {
        Map<UUID, Double> hits = new LinkedHashMap<>();
        for (UUID fighter : currentHost.activeFighters()) {
            Player player = plugin.getServer().getPlayer(fighter);
            if (player == null || !player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            Location location = player.getLocation();
            OptionalDouble damage = currentRing.damageAt(location.getX(), location.getZ(), radius);
            if (damage.isEmpty() || now < nextHitTicks.getOrDefault(fighter, 0L)) {
                continue;
            }
            nextHitTicks.put(fighter, now + currentRing.damageIntervalTicks());
            hits.put(fighter, damage.getAsDouble());
            showHit(player);
        }
        if (!hits.isEmpty()) {
            currentHost.damage(hits);
        }
    }

    private void showHit(Player player) {
        Location body = player.getLocation().add(0.0D, 1.0D, 0.0D);
        World world = body.getWorld();
        world.spawnParticle(Particle.ELECTRIC_SPARK, body, 18, 0.3D, 0.6D, 0.3D, 0.15D, null, true);
        world.spawnParticle(Particle.DUST, body, 10, 0.3D, 0.6D, 0.3D, 0.0D, HIT_DUST, true);
        world.playSound(body, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6F, 1.8F);
    }

    private void showAmbience(Location ringAnchor, double radius, long shrunkTicks) {
        int poleCount = poles.size();
        if (poleCount == 0) {
            return;
        }
        World world = ringAnchor.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int particle = 0; particle < AMBIENT_PARTICLES_PER_TICK; particle++) {
            int from = random.nextInt(poleCount);
            Pole first = poles.get(from);
            Pole second = poles.get((from + 1) % poleCount);
            float height = BEAM_HEIGHTS[random.nextInt(BEAM_HEIGHTS.length)];
            double along = random.nextDouble();
            Location start = polePosition(ringAnchor, first, radius, height);
            Location end = polePosition(ringAnchor, second, radius, height);
            Location point = start.add(end.subtract(start).multiply(along));
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, LASER_DUST, true);
        }
        if (shrunkTicks % HUM_PERIOD_TICKS == 0L) {
            Pole pole = poles.get(random.nextInt(poleCount));
            playSound(polePosition(ringAnchor, pole, radius, 1.5D), Sound.BLOCK_BEACON_AMBIENT, 1.5F, 1.2F);
        }
    }

    private void dustFromTheFloor(Location base) {
        World world = base.getWorld();
        BlockData floor = world.getBlockAt(base.getBlockX(), base.getBlockY() - 1, base.getBlockZ()).getBlockData();
        if (!floor.getMaterial().isAir()) {
            world.spawnParticle(Particle.BLOCK, base.clone().add(0.0D, 0.1D, 0.0D), 24, 0.3D, 0.05D, 0.3D, 0.0D,
                    floor, true);
        }
        world.spawnParticle(Particle.SMOKE, base.clone().add(0.0D, 0.2D, 0.0D), 8, 0.2D, 0.05D, 0.2D, 0.02D,
                null, true);
    }

    private void spawnPoles(Location ringAnchor, int poleCount, double radius) {
        Models currentModels = models;
        for (int index = 0; index < poleCount; index++) {
            double angle = 2.0D * Math.PI * index / poleCount;
            List<PolePart> parts = currentModels == null ? blockPoleParts(ringAnchor) : List.of(
                    PolePart.model(spawnModel(ringAnchor, currentModels.pole(), false)),
                    PolePart.model(spawnModel(ringAnchor, currentModels.poleGlow(), true)));
            Pole pole = new Pole(angle, parts);
            poles.add(pole);
            placePole(pole, radius, -HIDDEN_DEPTH, 0);
        }
    }

    private List<PolePart> blockPoleParts(Location ringAnchor) {
        return List.of(
                PolePart.block(spawnBlock(ringAnchor, Material.POLISHED_BLACKSTONE, false),
                        new Vector3f(-SOCKET_WIDTH / 2.0F, 0.0F, -SOCKET_WIDTH / 2.0F),
                        new Vector3f(SOCKET_WIDTH, SOCKET_HEIGHT, SOCKET_WIDTH)),
                PolePart.block(spawnBlock(ringAnchor, Material.IRON_BLOCK, false),
                        new Vector3f(-POLE_WIDTH / 2.0F, 0.0F, -POLE_WIDTH / 2.0F),
                        new Vector3f(POLE_WIDTH, POLE_HEIGHT, POLE_WIDTH)),
                PolePart.block(spawnBlock(ringAnchor, Material.RED_CONCRETE, true),
                        new Vector3f(POLE_WIDTH / 2.0F - 0.01F, SOCKET_HEIGHT + 0.1F, -STRIP_WIDTH / 2.0F),
                        new Vector3f(STRIP_DEPTH, POLE_HEIGHT - SOCKET_HEIGHT - 0.3F, STRIP_WIDTH)),
                PolePart.block(spawnBlock(ringAnchor, Material.REDSTONE_BLOCK, true),
                        new Vector3f(-CAP_WIDTH / 2.0F, POLE_HEIGHT, -CAP_WIDTH / 2.0F),
                        new Vector3f(CAP_WIDTH, CAP_HEIGHT, CAP_WIDTH)));
    }

    private void spawnBeams(Location ringAnchor, double radius) {
        int poleCount = poles.size();
        for (int from = 0; from < poleCount; from++) {
            int to = (from + 1) % poleCount;
            for (float height : BEAM_HEIGHTS) {
                addBeam(ringAnchor, radius, from, to, height, height);
            }
            addBeam(ringAnchor, radius, from, to, LOW_DIAGONAL_HEIGHT, HIGH_DIAGONAL_HEIGHT);
            addBeam(ringAnchor, radius, from, to, HIGH_DIAGONAL_HEIGHT, LOW_DIAGONAL_HEIGHT);
        }
    }

    private void addBeam(Location ringAnchor, double radius, int from, int to, float fromHeight, float toHeight) {
        Models currentModels = models;
        Display display = currentModels == null
                ? spawnBlock(ringAnchor, Material.RED_CONCRETE, true)
                : spawnModel(ringAnchor, currentModels.beam(), true);
        Beam beam = new Beam(from, to, fromHeight, toHeight, display, currentModels != null);
        beams.add(beam);
        placeBeam(beam, radius, 0.0F, 0);
    }

    private BlockDisplay spawnBlock(Location ringAnchor, Material material, boolean glowing) {
        return ringAnchor.getWorld().spawn(ringAnchor, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            prepare(display, glowing);
        });
    }

    private ItemDisplay spawnModel(Location ringAnchor, ItemStack model, boolean glowing) {
        return ringAnchor.getWorld().spawn(ringAnchor, ItemDisplay.class, display -> {
            display.setItemStack(model.clone());
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            prepare(display, glowing);
        });
    }

    private void prepare(Display display, boolean glowing) {
        display.setPersistent(false);
        display.setViewRange(VIEW_RANGE);
        display.setShadowRadius(0.0F);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(0);
        if (glowing) {
            display.setBrightness(FULL_BRIGHTNESS);
        }
        display.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, MARKER_VALUE);
    }

    private void placePole(Pole pole, double radius, float heightOffset, int durationTicks) {
        Vector3f base = new Vector3f((float) (radius * Math.cos(pole.angle())), heightOffset,
                (float) (radius * Math.sin(pole.angle())));
        Quaternionf facingTheCentre = new Quaternionf().rotationY((float) (Math.PI - pole.angle()));
        for (PolePart part : pole.parts()) {
            Vector3f corner = part.corner();
            Vector3f size = part.size();
            Transformation transformation = corner == null || size == null
                    ? new Transformation(new Vector3f(base), new Quaternionf(), new Vector3f(POLE_MODEL_SCALE),
                            new Quaternionf())
                    : orientedBox(base, facingTheCentre, corner, size);
            transform(part.display(), transformation, durationTicks);
        }
    }

    private void placeBeam(Beam beam, double radius, float lengthShare, int durationTicks) {
        Pole from = poles.get(beam.fromPole());
        Pole to = poles.get(beam.toPole());
        Vector3f start = new Vector3f((float) (radius * Math.cos(from.angle())), beam.fromHeight(),
                (float) (radius * Math.sin(from.angle())));
        Vector3f end = new Vector3f((float) (radius * Math.cos(to.angle())), beam.toHeight(),
                (float) (radius * Math.sin(to.angle())));
        Vector3f direction = new Vector3f(end).sub(start);
        float length = direction.length();
        if (length < 1.0E-4F) {
            return;
        }
        direction.div(length);
        Quaternionf alongTheBeam = new Quaternionf().rotationTo(1.0F, 0.0F, 0.0F,
                direction.x, direction.y, direction.z);
        float shownLength = Math.max(COLLAPSED_BEAM_LENGTH, length * lengthShare);
        if (beam.model()) {
            Vector3f middle = new Vector3f(direction).mul(shownLength / 2.0F).add(start);
            transform(beam.display(), new Transformation(middle, alongTheBeam,
                    new Vector3f(shownLength, 1.0F, 1.0F), new Quaternionf()), durationTicks);
            return;
        }
        transform(beam.display(), orientedBox(start, alongTheBeam,
                new Vector3f(0.0F, -BEAM_THICKNESS / 2.0F, -BEAM_THICKNESS / 2.0F),
                new Vector3f(shownLength, BEAM_THICKNESS, BEAM_THICKNESS)), durationTicks);
    }

    static Transformation orientedBox(Vector3f pivot, Quaternionf rotation, Vector3f localCorner, Vector3f size) {
        Vector3f rotatedCorner = new Quaternionf(rotation).transform(new Vector3f(localCorner));
        return new Transformation(new Vector3f(pivot).add(rotatedCorner), new Quaternionf(rotation),
                new Vector3f(size), new Quaternionf());
    }

    private static void transform(Display display, Transformation transformation, int durationTicks) {
        if (!display.isValid()) {
            return;
        }
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(durationTicks);
        display.setTransformation(transformation);
    }

    private static Location polePosition(Location ringAnchor, Pole pole, double radius, double height) {
        return ringAnchor.clone().add(radius * Math.cos(pole.angle()), height, radius * Math.sin(pole.angle()));
    }

    private static void playSound(Location location, Sound sound, float volume, float pitch) {
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    private void removeLeftovers(World world) {
        for (Display display : world.getEntitiesByClass(Display.class)) {
            if (display.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) {
                display.remove();
            }
        }
    }

    private static void removeEntity(Entity entity) {
        if (entity.isValid()) {
            entity.remove();
        }
    }

    private record Models(ItemStack pole, ItemStack poleGlow, ItemStack beam) { }

    private record Pole(double angle, List<PolePart> parts) { }

    /** A block part is a box in the frame of the pole, a model part carries the whole pole model. */
    private record PolePart(Display display, @Nullable Vector3f corner, @Nullable Vector3f size) {

        static PolePart block(Display display, Vector3f corner, Vector3f size) {
            return new PolePart(display, corner, size);
        }

        static PolePart model(Display display) {
            return new PolePart(display, null, null);
        }
    }

    private record Beam(int fromPole, int toPole, float fromHeight, float toHeight, Display display,
                        boolean model) { }
}
