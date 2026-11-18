package com.example.darkspirit.client.model;

// Сущность нашего копья.
import com.example.darkspirit.entity.DarkSpiritSpearEntity;

// GeckoLib: базовые классы модели.
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

// Minecraft: идентификаторы ресурсов.
import net.minecraft.resources.Identifier;

/**
 * Описывает геометрию, текстуру и анимации копья.
 */
public class DarkSpiritSpearModel extends GeoModel<DarkSpiritSpearEntity> {

    // Путь к геометрии копья.
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(
            "dark_spirit",
            "entity/dark_spirit_spear");

    // Путь к текстуре копья.
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "dark_spirit",
            "textures/entity/dark_spirit_spear.png");

    // Используем общий файл анимаций Духа тьмы.
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
     * Возвращает общий файл анимаций.
     */
    @Override
    public Identifier getAnimationResource(DarkSpiritSpearEntity animatable) {
        return ANIMATIONS;
    }
}