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
import java.util.Iterator;
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
 * after another, lasers shoot from pole to pole as a grid, then the ring closes in slowly. The poles are stacks of
 * sections: when a fighter climbs higher than the grid reaches, one more section extends out of the top of every
 * pole with its own row of lasers, and once nobody is up there any more the top section retracts again. Everything
 * is made of display entities anchored in the arena centre, so the shrinking and the extending are smooth client side
 * interpolations and the fighters are never moved, blinded or interrupted. With ItemsAdder the parts are the item
 * models laser_pole_base, laser_pole_section, laser_pole_cap (each with a _glow model) and laser_beam of the bundled
 * content, without them they are built from blocks.
 */
@NullMarked
public final class LaserZoneService implements SuddenDeathZone {

    public static final String ENTITY_MARKER_KEY_NAME = "laser_zone_entity";
    public static final String BASE_MODEL = "laser_pole_base";
    public static final String SECTION_MODEL = "laser_pole_section";
    public static final String CAP_MODEL = "laser_pole_cap";
    public static final String GLOW_SUFFIX = "_glow";
    public static final String BEAM_MODEL = "laser_beam";
    /** Matches POLE_SCALE of CraftCoreTools/simplepvparena_laser_gen.py, the bottom of every part is its anchor. */
    public static final float POLE_MODEL_SCALE = 4.0F;
    /** Matches SECTION_HEIGHT of CraftCoreTools/simplepvparena_laser_gen.py. */
    public static final float SECTION_HEIGHT = 3.2F;
    /** Most sections a pole extends to, about 38 blocks, so a very high tower does not flood the clients. */
    public static final int MAXIMUM_SECTIONS = 12;
    /** Blocks the grid reaches above the feet of the highest fighter: the head plus a little, so a jump on the
     * ground does not extend the poles yet. */
    public static final double CLEARANCE_ABOVE_THE_HIGHEST_FIGHTER = 1.9D;

    private static final byte MARKER_VALUE = 1;
    private static final Display.Brightness FULL_BRIGHTNESS = new Display.Brightness(15, 15);
    private static final float VIEW_RANGE = 3.0F;

    private static final float POLE_WIDTH = 0.3F;
    private static final float SOCKET_WIDTH = 0.5F;
    private static final float SOCKET_HEIGHT = 0.25F;
    private static final float CAP_WIDTH = 0.4F;
    private static final float CAP_HEIGHT = 0.3F;
    private static final float STRIP_DEPTH = 0.05F;
    private static final float STRIP_WIDTH = 0.1F;
    private static final float BEAM_THICKNESS = 0.06F;
    private static final float COLLAPSED = 0.001F;
    private static final float HIDDEN_DEPTH = SECTION_HEIGHT + CAP_HEIGHT + 0.4F;
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
    private static final int HEIGHT_CHECK_PERIOD_TICKS = 5;
    private static final int EXTEND_DURATION_TICKS = 8;
    private static final int RETRACT_BEAMS_TICKS = 4;
    private static final int RETRACT_DURATION_TICKS = 8;
    private static final int STEP_TICKS = 16;
    private static final int RETRACT_DELAY_TICKS = 60;
    private static final int AMBIENT_PARTICLES_PER_TICK = 4;
    private static final int HUM_PERIOD_TICKS = 60;

    private static final Particle.DustOptions LASER_DUST = new Particle.DustOptions(Color.fromRGB(255, 40, 40), 0.8F);
    private static final Particle.DustOptions HIT_DUST = new Particle.DustOptions(Color.fromRGB(255, 90, 60), 1.2F);

    private final Plugin plugin;
    private final Function<String, Optional<ItemStack>> modelLook;
    private final NamespacedKey markerKey;
    private final List<Section> sections = new ArrayList<>();
    private final List<List<PolePart>> capsByPole = new ArrayList<>();
    private final Map<UUID, Long> nextHitTicks = new HashMap<>();
    private final List<ScheduledStep> scheduledSteps = new ArrayList<>();

    private double[] poleAngles = new double[0];
    private float[] poleHeightOffsets = new float[0];
    private float capHeight;
    private double shownRadius;
    private int maximumSections = 1;
    private long busyUntilTick;
    private long tooHighSinceTick = -1L;

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

    /** Sections a pole needs so the grid reaches above a fighter whose feet are the given height above the ground. */
    static int sectionsFor(double highestFeetAboveTheGround, int maximumSections) {
        double neededHeight = highestFeetAboveTheGround + CLEARANCE_ABOVE_THE_HIGHEST_FIGHTER;
        int needed = (int) Math.ceil(neededHeight / SECTION_HEIGHT);
        return Math.max(1, Math.min(Math.max(1, maximumSections), needed));
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
        int groundY = findGroundY(world, shape.get(), laserRing);
        Location ringAnchor = new Location(world, centre.x(), groundY, centre.z());
        this.ring = laserRing;
        this.anchor = ringAnchor;
        this.host = host;
        this.elapsedTicks = 0L;
        this.busyUntilTick = 0L;
        this.tooHighSinceTick = -1L;
        this.shownRadius = laserRing.startRadius();
        this.maximumSections = Math.max(1, Math.min(MAXIMUM_SECTIONS,
                (int) ((world.getMaxHeight() - groundY - CAP_HEIGHT) / SECTION_HEIGHT)));
        this.models = findModels();
        spawnPoles(ringAnchor, LaserRing.poleCountFor(laserRing.startRadius()));
        playSound(ringAnchor, Sound.BLOCK_BEACON_POWER_SELECT, 3.0F, 0.6F);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickSafely, 1L, 1L);
        plugin.getLogger().info("The laser zone of match " + match.getMatchId() + " rises with " + poleAngles.length
                + " poles at Y " + groundY + ", radius " + Math.round(laserRing.startRadius() * 10.0D) / 10.0D
                + " shrinking to " + settings.minimumRadius() + " in " + settings.shrinkSeconds() + " seconds"
                + (models == null ? ", built from blocks" : ", with the ItemsAdder models"));
        return true;
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Section section : sections) {
            section.removeEntities();
        }
        for (List<PolePart> cap : capsByPole) {
            cap.forEach(part -> removeEntity(part.display()));
        }
        sections.clear();
        capsByPole.clear();
        scheduledSteps.clear();
        nextHitTicks.clear();
        poleAngles = new double[0];
        poleHeightOffsets = new float[0];
        host = null;
        ring = null;
        anchor = null;
        models = null;
    }

    private @Nullable Models findModels() {
        try {
            Optional<ItemStack> base = modelLook.apply(BASE_MODEL);
            Optional<ItemStack> baseGlow = modelLook.apply(BASE_MODEL + GLOW_SUFFIX);
            Optional<ItemStack> section = modelLook.apply(SECTION_MODEL);
            Optional<ItemStack> sectionGlow = modelLook.apply(SECTION_MODEL + GLOW_SUFFIX);
            Optional<ItemStack> cap = modelLook.apply(CAP_MODEL);
            Optional<ItemStack> capGlow = modelLook.apply(CAP_MODEL + GLOW_SUFFIX);
            Optional<ItemStack> beam = modelLook.apply(BEAM_MODEL);
            if (base.isPresent() && baseGlow.isPresent() && section.isPresent() && sectionGlow.isPresent()
                    && cap.isPresent() && capGlow.isPresent() && beam.isPresent()) {
                return new Models(base.get(), baseGlow.get(), section.get(), sectionGlow.get(), cap.get(),
                        capGlow.get(), beam.get());
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
        runScheduledSteps(now);
        if (now < ARMED_TICK) {
            animateTheRise(now, currentAnchor);
            return;
        }
        long shrunkTicks = now - ARMED_TICK;
        if (shrunkTicks % SHRINK_UPDATE_PERIOD_TICKS == 0L
                && shrunkTicks <= currentRing.shrinkTicks() + SHRINK_UPDATE_PERIOD_TICKS) {
            shownRadius = currentRing.radiusAfter(shrunkTicks + SHRINK_UPDATE_PERIOD_TICKS);
            placeEverything(SHRINK_UPDATE_PERIOD_TICKS);
        }
        if (shrunkTicks % HEIGHT_CHECK_PERIOD_TICKS == 0L) {
            followTheHighestFighter(currentHost, currentAnchor, now);
        }
        double radius = currentRing.radiusAfter(shrunkTicks);
        showAmbience(currentAnchor, radius, shrunkTicks);
        if (shrunkTicks % DAMAGE_CHECK_PERIOD_TICKS == 0L) {
            hurtFightersInTheLasers(currentHost, currentRing, currentAnchor.getWorld(), radius, now);
        }
    }

    private void runScheduledSteps(long now) {
        if (scheduledSteps.isEmpty()) {
            return;
        }
        List<ScheduledStep> due = new ArrayList<>();
        for (Iterator<ScheduledStep> iterator = scheduledSteps.iterator(); iterator.hasNext(); ) {
            ScheduledStep step = iterator.next();
            if (step.tick() <= now) {
                due.add(step);
                iterator.remove();
            }
        }
        due.forEach(step -> step.action().run());
    }

    private void schedule(long tick, Runnable action) {
        scheduledSteps.add(new ScheduledStep(tick, action));
    }

    private void animateTheRise(long now, Location ringAnchor) {
        int poleCount = poleAngles.length;
        for (int index = 0; index < poleCount; index++) {
            if (now == RISE_START_TICK + spread(index, poleCount, RISE_SPREAD_TICKS)) {
                poleHeightOffsets[index] = 0.0F;
                placePole(index, RISE_DURATION_TICKS);
                Location base = polePosition(ringAnchor, index, shownRadius, 0.0D);
                playSound(base, Sound.BLOCK_PISTON_EXTEND, 1.2F, 0.6F);
                dustFromTheFloor(base);
            }
        }
        Section base = sections.getFirst();
        if (now == BEAM_SPAWN_TICK) {
            spawnBeams(ringAnchor, base);
        }
        if (now == IGNITE_START_TICK) {
            base.beamShare = 1.0F;
            playSound(ringAnchor, Sound.BLOCK_BEACON_ACTIVATE, 3.0F, 1.4F);
        }
        for (int gap = 0; gap < poleCount; gap++) {
            if (now == IGNITE_START_TICK + spread(gap, poleCount, IGNITE_SPREAD_TICKS)) {
                for (Beam beam : base.beams) {
                    if (beam.fromPole() == gap) {
                        placeBeam(beam, 1.0F, IGNITE_DURATION_TICKS);
                    }
                }
            }
        }
    }

    /**
     * Extends the poles by one section when a fighter climbs above the grid, and retracts the top section once
     * nobody has needed it for a while. One step at a time, each step takes a little less than a second.
     */
    private void followTheHighestFighter(SuddenDeathZoneHost currentHost, Location ringAnchor, long now) {
        if (now < busyUntilTick) {
            return;
        }
        double highestFeet = 0.0D;
        for (UUID fighter : currentHost.activeFighters()) {
            Player player = plugin.getServer().getPlayer(fighter);
            if (player != null && player.isOnline() && !player.isDead()
                    && player.getWorld().equals(ringAnchor.getWorld())) {
                highestFeet = Math.max(highestFeet, player.getLocation().getY() - ringAnchor.getY());
            }
        }
        int wanted = sectionsFor(highestFeet, maximumSections);
        int shown = sections.size();
        if (wanted > shown) {
            tooHighSinceTick = -1L;
            extend(ringAnchor, now);
            return;
        }
        if (wanted >= shown) {
            tooHighSinceTick = -1L;
            return;
        }
        if (tooHighSinceTick < 0L) {
            tooHighSinceTick = now;
            return;
        }
        if (now - tooHighSinceTick >= RETRACT_DELAY_TICKS) {
            retract(ringAnchor, now);
        }
    }

    private void extend(Location ringAnchor, long now) {
        int level = sections.size();
        Section section = spawnSection(ringAnchor, level);
        sections.add(section);
        placeSection(section, 0);
        busyUntilTick = now + STEP_TICKS;
        schedule(now + 1L, () -> {
            section.scale = 1.0F;
            capHeight = (level + 1) * SECTION_HEIGHT;
            placeSection(section, EXTEND_DURATION_TICKS);
            placeCaps(EXTEND_DURATION_TICKS);
            playSound(polePosition(ringAnchor, 0, shownRadius, capHeight), Sound.BLOCK_PISTON_EXTEND, 1.5F, 0.8F);
        });
        schedule(now + 1L + EXTEND_DURATION_TICKS, () -> spawnBeams(ringAnchor, section));
        schedule(now + 3L + EXTEND_DURATION_TICKS, () -> {
            section.beamShare = 1.0F;
            section.beams.forEach(beam -> placeBeam(beam, 1.0F, IGNITE_DURATION_TICKS));
            playSound(polePosition(ringAnchor, 0, shownRadius, capHeight), Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 1.8F);
        });
    }

    private void retract(Location ringAnchor, long now) {
        if (sections.size() <= 1) {
            return;
        }
        Section section = sections.getLast();
        int level = sections.size() - 1;
        busyUntilTick = now + STEP_TICKS;
        tooHighSinceTick = -1L;
        section.beamShare = 0.0F;
        section.beams.forEach(beam -> placeBeam(beam, 0.0F, RETRACT_BEAMS_TICKS));
        schedule(now + RETRACT_BEAMS_TICKS + 1L, () -> {
            section.beams.forEach(beam -> removeEntity(beam.display()));
            section.beams.clear();
            section.scale = COLLAPSED;
            capHeight = level * SECTION_HEIGHT;
            placeSection(section, RETRACT_DURATION_TICKS);
            placeCaps(RETRACT_DURATION_TICKS);
            playSound(polePosition(ringAnchor, 0, shownRadius, capHeight), Sound.BLOCK_PISTON_CONTRACT, 1.5F, 0.8F);
        });
        schedule(now + RETRACT_BEAMS_TICKS + RETRACT_DURATION_TICKS + 2L, () -> {
            section.removeEntities();
            sections.remove(section);
        });
    }

    /**
     * Height the poles stand on: the ground of the fight area where they rise. The reference height of the shape is
     * only where the admin stood during the calculation, often on the wall, so the ground is looked up in the world:
     * the most common height of the topmost solid block under the circle and in the centre.
     */
    private static int findGroundY(World world, ArenaShape shape, LaserRing laserRing) {
        int sampleCount = LaserRing.poleCountFor(laserRing.startRadius());
        Map<Integer, Integer> votes = new HashMap<>();
        for (int sample = 0; sample <= sampleCount; sample++) {
            double distance = sample == sampleCount ? 0.0D : laserRing.startRadius();
            double angle = 2.0D * Math.PI * sample / sampleCount;
            int blockX = (int) Math.floor(laserRing.centreX() + distance * Math.cos(angle));
            int blockZ = (int) Math.floor(laserRing.centreZ() + distance * Math.sin(angle));
            for (int blockY = shape.getWallTopY(); blockY >= shape.getFloorY(); blockY--) {
                if (world.getBlockAt(blockX, blockY, blockZ).getType().isSolid()) {
                    votes.merge(blockY + 1, 1, Integer::sum);
                    break;
                }
            }
        }
        return votes.entrySet().stream()
                .max(Map.Entry.<Integer, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .orElse(shape.getFloorY() + 1);
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
        int poleCount = poleAngles.length;
        if (poleCount == 0 || sections.isEmpty()) {
            return;
        }
        World world = ringAnchor.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int particle = 0; particle < AMBIENT_PARTICLES_PER_TICK; particle++) {
            int from = random.nextInt(poleCount);
            int level = random.nextInt(sections.size());
            double height = level * SECTION_HEIGHT + BEAM_HEIGHTS[random.nextInt(BEAM_HEIGHTS.length)];
            double along = random.nextDouble();
            Location start = polePosition(ringAnchor, from, radius, height);
            Location end = polePosition(ringAnchor, (from + 1) % poleCount, radius, height);
            Location point = start.add(end.subtract(start).multiply(along));
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, LASER_DUST, true);
        }
        if (shrunkTicks % HUM_PERIOD_TICKS == 0L) {
            playSound(polePosition(ringAnchor, random.nextInt(poleCount), radius, 1.5D), Sound.BLOCK_BEACON_AMBIENT,
                    1.5F, 1.2F);
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

    private void spawnPoles(Location ringAnchor, int poleCount) {
        poleAngles = new double[poleCount];
        poleHeightOffsets = new float[poleCount];
        for (int index = 0; index < poleCount; index++) {
            poleAngles[index] = 2.0D * Math.PI * index / poleCount;
            poleHeightOffsets[index] = -HIDDEN_DEPTH;
        }
        Section base = spawnSection(ringAnchor, 0);
        base.scale = 1.0F;
        sections.add(base);
        capHeight = SECTION_HEIGHT;
        Models currentModels = models;
        for (int index = 0; index < poleCount; index++) {
            capsByPole.add(currentModels == null
                    ? List.of(PolePart.block(spawnBlock(ringAnchor, Material.REDSTONE_BLOCK, true),
                            new Vector3f(-CAP_WIDTH / 2.0F, 0.0F, -CAP_WIDTH / 2.0F),
                            new Vector3f(CAP_WIDTH, CAP_HEIGHT, CAP_WIDTH)))
                    : List.of(PolePart.model(spawnModel(ringAnchor, currentModels.cap(), false)),
                            PolePart.model(spawnModel(ringAnchor, currentModels.capGlow(), true))));
        }
        for (int index = 0; index < poleCount; index++) {
            placePole(index, 0);
        }
    }

    private Section spawnSection(Location ringAnchor, int level) {
        Models currentModels = models;
        Section section = new Section(level);
        for (int index = 0; index < poleAngles.length; index++) {
            if (currentModels != null) {
                ItemStack body = level == 0 ? currentModels.base() : currentModels.section();
                ItemStack glow = level == 0 ? currentModels.baseGlow() : currentModels.sectionGlow();
                section.partsByPole.add(List.of(PolePart.model(spawnModel(ringAnchor, body, false)),
                        PolePart.model(spawnModel(ringAnchor, glow, true))));
                continue;
            }
            List<PolePart> parts = new ArrayList<>();
            float shaftBottom = level == 0 ? SOCKET_HEIGHT : 0.0F;
            if (level == 0) {
                parts.add(PolePart.block(spawnBlock(ringAnchor, Material.POLISHED_BLACKSTONE, false),
                        new Vector3f(-SOCKET_WIDTH / 2.0F, 0.0F, -SOCKET_WIDTH / 2.0F),
                        new Vector3f(SOCKET_WIDTH, SOCKET_HEIGHT, SOCKET_WIDTH)));
            }
            parts.add(PolePart.block(spawnBlock(ringAnchor, Material.IRON_BLOCK, false),
                    new Vector3f(-POLE_WIDTH / 2.0F, shaftBottom, -POLE_WIDTH / 2.0F),
                    new Vector3f(POLE_WIDTH, SECTION_HEIGHT - shaftBottom, POLE_WIDTH)));
            parts.add(PolePart.block(spawnBlock(ringAnchor, Material.RED_CONCRETE, true),
                    new Vector3f(POLE_WIDTH / 2.0F - 0.01F, shaftBottom + 0.1F, -STRIP_WIDTH / 2.0F),
                    new Vector3f(STRIP_DEPTH, SECTION_HEIGHT - shaftBottom - 0.2F, STRIP_WIDTH)));
            section.partsByPole.add(List.copyOf(parts));
        }
        return section;
    }

    private void spawnBeams(Location ringAnchor, Section section) {
        int poleCount = poleAngles.length;
        float bottom = section.level * SECTION_HEIGHT;
        for (int from = 0; from < poleCount; from++) {
            int to = (from + 1) % poleCount;
            for (float height : BEAM_HEIGHTS) {
                addBeam(ringAnchor, section, from, to, bottom + height, bottom + height);
            }
            addBeam(ringAnchor, section, from, to, bottom + LOW_DIAGONAL_HEIGHT, bottom + HIGH_DIAGONAL_HEIGHT);
            addBeam(ringAnchor, section, from, to, bottom + HIGH_DIAGONAL_HEIGHT, bottom + LOW_DIAGONAL_HEIGHT);
        }
    }

    private void addBeam(Location ringAnchor, Section section, int from, int to, float fromHeight, float toHeight) {
        Models currentModels = models;
        Display display = currentModels == null
                ? spawnBlock(ringAnchor, Material.RED_CONCRETE, true)
                : spawnModel(ringAnchor, currentModels.beam(), true);
        Beam beam = new Beam(from, to, fromHeight, toHeight, display, currentModels != null);
        section.beams.add(beam);
        placeBeam(beam, 0.0F, 0);
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

    private void placeEverything(int durationTicks) {
        for (int index = 0; index < poleAngles.length; index++) {
            placePole(index, durationTicks);
        }
        for (Section section : sections) {
            section.beams.forEach(beam -> placeBeam(beam, section.beamShare, durationTicks));
        }
    }

    private void placePole(int pole, int durationTicks) {
        for (Section section : sections) {
            placeSectionOfPole(section, pole, durationTicks);
        }
        placeCap(pole, durationTicks);
    }

    private void placeSection(Section section, int durationTicks) {
        for (int index = 0; index < poleAngles.length; index++) {
            placeSectionOfPole(section, index, durationTicks);
        }
    }

    private void placeCaps(int durationTicks) {
        for (int index = 0; index < poleAngles.length; index++) {
            placeCap(index, durationTicks);
        }
    }

    private void placeSectionOfPole(Section section, int pole, int durationTicks) {
        if (pole >= section.partsByPole.size()) {
            return;
        }
        float bottom = poleHeightOffsets[pole] + section.level * SECTION_HEIGHT;
        for (PolePart part : section.partsByPole.get(pole)) {
            placePart(part, pole, bottom, section.scale, durationTicks);
        }
    }

    private void placeCap(int pole, int durationTicks) {
        if (pole >= capsByPole.size()) {
            return;
        }
        for (PolePart part : capsByPole.get(pole)) {
            placePart(part, pole, poleHeightOffsets[pole] + capHeight, 1.0F, durationTicks);
        }
    }

    private void placePart(PolePart part, int pole, float bottom, float heightScale, int durationTicks) {
        double angle = poleAngles[pole];
        Vector3f base = new Vector3f((float) (shownRadius * Math.cos(angle)), bottom,
                (float) (shownRadius * Math.sin(angle)));
        Vector3f corner = part.corner();
        Vector3f size = part.size();
        float scaleY = Math.max(COLLAPSED, heightScale);
        Transformation transformation;
        if (corner == null || size == null) {
            transformation = new Transformation(base, new Quaternionf(),
                    new Vector3f(POLE_MODEL_SCALE, POLE_MODEL_SCALE * scaleY, POLE_MODEL_SCALE), new Quaternionf());
        } else {
            Quaternionf facingTheCentre = new Quaternionf().rotationY((float) (Math.PI - angle));
            transformation = orientedBox(base, facingTheCentre, new Vector3f(corner.x, corner.y * scaleY, corner.z),
                    new Vector3f(size.x, size.y * scaleY, size.z));
        }
        transform(part.display(), transformation, durationTicks);
    }

    private void placeBeam(Beam beam, float lengthShare, int durationTicks) {
        double fromAngle = poleAngles[beam.fromPole()];
        double toAngle = poleAngles[beam.toPole()];
        Vector3f start = new Vector3f((float) (shownRadius * Math.cos(fromAngle)), beam.fromHeight(),
                (float) (shownRadius * Math.sin(fromAngle)));
        Vector3f end = new Vector3f((float) (shownRadius * Math.cos(toAngle)), beam.toHeight(),
                (float) (shownRadius * Math.sin(toAngle)));
        Vector3f direction = new Vector3f(end).sub(start);
        float length = direction.length();
        if (length < 1.0E-4F) {
            return;
        }
        direction.div(length);
        Quaternionf alongTheBeam = new Quaternionf().rotationTo(1.0F, 0.0F, 0.0F,
                direction.x, direction.y, direction.z);
        float shownLength = Math.max(COLLAPSED, length * lengthShare);
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

    private Location polePosition(Location ringAnchor, int pole, double radius, double height) {
        double angle = poleAngles[pole];
        return ringAnchor.clone().add(radius * Math.cos(angle), height, radius * Math.sin(angle));
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

    private record Models(ItemStack base, ItemStack baseGlow, ItemStack section, ItemStack sectionGlow,
                          ItemStack cap, ItemStack capGlow, ItemStack beam) { }

    /** One level of every pole with its row of lasers, level 0 is the foot. */
    private static final class Section {

        private final int level;
        private final List<List<PolePart>> partsByPole = new ArrayList<>();
        private final List<Beam> beams = new ArrayList<>();
        private float scale = COLLAPSED;
        private float beamShare;

        private Section(int level) {
            this.level = level;
        }

        private void removeEntities() {
            for (List<PolePart> parts : partsByPole) {
                parts.forEach(part -> removeEntity(part.display()));
            }
            beams.forEach(beam -> removeEntity(beam.display()));
            partsByPole.clear();
            beams.clear();
        }
    }

    /** A block part is a box in the frame of its section, a model part carries a whole model. */
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

    private record ScheduledStep(long tick, Runnable action) { }
}
