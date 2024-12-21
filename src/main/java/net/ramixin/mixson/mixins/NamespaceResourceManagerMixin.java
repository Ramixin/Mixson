package net.ramixin.mixson.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.ramixin.mixson.Mixson;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(NamespaceResourceManager.class)
public class NamespaceResourceManagerMixin {


    @ModifyReturnValue(method = "getAllResources", at = @At("RETURN"))
    private List<Resource> runMixsonEvents(List<Resource> original, @Local(argsOnly = true) Identifier id) {
        return Mixson.runEvents(original, id);
    }

}
