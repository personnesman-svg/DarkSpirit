package com.example.darkspirit.entity;

// GeckoLib: поддержка модели.
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

// Minecraft: сущности и снаряды.
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;

// Minecraft: столкновение с блоком и вектор движения.
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// Minecraft: данные сущности.
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Minecraft: обработка урона.
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;

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
     * Пока запрещаем столкновения с другими сущностями.
     * Позже разрешим попадание только по игрокам.
     */
    @Override
    protected boolean canHitEntity(Entity entity) {
        return false;
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
     * Возвращает хранилище GeckoLib.
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}