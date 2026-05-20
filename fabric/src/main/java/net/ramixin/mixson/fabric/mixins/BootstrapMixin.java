package net.ramixin.mixson.fabric.mixins;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.Bootstrap;
import net.ramixin.mixson.Mixson;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.nio.file.Path;

@Mixin(Bootstrap.class)
public class BootstrapMixin {

    @Inject(method = "bootStrap", at = @At("TAIL"))
    private static void setMixsonGameDirectory(CallbackInfo ci) {
        Path gameDirectory = FabricLoader.getInstance().getGameDir();
        try {
            Field gameDirField = Mixson.class.getDeclaredField("gameDirectory");
            gameDirField.setAccessible(true);
            gameDirField.set(null, gameDirectory);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try {
            FileUtils.deleteDirectory(gameDirectory.resolve(".mixson").toFile());
        } catch (Exception e) {
            LoggerFactory.getLogger(Mixson.class).error("failed to delete .mixson debug directory", e);
        }

    }
}
