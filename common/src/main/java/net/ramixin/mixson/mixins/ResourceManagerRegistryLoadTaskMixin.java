package net.ramixin.mixson.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.hooks.StandardHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(ResourceManagerRegistryLoadTask.class)
public class ResourceManagerRegistryLoadTaskMixin {

    @WrapOperation(method = "lambda$load$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;"))
    private static Map<Identifier, Resource> runMixsonEvents(FileToIdConverter instance, ResourceManager resourceManager, Operation<Map<Identifier, Resource>> original) {
        return Mixson.processHook(new StandardHook(original.call(instance, resourceManager)));
    }
}
