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
    // Однократная анимация появления Духа после телепортации.
    private static final RawAnimation TELEPORT = RawAnimation.begin().thenPlay("teleport");

    // Радиус поиска ближайшего игрока.
    private static final double PLAYER_DETECTION_RANGE = 24.0D;

    // Интервал между выстрелами: 40 тиков = 2 секунды.
    private static final int SPEAR_COOLDOWN_TICKS = 40;

    // Счётчик перезарядки копья.
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

            // Уменьшаем время до следующего выстрела.
            if (this.spearCooldownTicks > 0) {
                this.spearCooldownTicks--;
            }

            // Каждый серверный тик ищем ближайшего живого игрока.
            // Поиск не зависит от NearestAttackableTargetGoal и игрового режима.
            Player nearestPlayer = this.findNearestPlayer();

            // Обновляем цель только при её фактической смене.
            // Это не даёт ИИ лишний раз сбрасывать текущее состояние цели.
            if (this.getTarget() != nearestPlayer) {
                this.setTarget(nearestPlayer);
            }

            // Поворачиваем голову и корпус прямо к игроку.
            if (nearestPlayer != null) {
                this.facePlayer(nearestPlayer);
            }

            // Самый простой тестовый боевой цикл:
            // если игрок найден, сразу выпускаем копьё в его сторону.
            // Не ждём анимацию, дистанцию остановки или отдельную AI-цель.
            if (nearestPlayer != null && this.spearCooldownTicks <= 0) {

                DarkSpirit.LOGGER.info(
                        "[DarkSpirit] Игрок найден, выпускаю копьё в сторону {}",
                        nearestPlayer.getName().getString());

                // Перезарядка устанавливается сразу, чтобы не создавать
                // десятки копий за один игровой тик.
                this.spearCooldownTicks = SPEAR_COOLDOWN_TICKS;

                // Создаём и запускаем копьё прямо сейчас.
                this.throwSpearAt(nearestPlayer);
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
     * Поворачивает голову и корпус Духа к указанному игроку.
     */
    private void facePlayer(Player target) {

        // Вычисляем горизонтальное направление от Духа к игроку.
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();

        // Вычисляем вертикальную разницу между глазами Духа и игрока.
        double dy = target.getEyeY() - this.getEyeY();

        // Горизонтальное расстояние нужно для расчёта вертикального угла.
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Преобразуем направление по горизонтали в угол Minecraft.
        // Нулевой угол соответствует взгляду вдоль положительной оси Z.
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;

        // Рассчитываем наклон вверх или вниз к глазам игрока.
        // В Minecraft отрицательный pitch означает взгляд вверх.
        float pitch = (float) (
                -Math.atan2(dy, horizontalDistance) * 180.0D / Math.PI);

        // Поворачиваем корпус, голову и вертикальный угол взгляда к игроку.
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);

        // Оставляем LookControl активным для дальнейшей работы ИИ.
        this.getLookControl().setLookAt(target, 180.0F, 180.0F);
    }

    /**
     * Запускает анимацию телепортации Духа.
     *
     * Событие отправляется с сервера, чтобы GeckoLib синхронизировал
     * запуск анимации с клиентами, которые видят эту сущность.
     */
    public void playTeleportAnimation() {
        if (!this.level().isClientSide()) {
            this.triggerAnim("base", "teleport");
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
     * Создаёт и запускает копьё в сторону цели.
     */
    private void throwSpearAt(LivingEntity target) {

        // Снаряды создаём исключительно на сервере.
        if (this.level().isClientSide() || target == null || !target.isAlive()) {
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

        // Назначаем владельца — Духа тьмы.
        spear.setOwner(this);

        // Задаём скорость напрямую: 0,9 блока за тик в сторону игрока.
        // Нормализация гарантирует одинаковую скорость при любом расстоянии.
        Vec3 velocity = direction.normalize().scale(0.9D);
        spear.setDeltaMovement(velocity);

        // Поворачиваем снаряд по направлению движения.
        // Поворачиваем копьё носом по направлению скорости.
        spear.alignToVelocity();

        // Добавляем снаряд в мир.
        boolean added = this.level().addFreshEntity(spear);

        // Подтверждаем создание копья в журнале игры.
        if (added) {
            DarkSpirit.LOGGER.info(
                    "[DarkSpirit] КОПЬЁ ДОБАВЛЕНО: скорость={}, позиция={}, цель={}",
                    spear.getDeltaMovement(),
                    spear.position(),
                    target.getName().getString());
        } else {
            DarkSpirit.LOGGER.error(
                    "[DarkSpirit] ОШИБКА: Minecraft не добавил копьё в мир.");
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
                        // Позволяем временно заменить полёт или покой анимацией телепортации.
                        .triggerableAnim("teleport", TELEPORT));
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

            // Вычисляем разницу по горизонтали между Духом и игроком.
            double dx = target.getX() - this.spirit.getX();
            double dz = target.getZ() - this.spirit.getZ();

            // Сравниваем высоту центров сущностей, а не только их координаты ног.
            double spiritCenterY = this.spirit.getY()
                    + this.spirit.getBbHeight() * 0.5D;
            double targetCenterY = target.getY()
                    + target.getBbHeight() * 0.5D;
            double dy = targetCenterY - spiritCenterY;

            // Полное трёхмерное расстояние учитывает и высоту игрока.
            double distanceSquared = dx * dx + dy * dy + dz * dz;

            // Если игрок дальше 6 блоков в любом направлении — продолжаем сближение.
            if (distanceSquared > 36.0D) {

                // Выбираем высоту, при которой центры Духа и игрока совпадут.
                double wantedY = target.getY()
                        + (target.getBbHeight() - this.spirit.getBbHeight()) * 0.5D;

                this.spirit.getMoveControl().setWantedPosition(
                        target.getX(),
                        wantedY,
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