package com.example.darkspirit.entity;

// GeckoLib: поддержка модели.
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

// Minecraft: реестр, идентификаторы и тип источника урона.
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

// Minecraft: эффекты, сущности и снаряды.
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;

// Minecraft: столкновения и вектор движения.
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

// Minecraft: данные сущности.
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Minecraft: серверная обработка урона.
import net.minecraft.server.level.ServerLevel;

/**
 * Отдельная сущность копья Духа тьмы.
 *
 * ThrowableProjectile — базовый класс летающих снарядов.
 * GeoEntity — подключает модель GeckoLib.
 */
public class DarkSpiritSpearEntity extends ThrowableProjectile implements GeoEntity {

    // Хранилище GeckoLib.
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Показывает, застряло ли копьё в блоке.
    private boolean stuckInBlock = false;

    // Отдельный тип урона копья, который не блокируется щитом.
    private static final ResourceKey<DamageType> SPEAR_DAMAGE_TYPE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    Identifier.fromNamespaceAndPath("dark_spirit", "dark_spirit_spear"));

    // Четыре сердца равны восьми единицам здоровья.
    private static final float SPEAR_DAMAGE = 8.0F;

    // Продолжительность Иссушения: 5 секунд.
    private static final int WITHER_DURATION_TICKS = 100;

    /**
     * Создаёт копьё.
     */
    public DarkSpiritSpearEntity(
            EntityType<? extends DarkSpiritSpearEntity> entityType,
            Level level) {

        super(entityType, level);

        // Копьё летит без воздействия гравитации.
        this.setNoGravity(true);
    }

    /**
     * Устанавливает направление и скорость полёта копья.
     *
     * owner — сущность, которая бросила копьё.
     * direction — направление полёта.
     * speed — скорость снаряда.
     */
    public void launch(Entity owner, Vec3 direction, float speed) {

        // Не запускаем копьё, если направление нулевое.
        if (direction.lengthSqr() < 1.0E-7D) {
            return;
        }

        // Запоминаем владельца копья.
        this.setOwner(owner);

        // Нормализуем направление.
        Vec3 normalizedDirection = direction.normalize();

        // Устанавливаем скорость напрямую по нормализованному направлению.
        // Так снаряд не получает случайного разброса.
        this.setDeltaMovement(normalizedDirection.scale(speed));
    }

    /**
     * Убираем влияние гравитации.
     */
    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    /**
     * Обновление копья каждый игровой тик.
     */
    @Override
    public void tick() {

        // Если копьё застряло, оставляем его на месте.
        if (this.stuckInBlock) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        // Стандартное обновление снаряда Minecraft.
        super.tick();

        // Пока копьё летит, каждый тик направляем его нос по вектору скорости.
        if (!this.stuckInBlock) {
            this.alignToVelocity();
        }
    }

    /**
     * Выравнивает модель копья вдоль текущего направления полёта.
     */
    public void alignToVelocity() {

        Vec3 velocity = this.getDeltaMovement();

        if (velocity.lengthSqr() < 1.0E-7D) {
            return;
        }

        // Горизонтальная составляющая скорости нужна для расчёта наклона.
        double horizontalSpeed = Math.sqrt(
                velocity.x * velocity.x + velocity.z * velocity.z);

        // Модель направлена вдоль локальной оси Z.
        // Эти углы учитывают систему поворотов рендерера GeckoLib.
        float yaw = (float) (
                Math.atan2(velocity.z, velocity.x) * 180.0D / Math.PI)
                + 90.0F;

        // Отрицательный знак нужен, чтобы подъём траектории поднимал острие копья.
        float pitch = (float) (
                -Math.atan2(velocity.y, horizontalSpeed)
                        * 180.0D / Math.PI);

        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    /**
     * Обрабатываем столкновение копья с блоком.
     */
    @Override
    protected void onHitBlock(BlockHitResult hitResult) {

        // Выполняем стандартную обработку попадания в блок.
        super.onHitBlock(hitResult);

        // Перемещаем копьё точно к месту столкновения.
        this.setPos(hitResult.getLocation());

        // Полностью останавливаем движение.
        this.setDeltaMovement(Vec3.ZERO);

        // Запоминаем, что копьё застряло.
        this.stuckInBlock = true;
    }

    /**
     * Копьё сталкивается только с игроками.
     *
     * Другие существа не считаются целью и не останавливают снаряд.
     */
    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity instanceof Player && super.canHitEntity(entity);
    }

    /**
     * Наносим урон игроку и накладываем Иссушение.
     */
    @Override
    protected void onHitEntity(EntityHitResult hitResult) {

        // Урон и эффекты рассчитываются только на сервере.
        if (this.level().isClientSide()) {
            return;
        }

        if (hitResult.getEntity() instanceof Player player) {

            // Сохраняем владельца, чтобы урон был приписан Духу тьмы.
            Entity owner = this.getOwner();

            // Создаём отдельный источник урона копья.
            // Его тип помечен как проходящий сквозь щиты.
            DamageSource damageSource = this.damageSources().source(
                    SPEAR_DAMAGE_TYPE,
                    this,
                    owner);

            // Наносим восемь единиц урона — четыре сердца.
            boolean damageApplied = player.hurt(
                    damageSource,
                    SPEAR_DAMAGE);

            // Накладываем Иссушение только при успешном попадании.
            if (damageApplied) {
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.WITHER,
                                WITHER_DURATION_TICKS,
                                0),
                        owner);
            }
        }

        // После попадания в игрока копьё исчезает.
        this.discard();
    }

    /**
     * Загружает сохранённые данные.
     */
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }

    /**
     * Сохраняет данные.
     */
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    /**
     * Регистрирует синхронизируемые данные.
     */
    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder) {
    }

    /**
     * Запрещаем наносить урон самому копью.
     */
    @Override
    public boolean hurtServer(
            ServerLevel level,
            DamageSource damageSource,
            float amount) {

        return false;
    }

    /**
     * Регистрирует контроллеры GeckoLib.
     */
    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
    }

    /**
     * Возвращает хранилище анимаций GeckoLib.
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}