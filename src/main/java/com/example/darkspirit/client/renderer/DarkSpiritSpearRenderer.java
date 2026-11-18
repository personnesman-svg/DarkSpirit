package com.example.darkspirit.client.renderer;

// Сущность и модель копья.
import com.example.darkspirit.client.model.DarkSpiritSpearModel;
import com.example.darkspirit.entity.DarkSpiritSpearEntity;

// Штатный GeckoLib-рендерер для снарядов, летящих остриём вперёд.
import com.geckolib.renderer.specialty.DirectionalProjectileRenderer;

// Minecraft: состояние отрисовки и контекст рендерера.
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Рендерер копья Духа тьмы.
 *
 * DirectionalProjectileRenderer автоматически поворачивает модель
 * по горизонтальному и вертикальному направлению полёта.
 */
public class DarkSpiritSpearRenderer
        extends DirectionalProjectileRenderer<DarkSpiritSpearEntity, EntityRenderState> {

    public DarkSpiritSpearRenderer(EntityRendererProvider.Context context) {
        super(context, new DarkSpiritSpearModel());
    }
}
