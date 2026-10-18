package de.simplepvparena.input;

import de.simplepvparena.message.InputMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import org.bukkit.Material;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public record BlockItemRequest(Material material, int amount) {

    public static final int MINIMUM_AMOUNT = 1;
    public static final int MAXIMUM_AMOUNT = 64;
    public static final int REQUIRED_VALUE_COUNT = 2;
    public static final String USAGE = "<material> <amount>";

    public BlockItemRequest {
        Objects.requireNonNull(material, "material");
    }

    public static BlockItemRequestResult parseLine(String line, Predicate<Material> placeableBlockCheck) {
        ChatInputValues values = ChatInputValues.of(line);
        Optional<String> countRejection = values.findCountRejection(REQUIRED_VALUE_COUNT, USAGE);
        if (countRejection.isPresent()) {
            return BlockItemRequestResult.rejected(countRejection.get());
        }
        return parse(values.get(0), values.get(1), placeableBlockCheck);
    }

    public static BlockItemRequestResult parse(String materialArgument, String amountArgument,
                                               Predicate<Material> placeableBlockCheck) {
        Material material = Material.getMaterial(normalizeMaterialArgument(materialArgument));
        if (material == null) {
            return BlockItemRequestResult.rejected(Messages.text(InputMessage.UNKNOWN_MATERIAL,
                    Placeholder.of("material", materialArgument)));
        }
        if (!placeableBlockCheck.test(material)) {
            return BlockItemRequestResult.rejected(Messages.text(InputMessage.NOT_A_BLOCK,
                    Placeholder.of("material", material.name())));
        }
        int amount;
        try {
            amount = Integer.parseInt(amountArgument.trim());
        } catch (NumberFormatException notANumber) {
            return BlockItemRequestResult.rejected(Messages.text(InputMessage.AMOUNT_NOT_A_NUMBER,
                    Placeholder.of("amount", amountArgument)));
        }
        if (amount < MINIMUM_AMOUNT || amount > MAXIMUM_AMOUNT) {
            return BlockItemRequestResult.rejected(Messages.text(InputMessage.AMOUNT_OUT_OF_RANGE,
                    Placeholder.of("minimum", MINIMUM_AMOUNT), Placeholder.of("maximum", MAXIMUM_AMOUNT)));
        }
        return BlockItemRequestResult.accepted(new BlockItemRequest(material, amount));
    }

    private static String normalizeMaterialArgument(String materialArgument) {
        String normalizedArgument = materialArgument.trim().toUpperCase(Locale.ROOT);
        int namespaceSeparatorIndex = normalizedArgument.indexOf(':');
        return namespaceSeparatorIndex < 0
                ? normalizedArgument
                : normalizedArgument.substring(namespaceSeparatorIndex + 1);
    }
}
