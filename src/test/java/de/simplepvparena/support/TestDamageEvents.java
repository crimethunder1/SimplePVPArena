package de.simplepvparena.support;

import com.google.common.base.Functions;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.EnumMap;
import java.util.Map;

public final class TestDamageEvents {

    private TestDamageEvents() {
    }

    public static EntityDamageEvent damage(Entity entity, DamageCause cause, DamageSource damageSource,
                                           double damage) {
        return new EntityDamageEvent(entity, cause, damageSource,
                new EnumMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, damage)),
                new EnumMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(-0.0D))));
    }
}
