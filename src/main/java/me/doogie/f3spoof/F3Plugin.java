package me.doogie.f3spoof;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.rusherhack.client.api.Globals;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Doogie13
 * @since 15/07/2024
 */
public class F3Plugin extends Plugin implements Globals {

    DebugScreenOverlay old = null;
    Field field;

    @Override
    public void onLoad() {

        Optional<Field> fieldO = Arrays.stream(mc.gui.getClass().getDeclaredFields())
                .peek(it -> it.setAccessible(true))
                .peek(it -> {
                    try {
                        Object o = it.get(mc.gui);
                        if (o == null)
                            logger.info("null class " + it.getName());
                        else
                            logger.info(o.getClass().getSimpleName());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(it -> {
                    try {
                        return it.get(mc.gui) instanceof DebugScreenOverlay;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .findAny();

        if (fieldO.isEmpty()) {
            this.getLogger().error("No DebugScreenOverlay field found in Gui class");
            return;
        }

        try {
            field = fieldO.get();
            field.setAccessible(true);
            old = (DebugScreenOverlay) field.get(mc.gui);
            field.set(mc.gui, new DebugScreenOverlayF3Spoof(mc));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // add F3 Spoof only after setting up fully
        RusherHackAPI.getModuleManager().registerFeature(new F3Spoof());

    }

    @Override
    public void onUnload() {
        if (field != null && old != null)
            try {
                field.set(mc.gui, old);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
    }

}