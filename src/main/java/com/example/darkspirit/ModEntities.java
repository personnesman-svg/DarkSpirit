package com.example.darkspirit;

import com.example.darkspirit.entity.DarkSpiritEntity;

import java.util.function.Supplier;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Реестр сущностей и их базовых характеристик.
 */
public final class ModEntities {

        // Реестр типов сущностей нашего мода.
        public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(DarkSpirit.MODID);

        // Регистрация Духа тьмы.
        public static final Supplier<EntityType<DarkSpiritEntity>> DARK_SPIRIT = ENTITY_TYPES.registerEntityType(
                        "dark_spirit",
                        DarkSpiritEntity::new,
                        MobCategory.MONSTER);

        /**
         * Регистрирует базовые характеристики Духа тьмы.
         */
        public static void registerAttributes(EntityAttributeCreationEvent event) {

                event.put(
                                DARK_SPIRIT.get(),

                                Mob.createMobAttributes()

                                                // 25 сердец здоровья.
                                                .add(Attributes.MAX_HEALTH, 50.0)

                                                // 2 единицы брони.
                                                .add(Attributes.ARMOR, 2.0)

                                                // Базовая скорость перемещения.
                                                .add(Attributes.MOVEMENT_SPEED, 0.2)

                                                // Базовая скорость полёта.
                                                .add(Attributes.FLYING_SPEED, 0.2)
                                                // Расстояние обнаружения игрока — 24 блока.
                                                .add(Attributes.FOLLOW_RANGE, 24.0)

                                                .build());
        }

        // Запрещаем создавать экземпляры этого класса.
        private ModEntities() {
        }
}