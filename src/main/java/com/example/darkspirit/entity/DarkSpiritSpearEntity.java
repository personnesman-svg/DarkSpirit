package com.example.darkspirit.entity;

// GeckoLib: поддержка анимационных моделей.
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

// Minecraft: базовые классы сущностей.
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

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
 * Entity — базовый класс всех объектов игрового мира.
 * GeoEntity — подключает поддержку моделей GeckoLib.
 *
 * Позже этот класс будет отвечать за:
 * полёт копья, столкновения, урон и застревание в блоках.
 */
public class DarkSpiritSpearEntity extends Entity implements GeoEntity {

    // Хранилище GeckoLib для этой сущности.
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /**
     * Основной конструктор сущности.
     */
    public DarkSpiritSpearEntity(
            EntityType<? extends DarkSpiritSpearEntity> entityType,
            Level level) {

        super(entityType, level);
    }

    /**
     * Обновление сущности каждый игровой тик.
     */
    @Override
    public void tick() {

        // Выполняем стандартное обновление Minecraft.
        super.tick();

        // Логику полёта добавим на следующем этапе.
    }

    /**
     * Загружает сохранённые данные сущности.
     */
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // Пока дополнительных данных нет.
    }

    /**
     * Сохраняет данные сущности.
     */
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // Позже здесь сохраним нужные координаты и состояние копья.
    }

    /**
     * Определяет синхронизируемые данные сущности.
     */
    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder) {
        // Пока синхронизируемых данных нет.
    }

    /**
     * Запрещаем наносить урон самому объекту копья.
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

        // Для отдельного копья пока не запускаем анимации.
        // Направление модели будем задавать по движению.
    }

    /**
     * Возвращает хранилище GeckoLib.
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}