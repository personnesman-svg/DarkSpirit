
package com.example.darkspirit.entity;

// Импорты GeckoLib 5
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

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
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    // Постоянная анимация покоя без копья.
    private static final RawAnimation IDLE_WITHOUT_SPEAR = RawAnimation.begin().thenLoop("idle2");

    /**
     * Создаёт новый экземпляр Духа тьмы.
     *
     * @param entityType зарегистрированный тип существа
     * @param level      мир, в котором находится существо
     */
    public DarkSpiritEntity(
            EntityType<? extends Mob> entityType,
            Level level) {
        super(entityType, level);
    }

    /**
     * Здесь будут регистрироваться контроллеры анимаций:
     * idle, idle2, fly, attack и teleport.
     */
    /**
     * Регистрирует контроллеры анимаций Духа тьмы.
     */
    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {

        // Основной контроллер постоянно воспроизводит анимацию idle2.
        controllers.add(
                new AnimationController<>(
                        "base",
                        state -> state.setAndContinue(IDLE_WITHOUT_SPEAR)));
    }

    /**
     * Возвращает хранилище состояния анимаций GeckoLib.
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}