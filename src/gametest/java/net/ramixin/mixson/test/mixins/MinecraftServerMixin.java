package net.ramixin.mixson.test.mixins;

import net.minecraft.server.MinecraftServer;
import net.ramixin.mixson.test.MixsonTestEntrypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void initMixsonTests(CallbackInfo ci) {
        MixsonTestEntrypoint.onInitialize();
        System.out.println("Mixson tests initialized");
    }

}
