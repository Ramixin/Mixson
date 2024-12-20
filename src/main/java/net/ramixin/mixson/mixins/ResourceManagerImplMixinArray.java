package net.ramixin.mixson.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.ramixin.mixson.Mixson;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin({LifecycledResourceManagerImpl.class, ReloadableResourceManagerImpl.class})
public class ResourceManagerImplMixinArray {

    @ModifyReturnValue(method = "findResources", at = @At("RETURN"))
    private Map<Identifier, Resource> runMixsonEvents(Map<Identifier, Resource> original) {
        return Mixson.runEvents(original);
    }

}
