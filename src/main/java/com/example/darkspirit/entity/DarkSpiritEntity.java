
package com.example.darkspirit.entity;

// Импорты GeckoLib 5
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

// Импорты Minecraft
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Основной класс существа «Дух тьмы».
 *
 * Mob — базовый класс мобов Minecraft.
 * GeoEntity — подключает поддержку анимаций GeckoLib.
 * Enemy — обозначает враждебное существо.
 */
public class DarkSpiritEntity extends Mob implements GeoEntity, Enemy {

    // Отдельное хранилище анимаций для каждого экземпляра духа.
    private final AnimatableInstanceCache geoCache =
            GeckoLibUtil.createInstanceCache(this);

    /**
     * Создаёт новый экземпляр Духа тьмы.
     *
     * @param entityType зарегистрированный тип существа
     * @param level мир, в котором находится существо
     */
    public DarkSpiritEntity(
            EntityType<? extends Mob> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    /**
     * Здесь будут регистрироваться контроллеры анимаций:
     * idle, idle2, fly, attack и teleport.
     */
    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        // Контроллеры добавим на следующем этапе.
    }

    /**
     * Возвращает хранилище состояния анимаций GeckoLib.
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}