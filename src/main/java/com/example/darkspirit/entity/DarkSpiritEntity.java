package com.example.darkspirit.entity;

import net.minecraft.world.entity.monster.Ghast;

// GeckoLib 5: анимации сущности.
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

// Minecraft: сущность и полёт.
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Основной класс существа «Дух тьмы».
 *
 * PathfinderMob — моб с поддержкой поиска пути.
 * FlyingMoveControl — управление перемещением в воздухе.
 * FlyingPathNavigation — прокладывание маршрутов для летающего моба.
 * GeoEntity — поддержка анимаций GeckoLib.
 * Enemy — обозначает враждебное существо.
 */
public class DarkSpiritEntity extends PathfinderMob implements GeoEntity, Enemy {

    // Хранилище анимаций GeckoLib.
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Постоянная анимация покоя без копья.
    private static final RawAnimation IDLE_WITHOUT_SPEAR = RawAnimation.begin().thenLoop("idle2");
    // Анимация движения в воздухе.
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");

    /**
     * Регистрирует цели поведения Духа тьмы.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Выбираем случайные точки для полёта.
        this.goalSelector.addGoal(
                5,
                new Ghast.RandomFloatAroundGoal(this));

        // Поворачиваем тело в направлении движения.
        this.goalSelector.addGoal(
                7,
                new Ghast.GhastLookGoal(this));
    }

    /**
     * Создаёт новый экземпляр Духа тьмы.
     */
    public DarkSpiritEntity(
            EntityType<? extends PathfinderMob> entityType,
            Level level) {
        super(entityType, level);

        // Используем систему плавного перемещения, как у гаста.
        this.moveControl = new Ghast.GhastMoveControl(
                this,
                false,
                () -> false);

        // Отключаем гравитацию.
        this.setNoGravity(true);
    }

    /**
     * Создаёт навигацию, предназначенную для полёта.
     */
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    /**
     * Использует физику полёта Minecraft.
     */
    @Override
    public void travel(Vec3 input) {
        // Обычное перемещение по земле не используется.
        this.travelFlying(input, 0.02F);
    }

    /**
     * Регистрирует контроллеры анимаций.
     */
    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(
                        "base",
                        5,
                        state -> {

                            // Если дух движется, включаем анимацию полёта.
                            if (state.isMoving()) {
                                return state.setAndContinue(FLY);
                            }

                            // Если дух остановился, включаем анимацию покоя.
                            return state.setAndContinue(IDLE_WITHOUT_SPEAR);
                        }));
    }

    /**
     * Возвращает хранилище анимаций GeckoLib.
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}