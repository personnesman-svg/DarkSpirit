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

    // Радиус, в котором Дух замечает игроков.
    private static final double PLAYER_DETECTION_RANGE = 24.0D;

    // Радиус начала броска копья по горизонтали.
    private static final double SPEAR_ATTACK_RANGE = 7.0D;

    // Перезарядка между бросками копья.
    private int spearCooldownTicks = 0;

    // Сколько тиков осталось до вылета копья после начала анимации атаки.
    // Значение -1 означает, что бросок сейчас не подготовлен.
    private int spearLaunchDelayTicks = -1;

    // Цель, которую Дух выбрал для текущего броска.
    // Поле используется только на сервере.
    private LivingEntity pendingSpearTarget;

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

        // Обязательно обновляем стандартную логику Minecraft:
        // ИИ, выбранную цель, GoalSelector, MoveControl и физику сущности.
        super.tick();

        // Боевой ИИ и создание снарядов выполняются только на сервере.
        if (!this.level().isClientSide()) {

            // Находим ближайшего живого игрока самостоятельно.
            // Это не использует ванильные боевые фильтры TargetingConditions,
            // которые не выбирают игроков в творческом режиме.
            LivingEntity previousTarget = this.getTarget();
            Player nearestPlayer = this.findNearestPlayer();

            // Устанавливаем ближайшего игрока целью.
            // Наблюдателей не выбираем, творческий режим для тестирования разрешён.
            this.setTarget(nearestPlayer);

            // Записываем в лог только смену цели, а не каждую игровую итерацию.
            if (nearestPlayer != previousTarget) {
                if (nearestPlayer != null) {
                    DarkSpirit.LOGGER.info(
                            "[DarkSpirit] Цель обнаружена: {}, расстояние={}",
                            nearestPlayer.getName().getString(),
                            this.distanceTo(nearestPlayer));
                } else if (previousTarget != null) {
                    DarkSpirit.LOGGER.info(
                            "[DarkSpirit] Игрок вышел из радиуса обнаружения.");
                }
            }

            // Уменьшаем перезарядку между атаками.
            if (this.spearCooldownTicks > 0) {
                this.spearCooldownTicks--;
            }

            // Обрабатываем задержку между анимацией замаха и вылетом копья.
            if (this.spearLaunchDelayTicks > 0) {
                this.spearLaunchDelayTicks--;

                // Когда задержка закончилась, запускаем копьё.
                if (this.spearLaunchDelayTicks == 0) {
                    LivingEntity target = this.pendingSpearTarget;

                    // Сбрасываем подготовленный бросок до создания снаряда.
                    this.pendingSpearTarget = null;
                    this.spearLaunchDelayTicks = -1;

                    // Если цель ещё жива, выполняем бросок.
                    if (target != null && target.isAlive()) {
                        this.throwSpearAt(target);
                    }
                }
            }

            // Атаку проверяем здесь, а не внутри ChasePlayerGoal.
            // Так запуск броска не зависит от частоты обновления GoalSelector.
            LivingEntity target = this.getTarget();

            if (target != null
                    && target.isAlive()
                    && this.spearCooldownTicks <= 0
                    && this.spearLaunchDelayTicks < 0) {

                // Для дистанции атаки учитываем только горизонтальную плоскость.
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                double horizontalDistanceSquared = dx * dx + dz * dz;

                // Начинаем атаку, когда игрок находится в заданном радиусе.
                if (horizontalDistanceSquared
                        <= SPEAR_ATTACK_RANGE * SPEAR_ATTACK_RANGE) {
                    this.startSpearAttack(target);
                }
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
     * Ищет ближайшего живого игрока в радиусе обнаружения.
     *
     * В отличие от NearestAttackableTargetGoal, этот метод временно
     * разрешает обнаружение игроков в творческом режиме.
     * Это упрощает проверку атаки во время разработки мода.
     */
    private Player findNearestPlayer() {

        Player nearestPlayer = null;

        // Изначально считаем, что игрока ближе радиуса обнаружения нет.
        double nearestDistanceSquared =
                PLAYER_DETECTION_RANGE * PLAYER_DETECTION_RANGE;

        // Проверяем игроков в области вокруг Духа.
        for (Player player : this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(PLAYER_DETECTION_RANGE),
                candidate -> candidate.isAlive() && !candidate.isSpectator())) {

            // Сравниваем расстояние до текущего кандидата.
            double distanceSquared = this.distanceToSqr(player);

            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestPlayer = player;
            }
        }

        return nearestPlayer;
    }

    /**
     * Начинает подготовку броска копья.
     *
     * Этот метод вызывается из серверного tick() сущности,
     * поэтому он не зависит от обновления отдельной AI-цели.
     */
    private void startSpearAttack(LivingEntity target) {

        // Атаковать можно только на сервере.
        if (this.level().isClientSide()) {
            return;
        }

        // Не начинаем атаку без живой цели или во время перезарядки.
        if (target == null
                || !target.isAlive()
                || this.spearCooldownTicks > 0
                || this.spearLaunchDelayTicks >= 0) {
            return;
        }

        // Запоминаем цель до момента фактического броска.
        this.pendingSpearTarget = target;

        // Запускаем 5-секундную перезарядку.
        this.spearCooldownTicks = 100;

        // Копьё вылетит через 28 тиков (примерно 1,4 секунды).
        this.spearLaunchDelayTicks = 28;

        // Запускаем отдельную одноразовую анимацию броска GeckoLib.
        this.triggerAnim("base", "attack");

        // Диагностическое сообщение появляется только один раз за атаку.
        DarkSpirit.LOGGER.info(
                "[DarkSpirit] Начал подготовку броска в сторону {}",
                target.getName().getString());
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

        // Добавляем снаряд в мир с сервера.
        boolean added = this.level().addFreshEntity(spear);

        // Если сервер отказался добавить копьё, оставляем понятную причину в логе.
        if (added) {
            DarkSpirit.LOGGER.info(
                    "[DarkSpirit] Копьё создано: скорость={}, позиция={}",
                    spear.getDeltaMovement(),
                    spear.position());
        } else {
            DarkSpirit.LOGGER.error(
                    "[DarkSpirit] Minecraft отказался добавить сущность копья в мир.");
        }
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

        // Основной контроллер выбирает постоянную анимацию:
        // полёт или покой.
        // Анимация атаки зарегистрирована на этом же контроллере,
        // потому что она задействует всё тело и должна временно заменить idle/fly.
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
                        // Одноразовая анимация броска. При вызове triggerAnim
                        // GeckoLib временно проигрывает её вместо обычного состояния.
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

            // Атака проверяется отдельно в DarkSpiritEntity.tick().
            // Здесь Goal отвечает только за преследование и остановку перед игроком.
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