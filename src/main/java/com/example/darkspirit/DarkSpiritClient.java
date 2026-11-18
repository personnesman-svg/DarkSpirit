package com.example.darkspirit;

// Рендерер GeckoLib.
import com.geckolib.renderer.GeoEntityRenderer;

// Событие регистрации рендереров NeoForge.
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.example.darkspirit.client.model.DarkSpiritModel;
import com.example.darkspirit.client.renderer.DarkSpiritSpearRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = DarkSpirit.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods
// in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = DarkSpirit.MODID, value = Dist.CLIENT)
public class DarkSpiritClient {

    public DarkSpiritClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your
        // mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json
        // file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /**
     * Регистрирует способы отображения сущностей нашего мода.
     */
    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {

        // Подключаем модель Духа тьмы.
        event.registerEntityRenderer(
                ModEntities.DARK_SPIRIT.get(),
                context -> new GeoEntityRenderer<>(
                        context,
                        new DarkSpiritModel()));

        // Подключаем рендерер копья с поддержкой горизонтального и вертикального поворота.
        event.registerEntityRenderer(
                ModEntities.DARK_SPIRIT_SPEAR.get(),
                DarkSpiritSpearRenderer::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        DarkSpirit.LOGGER.info("HELLO FROM CLIENT SETUP");
        DarkSpirit.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
