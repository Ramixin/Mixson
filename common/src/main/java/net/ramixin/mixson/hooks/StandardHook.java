package net.ramixin.mixson.hooks;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.MixsonUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class StandardHook extends AbstractHook<Map<Identifier, Resource>> {

    public StandardHook(Map<Identifier, Resource> attachedResources) {
        super(attachedResources);
    }

    @Override
    public Optional<List<Resource>> captureFiles(@NotNull Index index, String fileExt) {
        if(index.ordinal() > 0)
            return Optional.empty();
        Resource resource = this.attachedResources.get(index.id().withSuffix(fileExt));
        if(resource == null) return Optional.empty();
        return Optional.of(List.of(resource));
    }

    @Override
    public List<Map.Entry<Index, Resource>> getMatching(Predicate<Index> predicate) {
        return this.attachedResources
                .entrySet()
                .stream()
                .filter((entry) -> predicate.test(new Index(entry.getKey(), 0)))
                .map(entry -> Map.entry(new Index(MixsonUtil.removeExtension(entry.getKey()), 0), entry.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void insert(@NotNull Index index, List<Resource> resources, String fileExt, boolean overwrite) {
        if(resources.isEmpty())
            throw new IllegalArgumentException("Cannot insert empty resource list");
        if(index.ordinal() > 0)
            throw new IllegalArgumentException("Resource type does not support ordinal indexing");
        if(this.attachedResources.put(index.id().withSuffix(fileExt), resources.getFirst()) == null && overwrite)
            throw new IllegalStateException("Cannot overwrite resource: Resource with id " + index + " does not exists");
    }

    @Override
    public void delete(Index index, String fileExt) {
        if(index.ordinal() < 1)
            this.attachedResources.remove(index.id().withSuffix(fileExt));
    }
}
