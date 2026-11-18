package com.example.darkspirit.entity;

import net.minecraft.world.entity.monster.Ghast;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import com.example.darkspirit.DarkSpirit;

// GeckoLib 5: анимации сущности.
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

// Minecraft: сущность и полёт.
import net.minecraft.world.entity.EntityType;
import com.example.darkspirit.ModEntities;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

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

    // Анимация броска: проигрывается один раз, без зацикливания.
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    // Перезарядка между бросками.
    private int spearCooldownTicks = 0;

    // Координаты места, где дух появился.
    private double flightCenterX;
    private double flightCenterY;
    private double flightCenterZ;

    // Проверяем, запомнили ли мы начальное положение.
    private boolean flightCenterSet = false;
    // Сколько тиков дух должен отдыхать между перелётами.
    private int flightPauseTicks = 0;

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Приоритет 1: ищем ближайшего игрока.
        this.targetSelector.addGoal(
                1,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        false));

        // Приоритет 2: преследуем найденного игрока.
        this.goalSelector.addGoal(
                2,
                new ChasePlayerGoal(this));

        // Приоритет 5: спокойно летаем, когда игрока рядом нет.
        this.goalSelector.addGoal(
                5,
                new BoundedRandomFlightGoal(this));

        // Приоритет 7: управление поворотом тела.
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

        // При первом серверном обновлении запоминаем место появления Духа.
        // Эти координаты нужны для ограниченного патрулирования вокруг точки спавна.
        if (!this.level().isClientSide() && !this.flightCenterSet) {
            this.flightCenterX = this.getX();
            this.flightCenterY = this.getY();
            this.flightCenterZ = this.getZ();

            this.flightCenterSet = true;
        }

        // КРИТИЧЕСКИ ВАЖНО:
        // Выполняем штатное обновление Minecraft.
        // Здесь обновляются ИИ, цели, контроллер движения, навигация и физика моба.
        super.tick();

        // Таймеры нашего мода изменяем только на сервере.
        if (!this.level().isClientSide()) {

            // Уменьшаем перезарядку между бросками копья.
            if (this.spearCooldownTicks > 0) {
                this.spearCooldownTicks--;
            }

            // После завершения патрульного перелёта постепенно гасим скорость.
            if (this.flightPauseTicks > 0) {
                this.flightPauseTicks--;

                // Плавно уменьшаем скорость, чтобы Дух не останавливался резко.
                this.setDeltaMovement(
                        this.getDeltaMovement().scale(0.80D));
            }
        }
    }

    /**
     * Начинает атаку и сразу запускает копьё.
     */
    private void startSpearAttack(LivingEntity target) {

        // Атаку выполняет только сервер.
        if (this.level().isClientSide()) {
            return;
        }

        // Проверяем цель и перезарядку.
        if (target == null || !target.isAlive() || this.spearCooldownTicks > 0) {
            return;
        }

        // Устанавливаем перезарядку на 4 секунды.
        this.spearCooldownTicks = 80;

        // Запускаем анимацию атаки.
        this.triggerAnim("base", "attack");

        // Создаём и запускаем копьё.
        this.throwSpearAt(target);
    }

    /**
     * Создаёт и запускает копьё в сторону цели.
     */
    private void throwSpearAt(LivingEntity target) {

        if (this.level().isClientSide() || !target.isAlive()) {
            return;
        }

        // Примерное положение руки Духа.
        Vec3 origin = this.position().add(
                0.0D,
                this.getBbHeight() * 0.65D,
                0.0D);

        // Целимся в середину тела игрока.
        Vec3 targetPosition = target.position().add(
                0.0D,
                target.getBbHeight() * 0.5D,
                0.0D);

        // Вычисляем направление полёта.
        Vec3 direction = targetPosition.subtract(origin);

        if (direction.lengthSqr() < 1.0E-7D) {
            return;
        }

        // Получаем нормализованное направление.
        Vec3 normalizedDirection = direction.normalize();

        // Создаём новую сущность копья.
        DarkSpiritSpearEntity spear = new DarkSpiritSpearEntity(
                ModEntities.DARK_SPIRIT_SPEAR.get(),
                this.level());

        // Выносим копьё немного вперёд от Духа.
        Vec3 spawnPosition = origin.add(
                normalizedDirection.scale(0.8D));

        spear.setPos(
                spawnPosition.x,
                spawnPosition.y,
                spawnPosition.z);

        // Запускаем копьё со скоростью 1,1 блока за тик.
        spear.launch(this, direction, 1.1F);

        // Добавляем копьё в игровой мир.
        boolean added = this.level().addFreshEntity(spear);

        // Временно проверяем результат добавления.
        DarkSpirit.LOGGER.info(
                "[DarkSpirit] Копьё добавлено в мир: {}",
                added);
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

        // Основной контроллер отвечает за полёт и покой.
        controllers.add(
                new AnimationController<>(
                        "base",
                        5,
                        state -> {

                            if (state.isMoving()) {
                                return state.setAndContinue(FLY);
                            }

                            return state.setAndContinue(IDLE_WITHOUT_SPEAR);
                        })

                        // Добавляем одноразовую анимацию броска.
                        .triggerableAnim("attack", ATTACK));
    }

    /**
     * Преследует ближайшего обнаруженного игрока.
     */
    private static class ChasePlayerGoal extends Goal {

        private final DarkSpiritEntity spirit;

        public ChasePlayerGoal(DarkSpiritEntity spirit) {
            this.spirit = spirit;

            // Во время преследования управляем движением и взглядом.
            this.setFlags(EnumSet.of(
                    Goal.Flag.MOVE,
                    Goal.Flag.LOOK));
        }

        /**
         * Проверяем, есть ли живая цель для преследования.
         */
        @Override
        public boolean canUse() {
            LivingEntity target = this.spirit.getTarget();

            return target != null && target.isAlive();
        }

        /**
         * Продолжаем преследование, пока цель жива.
         */
        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.spirit.getTarget();

            return target != null && target.isAlive();
        }

        /**
         * Выполняем преследование каждый игровой тик.
         */
        @Override
        public void tick() {

            LivingEntity target = this.spirit.getTarget();

            if (target == null) {
                return;
            }

            // Поворачиваем взгляд в сторону игрока.
            this.spirit.getLookControl().setLookAt(
                    target,
                    30.0F,
                    30.0F);

            // Вычисляем горизонтальное расстояние до игрока.
            double dx = target.getX() - this.spirit.getX();
            double dz = target.getZ() - this.spirit.getZ();

            double horizontalDistanceSquared = dx * dx + dz * dz;

            // Если игрок дальше 6 блоков по горизонтали — приближаемся.
            if (horizontalDistanceSquared > 36.0D) {

                this.spirit.getMoveControl().setWantedPosition(
                        target.getX(),
                        target.getY() + 1.0D,
                        target.getZ(),
                        0.55D);

            } else {

                // Если приблизились — прекращаем движение.
                this.spirit.getMoveControl().setWait();

                // Плавно гасим оставшуюся скорость.
                this.spirit.setDeltaMovement(
                        this.spirit.getDeltaMovement().scale(0.65D));
            }

            // Начинаем подготовку атаки в пределах 7 блоков.
            // Проверка не зависит от высоты полёта Духа.
            if (horizontalDistanceSquared <= 49.0D) {
                this.spirit.startSpearAttack(target);
            }
        }

        /**
         * Завершаем движение, если цель потеряна.
         */
        @Override
        public void stop() {
            this.spirit.getMoveControl().setWait();
        }
    }

    /**
     * Управляет спокойным полётом вокруг места появления.
     */
    private static class BoundedRandomFlightGoal extends Goal {

        private final DarkSpiritEntity spirit;

        // Координаты выбранной точки полёта.
        private double targetX;
        private double targetY;
        private double targetZ;

        // Сколько тиков длится текущий перелёт.
        private int flightTicks;

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

            // Если игрок обнаружен, патрулирование запрещено.
            return this.spirit.getTarget() == null
                    && this.spirit.flightCenterSet
                    && this.spirit.flightPauseTicks <= 0
                    && !this.spirit.getMoveControl().hasWanted();
        }

        /**
         * Проверяем, нужно ли продолжать перелёт.
         */
        @Override
        public boolean canContinueToUse() {

            double dx = this.targetX - this.spirit.getX();
            double dy = this.targetY - this.spirit.getY();
            double dz = this.targetZ - this.spirit.getZ();

            // Считаем расстояние до точки назначения.
            double distanceSquared = dx * dx + dy * dy + dz * dz;

            // Прерываем патрулирование, если обнаружили игрока.
            // Также завершаем перелёт при достижении точки
            // или по истечении 100 тиков.
            return this.spirit.getTarget() == null
                    && distanceSquared > 2.25
                    && this.flightTicks < 100;
        }

        /**
         * Выбираем новую точку для перелёта.
         */
        @Override
        public void start() {

            this.flightTicks = 0;

            // Выбираем случайное горизонтальное направление.
            double angle = this.spirit.getRandom().nextDouble()
                    * Math.PI * 2.0;

            // Расстояние перелёта: от 2.5 до 6 блоков.
            double distance = 2.5
                    + this.spirit.getRandom().nextDouble() * 3.5;

            // Вычисляем координаты новой точки.
            this.targetX = this.spirit.flightCenterX
                    + Math.cos(angle) * distance;

            this.targetZ = this.spirit.flightCenterZ
                    + Math.sin(angle) * distance;

            // Высота меняется незначительно: максимум на 0.6 блока.
            this.targetY = this.spirit.flightCenterY
                    + (this.spirit.getRandom().nextDouble() * 1.2 - 0.6);

            // Начинаем плавный перелёт.
            this.spirit.getMoveControl().setWantedPosition(
                    this.targetX,
                    this.targetY,
                    this.targetZ,
                    0.35);
        }

        /**
         * Обновляем время текущего перелёта.
         */
        @Override
        public void tick() {
            this.flightTicks++;
        }

        /**
         * Останавливаемся после завершения перелёта.
         */
        @Override
        public void stop() {

            // Сбрасываем текущую команду движения.
            this.spirit.getMoveControl().setWait();

            // Даём духу 3 секунды отдыха.
            this.spirit.flightPauseTicks = 60;
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