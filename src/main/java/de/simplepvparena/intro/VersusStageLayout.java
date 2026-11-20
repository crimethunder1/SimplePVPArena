package de.simplepvparena.intro;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class VersusStageLayout {

    public static final int MAXIMUM_FIGHTERS_PER_SIDE = 4;
    public static final double FIRST_FIGHTER_SIDE_OFFSET = 2.2D;
    public static final double FIGHTER_SPACING = 1.7D;
    public static final double BACK_ROW_DEPTH = -0.9D;
    public static final double JUMP_HEIGHT = 1.3D;
    public static final float TURN_TOWARDS_THE_CENTRE_DEGREES = 25.0F;
    public static final double STAGE_MARGIN = 1.6D;
    public static final double MINIMUM_STAGE_HALF_WIDTH = 5.0D;

    private VersusStageLayout() {
    }

    public static List<Spot> spotsOf(Side side, int fighterCount) {
        int shown = Math.max(0, Math.min(fighterCount, MAXIMUM_FIGHTERS_PER_SIDE));
        List<Spot> spots = new ArrayList<>(shown);
        for (int index = 0; index < shown; index++) {
            Pose pose = Pose.ofIndex(index);
            double sideOffset = FIRST_FIGHTER_SIDE_OFFSET + index * FIGHTER_SPACING;
            double depth = index % 2 == 1 ? BACK_ROW_DEPTH : 0.0D;
            double height = pose == Pose.BOW_JUMP ? JUMP_HEIGHT : 0.0D;
            spots.add(new Spot(side.sign() * sideOffset, height, depth,
                    side.sign() * TURN_TOWARDS_THE_CENTRE_DEGREES, pose));
        }
        return spots;
    }

    public static double halfWidthFor(int largestSideCount) {
        int shown = Math.max(1, Math.min(largestSideCount, MAXIMUM_FIGHTERS_PER_SIDE));
        double outermost = FIRST_FIGHTER_SIDE_OFFSET + (shown - 1) * FIGHTER_SPACING;
        return Math.max(MINIMUM_STAGE_HALF_WIDTH, outermost + STAGE_MARGIN);
    }

    public enum Side {
        LEFT(-1),
        RIGHT(1);

        private final int sign;

        Side(int sign) {
            this.sign = sign;
        }

        public int sign() {
            return sign;
        }
    }

    public enum Pose {
        SWORD,
        BOW_JUMP,
        SHIELD;

        static Pose ofIndex(int index) {
            return values()[index % values().length];
        }
    }

    public record Spot(double right, double up, double forward, float yawOffset, Pose pose) {
    }
}
