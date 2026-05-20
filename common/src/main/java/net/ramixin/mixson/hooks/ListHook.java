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
public class ListHook extends AbstractHook<Map<Identifier, List<Resource>>> {

    public ListHook(Map<Identifier, List<Resource>> attachedResources) {
        super(attachedResources);
    }

    @Override
    public Optional<List<Resource>> captureFiles(Index index, String fileExt) {
        List<Resource> list = this.attachedResources.get(index.id().withSuffix(fileExt));
        if(list == null) return Optional.empty();
        if(index.ordinal() == -1) return Optional.of(list);
        return Optional.of(List.of(list.get(index.ordinal())));
    }

    @Override
    public List<Map.Entry<Index, Resource>> getMatching(Predicate<Index> predicate) {
        List<Map.Entry<Index, Resource>> result = new ArrayList<>();
        for(Map.Entry<Identifier, List<Resource>> entry : this.attachedResources.entrySet()) {
            for(int i = 0; i < entry.getValue().size(); i++) {
                if(predicate.test(new Index(entry.getKey(), i))) {
                    result.add(Map.entry(new Index(MixsonUtil.removeExtension(entry.getKey()), i), entry.getValue().get(i)));
                }
            }
        }
        return result;
    }

    @Override
    public void insert(Index index, List<Resource> resources, String fileExt, boolean overwrite) {
        if(resources.isEmpty())
            throw new IllegalArgumentException("Cannot insert empty resource list");
        Index suffixedIndex = index.withSuffixedId(fileExt);
        List<Resource> immutableList = this.attachedResources.get(suffixedIndex.id());
        if(index.ordinal() == -1) {
            if(immutableList == null)
                this.attachedResources.put(suffixedIndex.id(), resources);
            else if(!overwrite)
                throw new IllegalStateException("Cannot set list resource without overwrite permission");
            else this.attachedResources.put(suffixedIndex.id(), resources);
            return;
        }
        if(resources.size() != 1)
            throw new IllegalArgumentException("Cannot insert resource list with index "+index);
        Resource resource = resources.getFirst();
        List<Resource> mutableList = new ArrayList<>(immutableList != null ? immutableList : List.of());
        if(overwrite)
            mutableList.set(index.ordinal(), resource);
        else
            mutableList.add(index.ordinal(), resource);
        this.attachedResources.put(suffixedIndex.id(), mutableList);
    }

    @Override
    public void delete(Index index, String fileExt) {
        List<Resource> immutableList = this.attachedResources.get(index.id().withSuffix(fileExt));
        if(immutableList == null) return;
        List<Resource> mutableList = new ArrayList<>(immutableList);
        if(index.ordinal() >= mutableList.size())
            throw new IllegalStateException("Cannot delete resource: Resource with index " + index + " does not exists");
        mutableList.remove(index.ordinal());
        this.attachedResources.put(index.id().withSuffix(fileExt), mutableList);
    }
}
