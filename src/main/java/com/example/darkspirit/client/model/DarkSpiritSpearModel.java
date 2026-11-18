package com.example.darkspirit.client.model;

// GeckoLib: базовые классы для 3D-модели.
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

// Minecraft: идентификаторы ресурсов.
import net.minecraft.resources.Identifier;

/**
 * Описывает модель и текстуру копья Духа тьмы.
 *
 * T — тип объекта GeckoLib, который будет использовать эту модель.
 * Позже здесь укажем нашу сущность копья.
 */
public class DarkSpiritSpearModel<T extends GeoAnimatable> extends GeoModel<T> {

    // Путь к геометрии копья.
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(
            "dark_spirit",
            "entity/dark_spirit_spear");

    // Путь к текстуре копья.
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "dark_spirit",
            "textures/entity/dark_spirit_spear.png");

    // Используем существующий файл анимаций Духа тьмы.
    private static final Identifier ANIMATIONS = Identifier.fromNamespaceAndPath(
            "dark_spirit",
            "entity/dark_spirit");

    /**
     * Возвращает геометрию копья.
     */
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    /**
     * Возвращает текстуру копья.
     */
    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    /**
     * Возвращает файл анимаций.
     */
    @Override
    public Identifier getAnimationResource(T animatable) {
        return ANIMATIONS;
    }
}