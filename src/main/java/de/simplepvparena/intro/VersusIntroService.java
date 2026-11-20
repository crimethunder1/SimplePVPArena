package de.simplepvparena.intro;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.match.Match;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;

@NullMarked
public final class VersusIntroService {

    public static final String ENTITY_MARKER_KEY_NAME = "versus_intro_entity";
    public static final String RETURN_KEY_NAME = "versus_intro_return";

    private static final byte MARKER_VALUE = 1;
    private static final int TARGET_DELAY_TICKS = 2;
    private static final int VS_POP_TICK = 55;
    private static final int VS_POP_DURATION_TICKS = 6;
    private static final int SWING_PERIOD_TICKS = 18;
    private static final double MINIMUM_HEIGHT_ABOVE_THE_TRIBUNE = 14.0D;
    private static final double CLEARANCE_ABOVE_THE_GROUND = 3.0D;
    private static final double BACKDROP_DEPTH = -2.6D;
    private static final double STAGE_DEPTH_FRONT = 1.6D;
    private static final double FLOOR_THICKNESS = 0.3D;
    private static final double BACKDROP_HEIGHT = 5.2D;
    private static final double NEON_THICKNESS = 0.12D;
    private static final int TERRAIN_SAMPLE_STEP = 2;
    private static final int TOP_MARGIN = 12;
    private static final Display.Brightness FULL_BRIGHTNESS = new Display.Brightness(15, 15);
    private static final TextColor LEFT_NEON_TEXT = NamedTextColor.AQUA;
    private static final TextColor RIGHT_NEON_TEXT = NamedTextColor.RED;

    private final Plugin plugin;
    private final ArenaRegistry arenaRegistry;
    private final Supplier<KitDefinition> kitSource;
    private final Function<Match, List<Player>> audienceSource;
    private final Predicate<UUID> activeFighter;
    private final NamespacedKey markerKey;
    private final NamespacedKey returnKey;
    private final Map<UUID, SavedState> viewers = new LinkedHashMap<>();
    private final List<Entity> stageEntities = new ArrayList<>();
    private final List<Mannequin> swingers = new ArrayList<>();

    private @Nullable ItemDisplay camera;
    private @Nullable TextDisplay versusText;
    private @Nullable BukkitTask task;
    private @Nullable Location stageCentre;
    private float stageYaw;
    private double stageHalfWidth;
    private int tick;

    public VersusIntroService(Plugin plugin, ArenaRegistry arenaRegistry, Supplier<KitDefinition> kitSource,
                              Function<Match, List<Player>> audienceSource, Predicate<UUID> activeFighter) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.kitSource = Objects.requireNonNull(kitSource, "kitSource");
        this.audienceSource = Objects.requireNonNull(audienceSource, "audienceSource");
        this.activeFighter = Objects.requireNonNull(activeFighter, "activeFighter");
        this.markerKey = new NamespacedKey(plugin, ENTITY_MARKER_KEY_NAME);
        this.returnKey = new NamespacedKey(plugin, RETURN_KEY_NAME);
    }

    public boolean isRunning() {
        return task != null;
    }

    public boolean isWatching(UUID playerUniqueId) {
        return viewers.containsKey(playerUniqueId);
    }

    public boolean isCamera(Entity entity) {
        return camera != null && camera.getUniqueId().equals(entity.getUniqueId());
    }

    public void play(Match match) {
        stop();
        try {
            List<Player> audience = audienceSource.apply(match).stream()
                    .filter(player -> player.isOnline() && !player.isDead())
                    .filter(player -> !activeFighter.test(player.getUniqueId()))
                    .toList();
            if (audience.isEmpty()) {
                return;
            }
            Optional<Location> centre = findStageCentre(match);
            if (centre.isEmpty()) {
                return;
            }
            buildStage(centre.get(), match);
            for (Player player : audience) {
                startWatching(player);
            }
            if (viewers.isEmpty()) {
                removeStage();
                return;
            }
            tick = 0;
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::runTick, 1L, 1L);
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING, "The versus intro of match " + match.getMatchId()
                    + " could not be shown, the match goes on without it", failure);
            stop();
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID viewer : List.copyOf(viewers.keySet())) {
            Player player = plugin.getServer().getPlayer(viewer);
            if (player != null) {
                stopWatching(player);
            }
        }
        viewers.clear();
        removeStage();
    }

    public void stopWatching(Player player) {
        SavedState state = viewers.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        restore(player, state);
    }

    public void restoreAfterAnInterruptedIntro(Player player) {
        if (viewers.containsKey(player.getUniqueId())) {
            return;
        }
        String stored = player.getPersistentDataContainer().get(returnKey, PersistentDataType.STRING);
        if (stored == null) {
            return;
        }
        SavedState.parse(stored).ifPresentOrElse(state -> restore(player, state),
                () -> player.getPersistentDataContainer().remove(returnKey));
    }

    private void runTick() {
        tick++;
        ItemDisplay currentCamera = camera;
        Location centre = stageCentre;
        if (currentCamera == null || centre == null || tick > VersusCameraPath.TOTAL_TICKS || viewers.isEmpty()) {
            stop();
            return;
        }
        VersusCameraPath.Point point = VersusCameraPath.at(tick, stageHalfWidth);
        Location cameraLocation = local(centre, point.right(), point.up(), point.forward());
        cameraLocation.setYaw(stageYaw + 180.0F - point.yawOffset());
        cameraLocation.setPitch(point.pitch());
        currentCamera.teleport(cameraLocation);
        for (UUID viewer : List.copyOf(viewers.keySet())) {
            Player player = plugin.getServer().getPlayer(viewer);
            if (player == null || !player.isOnline()) {
                viewers.remove(viewer);
                continue;
            }
            if (tick >= TARGET_DELAY_TICKS && !currentCamera.equals(player.getSpectatorTarget())) {
                player.setSpectatorTarget(currentCamera);
            }
        }
        if (tick % SWING_PERIOD_TICKS == 0) {
            swingers.forEach(Mannequin::swingMainHand);
        }
        if (tick == VS_POP_TICK) {
            popTheVersusText();
            playSound(Sound.ITEM_TRIDENT_THUNDER, 0.7F, 1.2F);
        }
    }

    private void startWatching(Player player) {
        SavedState state = new SavedState(player.getLocation().clone(), player.getGameMode(), player.getAllowFlight(),
                player.isFlying());
        viewers.put(player.getUniqueId(), state);
        player.getPersistentDataContainer().set(returnKey, PersistentDataType.STRING, state.serialise());
        for (Entity entity : stageEntities) {
            player.showEntity(plugin, entity);
        }
        player.setGameMode(GameMode.SPECTATOR);
        ItemDisplay currentCamera = camera;
        if (currentCamera != null) {
            player.teleport(currentCamera.getLocation());
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8F, 0.8F);
    }

    private void restore(Player player, SavedState state) {
        player.getPersistentDataContainer().remove(returnKey);
        try {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setSpectatorTarget(null);
            }
            player.teleport(state.location());
            player.setGameMode(state.gameMode());
            player.setAllowFlight(state.allowFlight());
            player.setFlying(state.allowFlight() && state.flying());
            player.setFallDistance(0.0F);
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING, "The player " + player.getName()
                    + " could not be brought back from the versus intro", failure);
        }
    }

    private Optional<Location> findStageCentre(Match match) {
        Optional<Location> tribune = arenaRegistry.getArena().getTribuneSpawn().flatMap(PositionSnapshot::toLocation);
        if (tribune.isEmpty()) {
            return Optional.empty();
        }
        Location base = tribune.get();
        World world = base.getWorld();
        double facingX = 0.0D;
        double facingZ = 1.0D;
        Optional<ArenaShape> shape = arenaRegistry.getArena().getShape();
        if (shape.isPresent()) {
            PositionSnapshot arenaCentre = shape.get().getCenterPosition();
            double deltaX = base.getX() - arenaCentre.x();
            double deltaZ = base.getZ() - arenaCentre.z();
            double length = Math.hypot(deltaX, deltaZ);
            if (length > 0.5D) {
                facingX = deltaX / length;
                facingZ = deltaZ / length;
            }
        }
        stageYaw = (float) Math.toDegrees(Math.atan2(-facingX, facingZ));
        int largestSide = Math.max(match.getFirstTeam().getMembers().size(),
                match.getSecondTeam().getMembers().size());
        stageHalfWidth = VersusStageLayout.halfWidthFor(largestSide);
        Location probe = new Location(world, base.getX(), base.getY(), base.getZ(), stageYaw, 0.0F);
        double farthest = VersusCameraPath.at(0, stageHalfWidth).forward() + 2.0D;
        int highest = (int) Math.floor(base.getY());
        for (double forward = BACKDROP_DEPTH - 1.0D; forward <= farthest; forward += TERRAIN_SAMPLE_STEP) {
            for (double right = -stageHalfWidth - 2.0D; right <= stageHalfWidth + 2.0D;
                 right += TERRAIN_SAMPLE_STEP) {
                Location sample = local(probe, right, 0.0D, forward);
                highest = Math.max(highest, world.getHighestBlockYAt(sample.getBlockX(), sample.getBlockZ()));
            }
        }
        double stageY = Math.max(base.getY() + MINIMUM_HEIGHT_ABOVE_THE_TRIBUNE, highest + CLEARANCE_ABOVE_THE_GROUND);
        stageY = Math.min(stageY, world.getMaxHeight() - TOP_MARGIN);
        return Optional.of(new Location(world, base.getX(), stageY, base.getZ(), stageYaw, 0.0F));
    }

    private void buildStage(Location centre, Match match) {
        stageCentre = centre;
        World world = centre.getWorld();
        double width = stageHalfWidth * 2.0D;
        double depth = STAGE_DEPTH_FRONT - BACKDROP_DEPTH;
        block(centre, Material.POLISHED_DEEPSLATE, -stageHalfWidth, -FLOOR_THICKNESS, BACKDROP_DEPTH,
                width, FLOOR_THICKNESS, depth, false);
        block(centre, Material.LIGHT_BLUE_CONCRETE, -stageHalfWidth, -NEON_THICKNESS, STAGE_DEPTH_FRONT,
                stageHalfWidth, NEON_THICKNESS, NEON_THICKNESS, true);
        block(centre, Material.RED_CONCRETE, 0.0D, -NEON_THICKNESS, STAGE_DEPTH_FRONT,
                stageHalfWidth, NEON_THICKNESS, NEON_THICKNESS, true);
        block(centre, Material.BLACK_CONCRETE, -stageHalfWidth, 0.0D, BACKDROP_DEPTH,
                width, BACKDROP_HEIGHT, 0.2D, false);
        block(centre, Material.IRON_BLOCK, -stageHalfWidth - 0.15D, -FLOOR_THICKNESS, BACKDROP_DEPTH - 0.05D,
                0.3D, BACKDROP_HEIGHT + FLOOR_THICKNESS + 0.15D, 0.35D, false);
        block(centre, Material.IRON_BLOCK, stageHalfWidth - 0.15D, -FLOOR_THICKNESS, BACKDROP_DEPTH - 0.05D,
                0.3D, BACKDROP_HEIGHT + FLOOR_THICKNESS + 0.15D, 0.35D, false);
        block(centre, Material.IRON_BLOCK, -stageHalfWidth - 0.15D, BACKDROP_HEIGHT, BACKDROP_DEPTH - 0.05D,
                width + 0.3D, 0.3D, 0.35D, false);
        for (double height : new double[] {1.0D, 4.4D}) {
            block(centre, Material.LIGHT_BLUE_CONCRETE, -stageHalfWidth + 0.4D, height, BACKDROP_DEPTH + 0.21D,
                    stageHalfWidth - 0.9D, NEON_THICKNESS, 0.05D, true);
            block(centre, Material.RED_CONCRETE, 0.5D, height, BACKDROP_DEPTH + 0.21D,
                    stageHalfWidth - 0.9D, NEON_THICKNESS, 0.05D, true);
        }
        block(centre, Material.GOLD_BLOCK, -0.08D, 0.0D, BACKDROP_DEPTH + 0.21D, 0.16D, BACKDROP_HEIGHT, 0.05D, true);

        Team left = match.getFirstTeam();
        Team right = match.getSecondTeam();
        text(centre, -stageHalfWidth / 2.0D, 3.6D, BACKDROP_DEPTH + 0.6D,
                Component.text(left.getTeamName(), colourOf(left, LEFT_NEON_TEXT), TextDecoration.BOLD), 1.6F);
        text(centre, stageHalfWidth / 2.0D, 3.6D, BACKDROP_DEPTH + 0.6D,
                Component.text(right.getTeamName(), colourOf(right, RIGHT_NEON_TEXT), TextDecoration.BOLD), 1.6F);
        versusText = text(centre, 0.0D, 2.3D, BACKDROP_DEPTH + 0.8D,
                Component.text("VS", NamedTextColor.GOLD, TextDecoration.BOLD), 0.0F);

        VersusLoadout loadout = VersusLoadout.of(kitSource.get());
        placeFighters(centre, match, left, VersusStageLayout.Side.LEFT, loadout);
        placeFighters(centre, match, right, VersusStageLayout.Side.RIGHT, loadout);

        VersusCameraPath.Point start = VersusCameraPath.at(0, stageHalfWidth);
        Location cameraLocation = local(centre, start.right(), start.up(), start.forward());
        cameraLocation.setYaw(stageYaw + 180.0F - start.yawOffset());
        cameraLocation.setPitch(start.pitch());
        camera = world.spawn(cameraLocation, ItemDisplay.class, display -> {
            prepare(display);
            display.setTeleportDuration(1);
        });
        stageEntities.add(camera);
    }

    private void placeFighters(Location centre, Match match, Team team, VersusStageLayout.Side side,
                               VersusLoadout loadout) {
        List<Player> fighters = new ArrayList<>();
        List<UUID> chosen = match.findChosenFighters(team).orElseGet(() -> List.copyOf(team.getMemberUniqueIds()));
        for (UUID fighterUniqueId : chosen) {
            Player fighter = plugin.getServer().getPlayer(fighterUniqueId);
            if (fighter != null && fighter.isOnline()) {
                fighters.add(fighter);
            }
        }
        List<VersusStageLayout.Spot> spots = VersusStageLayout.spotsOf(side, fighters.size());
        for (int index = 0; index < spots.size(); index++) {
            placeFighter(centre, fighters.get(index), spots.get(index), side, loadout);
        }
    }

    private void placeFighter(Location centre, Player fighter, VersusStageLayout.Spot spot,
                              VersusStageLayout.Side side, VersusLoadout loadout) {
        Location location = local(centre, spot.right(), spot.up(), spot.forward());
        location.setYaw(stageYaw + spot.yawOffset());
        location.setPitch(0.0F);
        Mannequin mannequin = centre.getWorld().spawn(location, Mannequin.class, entity -> {
            prepare(entity);
            entity.setGravity(false);
            entity.setAI(false);
            entity.setImmovable(true);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setCollidable(false);
            entity.setProfile(ResolvableProfile.resolvableProfile(fighter.getPlayerProfile()));
            entity.customName(Component.text(fighter.getName()));
            entity.setCustomNameVisible(true);
            entity.setDescription(Component.empty());
            EntityEquipment equipment = entity.getEquipment();
            loadout.armour().forEach((slot, item) -> equipment.setItem(slot, item.clone()));
        });
        EntityEquipment equipment = mannequin.getEquipment();
        switch (spot.pose()) {
            case SWORD -> {
                loadout.weapon().ifPresent(item -> equipment.setItemInMainHand(item.clone()));
                swingers.add(mannequin);
            }
            case BOW_JUMP -> {
                Optional<ItemStack> ranged = loadout.rangedWeapon();
                if (ranged.isPresent()) {
                    equipment.setItemInMainHand(ranged.get().clone());
                    mannequin.startUsingItem(EquipmentSlot.HAND);
                    frozenArrow(location, side);
                } else {
                    loadout.weapon().ifPresent(item -> equipment.setItemInMainHand(item.clone()));
                    swingers.add(mannequin);
                }
            }
            case SHIELD -> {
                loadout.weapon().ifPresent(item -> equipment.setItemInMainHand(item.clone()));
                if (loadout.shield().isPresent()) {
                    equipment.setItemInOffHand(loadout.shield().get().clone());
                    mannequin.startUsingItem(EquipmentSlot.OFF_HAND);
                } else if (loadout.crossbow().isPresent()) {
                    equipment.setItemInMainHand(loadout.crossbow().get().clone());
                    mannequin.startUsingItem(EquipmentSlot.HAND);
                } else {
                    swingers.add(mannequin);
                }
            }
        }
        stageEntities.add(mannequin);
    }

    private void frozenArrow(Location fighterLocation, VersusStageLayout.Side side) {
        Location arrowLocation = fighterLocation.clone().add(0.0D, 1.45D, 0.0D);
        ItemDisplay arrow = arrowLocation.getWorld().spawn(arrowLocation, ItemDisplay.class, display -> {
            prepare(display);
            display.setItemStack(new ItemStack(Material.ARROW));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            double tipAngle = side == VersusStageLayout.Side.LEFT ? -45.0D : 135.0D;
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.toRadians(tipAngle));
            display.setTransformation(new Transformation(new Vector3f(-side.sign() * 0.9F, 0.0F, 0.3F), rotation,
                    new Vector3f(0.8F), new Quaternionf()));
            display.setBrightness(FULL_BRIGHTNESS);
        });
        stageEntities.add(arrow);
    }

    private void block(Location centre, Material material, double right, double up, double forward,
                       double width, double height, double depth, boolean bright) {
        BlockDisplay display = centre.getWorld().spawn(centre, BlockDisplay.class, entity -> {
            prepare(entity);
            entity.setBlock(material.createBlockData());
            entity.setTransformation(new Transformation(
                    new Vector3f((float) right, (float) up, (float) forward),
                    new AxisAngle4f(), new Vector3f((float) width, (float) height, (float) depth),
                    new AxisAngle4f()));
            if (bright) {
                entity.setBrightness(FULL_BRIGHTNESS);
            }
        });
        stageEntities.add(display);
    }

    private TextDisplay text(Location centre, double right, double up, double forward, Component content,
                             float scale) {
        Location location = local(centre, right, up, forward);
        TextDisplay display = centre.getWorld().spawn(location, TextDisplay.class, entity -> {
            prepare(entity);
            entity.text(content);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            entity.setShadowed(true);
            entity.setBrightness(FULL_BRIGHTNESS);
            entity.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale),
                    new AxisAngle4f()));
        });
        stageEntities.add(display);
        return display;
    }

    private void popTheVersusText() {
        TextDisplay display = versusText;
        if (display == null) {
            return;
        }
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(VS_POP_DURATION_TICKS);
        display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(3.2F),
                new AxisAngle4f()));
    }

    private void playSound(Sound sound, float volume, float pitch) {
        for (UUID viewer : viewers.keySet()) {
            Player player = plugin.getServer().getPlayer(viewer);
            if (player != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
    }

    private void prepare(Entity entity) {
        entity.setPersistent(false);
        entity.setVisibleByDefault(false);
        entity.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, MARKER_VALUE);
    }

    private void removeStage() {
        for (Entity entity : stageEntities) {
            if (entity.isValid()) {
                entity.remove();
            }
        }
        stageEntities.clear();
        swingers.clear();
        camera = null;
        versusText = null;
        stageCentre = null;
    }

    private Location local(Location centre, double right, double up, double forward) {
        double yaw = Math.toRadians(stageYaw);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double x = centre.getX() + right * cos - forward * sin;
        double z = centre.getZ() + right * sin + forward * cos;
        return new Location(centre.getWorld(), x, centre.getY() + up, z);
    }

    private static TextColor colourOf(Team team, TextColor fallback) {
        return team.getColour().<TextColor>map(TeamColour::getTextColour).orElse(fallback);
    }

    record SavedState(Location location, GameMode gameMode, boolean allowFlight, boolean flying) {

        private static final String SEPARATOR = ";";
        private static final int FIELD_COUNT = 9;

        String serialise() {
            return String.join(SEPARATOR, location.getWorld().getName(), Double.toString(location.getX()),
                    Double.toString(location.getY()), Double.toString(location.getZ()),
                    Float.toString(location.getYaw()), Float.toString(location.getPitch()), gameMode.name(),
                    Boolean.toString(allowFlight), Boolean.toString(flying));
        }

        static Optional<SavedState> parse(String stored) {
            String[] fields = stored.split(SEPARATOR);
            if (fields.length != FIELD_COUNT) {
                return Optional.empty();
            }
            World world = org.bukkit.Bukkit.getWorld(fields[0]);
            if (world == null) {
                return Optional.empty();
            }
            try {
                Location location = new Location(world, Double.parseDouble(fields[1]), Double.parseDouble(fields[2]),
                        Double.parseDouble(fields[3]), Float.parseFloat(fields[4]), Float.parseFloat(fields[5]));
                return Optional.of(new SavedState(location, GameMode.valueOf(fields[6]),
                        Boolean.parseBoolean(fields[7]), Boolean.parseBoolean(fields[8])));
            } catch (IllegalArgumentException malformed) {
                return Optional.empty();
            }
        }
    }
}
