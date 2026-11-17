package com.example.darkspirit.entity;

import net.minecraft.world.entity.monster.Ghast;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

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
    // Координаты места, где дух появился.
    private double flightCenterX;
    private double flightCenterY;
    private double flightCenterZ;

    // Проверяем, запомнили ли мы начальное положение.
    private boolean flightCenterSet = false;

    /**
     * Регистрирует цели поведения Духа тьмы.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Летаем вокруг места появления и не удаляемся бесконечно.
        this.goalSelector.addGoal(
                5,
                new BoundedRandomFlightGoal(this));

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

    @Override
    public void tick() {

        // При первом серверном обновлении запоминаем место появления.
        if (!this.level().isClientSide() && !this.flightCenterSet) {
            this.flightCenterX = this.getX();
            this.flightCenterY = this.getY();
            this.flightCenterZ = this.getZ();

            this.flightCenterSet = true;
        }

        super.tick();
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
     * Управляет спокойным полётом вокруг места появления.
     */
    private static class BoundedRandomFlightGoal extends Goal {

        private final DarkSpiritEntity spirit;

        public BoundedRandomFlightGoal(DarkSpiritEntity spirit) {
            this.spirit = spirit;

            // Эта цель отвечает за перемещение.
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        /**
         * Проверяем, можно ли начать новый перелёт.
         */
        @Override
        public boolean canUse() {

            // Дух должен знать своё начальное положение.
            // Также он не должен продолжать предыдущий перелёт.
            // Случайная проверка создаёт паузы между перелётами.
            return this.spirit.flightCenterSet
                    && !this.spirit.getMoveControl().hasWanted()
                    && this.spirit.getRandom().nextInt(20) == 0;
        }

        /**
         * Продолжаем цель, пока дух летит к выбранной точке.
         */
        @Override
        public boolean canContinueToUse() {
            return this.spirit.getMoveControl().hasWanted();
        }

        /**
         * Выбираем новую точку в ограниченной области.
         */
        @Override
        public void start() {

            // Случайная координата X: не дальше 8 блоков от центра.
            double x = this.spirit.flightCenterX
                    + this.spirit.getRandom().nextInt(17) - 8;

            // Случайная координата Y: не дальше 4 блоков от центра.
            double y = this.spirit.flightCenterY
                    + this.spirit.getRandom().nextInt(9) - 4;

            // Случайная координата Z: не дальше 8 блоков от центра.
            double z = this.spirit.flightCenterZ
                    + this.spirit.getRandom().nextInt(17) - 8;

            // Направляем духа к выбранной точке.
            this.spirit.getMoveControl().setWantedPosition(
                    x,
                    y,
                    z,
                    0.7);
        }
    }

    /**
     * Возвращает хранилище анимаций GeckoLib.
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}