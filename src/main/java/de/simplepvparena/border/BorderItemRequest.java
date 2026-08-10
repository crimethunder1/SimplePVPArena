package de.simplepvparena.border;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

public record BorderItemRequest(Material material, int amount) {

    public static final int MINIMUM_AMOUNT = 1;
    public static final int MAXIMUM_AMOUNT = 64;

    public BorderItemRequest {
        Objects.requireNonNull(material, "material");
    }

    public static BorderItemRequestResult parse(String materialArgument, String amountArgument,
                                                Predicate<Material> placeableBlockCheck) {
        Material material = Material.getMaterial(normalizeMaterialArgument(materialArgument));
        if (material == null) {
            return BorderItemRequestResult.rejected("Unknown material " + materialArgument);
        }
        if (!placeableBlockCheck.test(material)) {
            return BorderItemRequestResult.rejected(material.name() + " cannot be placed as a block");
        }
        int amount;
        try {
            amount = Integer.parseInt(amountArgument.trim());
        } catch (NumberFormatException notANumber) {
            return BorderItemRequestResult.rejected("The amount " + amountArgument + " is not a number");
        }
        if (amount < MINIMUM_AMOUNT || amount > MAXIMUM_AMOUNT) {
            return BorderItemRequestResult.rejected("The amount has to be between " + MINIMUM_AMOUNT
                    + " and " + MAXIMUM_AMOUNT);
        }
        return BorderItemRequestResult.accepted(new BorderItemRequest(material, amount));
    }

    private static String normalizeMaterialArgument(String materialArgument) {
        String normalizedArgument = materialArgument.trim().toUpperCase(Locale.ROOT);
        int namespaceSeparatorIndex = normalizedArgument.indexOf(':');
        return namespaceSeparatorIndex < 0
                ? normalizedArgument
                : normalizedArgument.substring(namespaceSeparatorIndex + 1);
    }
}
