package com.example.darkspirit.client.model;

import com.example.darkspirit.entity.DarkSpiritEntity;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.resources.Identifier;

/**
 * Описывает ресурсы модели Духа тьмы для GeckoLib.
 */
public class DarkSpiritModel extends GeoModel<DarkSpiritEntity> {

    // Путь к геометрии модели.
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(
            "dark_spirit",
            "dark_spirit");

    // Путь к текстуре существа.
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "dark_spirit",
            "textures/entity/dark_spirit.png");

    // Путь к файлу анимаций.
    private static final Identifier ANIMATIONS = Identifier.fromNamespaceAndPath(
            "dark_spirit",
            "dark_spirit");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(DarkSpiritEntity entity) {
        return ANIMATIONS;
    }
}