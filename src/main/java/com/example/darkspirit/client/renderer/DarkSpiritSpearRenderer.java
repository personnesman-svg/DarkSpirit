package com.example.darkspirit.client.renderer;

// Модель и сущность копья.
import com.example.darkspirit.client.model.DarkSpiritSpearModel;
import com.example.darkspirit.entity.DarkSpiritSpearEntity;

// GeckoLib: данные рендеринга и базовый рендерер сущностей.
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

// Minecraft: вращение модели во время отрисовки.
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Рендерер копья, который поворачивает модель по направлению полёта.
 *
 * Стандартный GeoEntityRenderer поворачивает эту сущность по горизонтали,
 * но не наклоняет её модель по вертикальному углу траектории.
 */
public class DarkSpiritSpearRenderer
        extends GeoEntityRenderer<DarkSpiritSpearEntity, EntityRenderState> {

    public DarkSpiritSpearRenderer(EntityRendererProvider.Context context) {
        super(context, new DarkSpiritSpearModel());
    }

    /**
     * Применяет горизонтальный поворот GeckoLib и добавляет наклон по высоте.
     */
    @Override
    protected void applyRotations(
            RenderPassInfo<EntityRenderState> renderPassInfo,
            PoseStack poseStack,
            float nativeScale) {

        // Сохраняем стандартный горизонтальный поворот GeckoLib.
        super.applyRotations(renderPassInfo, poseStack, nativeScale);

        // Берём угол наклона, который сущность вычисляет из скорости.
        float pitch = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0.0F);

        // Поворачиваем модель вокруг X, чтобы острие смотрело по траектории.
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }
}
