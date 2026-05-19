package net.ramixin.mixson.hooks;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.EventContext;

import java.util.*;
import java.util.function.Predicate;

/**
 * Base abstraction for a resource collection that Mixson can read from and write to. All methods are intended to be
 * used exclusively by Mixson. Calling these methods externally is not supported and may result in undefined behavior.
 *
 * @param <T> the resource collection managed by this hook
 */
public abstract class AbstractHook<T> {

    protected final T attachedResources;

    /**
     * implementation details:
     * 
     * @param attachedResources the collection that will be managed by this hook. Hooks are expected to take ownership
     *                          of the collection that is passed to them. Ownership is not returned until
     *                          {@link #getAttachedResources()} is called at the end of its processing
     *                          by {@link Mixson#processHook(AbstractHook)}.
     */
    public AbstractHook(T attachedResources) {
        this.attachedResources = attachedResources;
    }

    /**
     * Retrieves the files for the {@link EventContext#captureFiles(Index)} method.
     * <p>
     * Implementation details:
     * 
     * @return an optional list of resources that match the given index and file extension. If the list is empty, an
     * empty optional is returned instead. Indexes that cannot be found should cause an empty optional to be returned.
     */
    public abstract Optional<List<Resource>> captureFiles(Index index, String fileExt);

    public abstract List<Map.Entry<Index, Resource>> getMatching(Predicate<Index> predicate);

    public abstract void insert(Index index, List<Resource> resources, String fileExt, boolean overwrite);

    public void insert(Index index, Resource resource, String fileExt, boolean overwrite) {
        insert(index, List.of(resource), fileExt, overwrite);
    }

    /**
     * Removes the resource at the given index from the collection.
     * <p>
     * Implementation details:
     * <p>
     * The method is expected to return silently if there is no resource for the {@link Identifier} of the given index.
     * The method is allowed to throw if the ordinal is out of bounds, assuming it matches one or more resources.
     */
    public abstract void delete(Index index, String fileExt);

    /**
     * Gets the collection managed by this hook. Only Mixson should call this method. Calling this method is assumed
     * to resemble the end of the hook's processing.
     */
    public T getAttachedResources() {
        return attachedResources;
    }
}
