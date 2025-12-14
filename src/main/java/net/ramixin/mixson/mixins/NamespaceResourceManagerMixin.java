package net.ramixin.mixson.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.inline.Mixson;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(FallbackResourceManager.class)
public class NamespaceResourceManagerMixin {


    @ModifyReturnValue(method = "getResourceStack", at = @At("RETURN"))
    private List<Resource> runMixsonEvents(List<Resource> original, @Local(argsOnly = true) ResourceLocation id) {
        return Mixson.runNamespaceEvents(original, id);
    }

    @ModifyReturnValue(method = "getResource", at = @At("RETURN"))
    private Optional<Resource> runMixsonEvents(Optional<Resource> original, ResourceLocation id) {
        if(original.isEmpty()) return Optional.empty();
        List<Resource> result = Mixson.runNamespaceEvents(new ArrayList<>(List.of(original.get())), id);
        return Optional.ofNullable(result.get(0));
    }

}
