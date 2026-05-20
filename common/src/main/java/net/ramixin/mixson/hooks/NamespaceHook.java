package net.ramixin.mixson.hooks;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.MixsonUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@ApiStatus.Internal
public class NamespaceHook extends AbstractHook<List<Resource>> {

    private final Identifier Identifier;

    public NamespaceHook(List<Resource> attachedResources, Identifier Identifier) {
        super(attachedResources);
        this.Identifier = Identifier;
    }

    @Override
    public Optional<List<Resource>> captureFiles(Index index, String fileExt) {
        if(!index.id().withSuffix(fileExt).equals(this.Identifier))
            return Optional.empty();
        if(index.ordinal() == -1) return Optional.of(this.attachedResources);
        return Optional.of(List.of(this.attachedResources.get(index.ordinal())));
    }

    @Override
    public List<Map.Entry<Index, Resource>> getMatching(Predicate<Index> predicate) {
        List<Map.Entry<Index, Resource>> result = new ArrayList<>();
        Identifier withoutExtension = MixsonUtil.removeExtension(this.Identifier);
        for(int i = 0; i < this.attachedResources.size(); i++) {
            if(!predicate.test(new Index(this.Identifier, i))) continue;
            result.add(Map.entry(new Index(withoutExtension, i), this.attachedResources.get(i)));
        }
        return result;
    }

    @Override
    public void insert(Index index, List<Resource> resources, String fileExt, boolean overwrite) {
        if(!index.id().withSuffix(fileExt).equals(this.Identifier))
            throw new IllegalArgumentException("Namespace Hook cannot process index " + index + " with id: " + this.Identifier);
        if(index.ordinal() == -1) {
            if(!overwrite)
                throw new IllegalStateException("Cannot set resource: missing overwrite permission");
            this.attachedResources.clear();
            this.attachedResources.addAll(resources);
            return;
        }
        if(resources.size() != 1)
            throw new IllegalArgumentException("Cannot insert resource: list with index "+index);
        Resource resource = resources.getFirst();
        if(index.ordinal() >= this.attachedResources.size())
            throw new IllegalStateException("Cannot overwrite resource: Resource with index " + index + " does not exists");
        if(overwrite)
            this.attachedResources.set(index.ordinal(), resource);
        else
            attachedResources.add(index.ordinal(), resource);
    }

    @Override
    public void delete(Index index, String fileExt) {
        if(!index.id().withSuffix(fileExt).equals(this.Identifier))
            throw new IllegalArgumentException("Namespace Hook cannot process index " + index + "with id: " + this.Identifier);
        if(index.ordinal() >= this.attachedResources.size())
            throw new IllegalStateException("Cannot delete resource: Resource with index " + index + " does not exists");
        if(index.ordinal() == -1)
            this.attachedResources.clear();
        else
            this.attachedResources.remove(index.ordinal());
    }
}
