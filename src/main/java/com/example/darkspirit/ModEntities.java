package com.example.darkspirit;

import com.example.darkspirit.entity.DarkSpiritEntity;

import java.util.function.Supplier;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Реестр всех сущностей нашего мода.
 */
public final class ModEntities {

    // Создаём реестр типов сущностей для нашего мода.
    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(DarkSpirit.MODID);

    // Регистрируем существо «Дух тьмы».
    public static final Supplier<EntityType<DarkSpiritEntity>> DARK_SPIRIT =
            ENTITY_TYPES.registerEntityType(
                    "dark_spirit",
                    DarkSpiritEntity::new,
                    MobCategory.MONSTER
            );

    // Запрещаем создавать экземпляры этого класса.
    private ModEntities() {
    }
}