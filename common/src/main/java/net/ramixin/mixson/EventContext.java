package net.ramixin.mixson;

import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.entries.EventEntry;
import net.ramixin.mixson.util.Index;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.IOException;
import java.util.*;

import static net.ramixin.mixson.util.MixsonUtil.deserializeFile;
import static net.ramixin.mixson.util.MixsonUtil.overlappingIndices;

@SuppressWarnings("unused")
public class EventContext<T> {

    private final Mutable<T> file;
    private final Index index;
    private final EventEntry<T> entry;
    private boolean markedForDeletion;
    private final MixsonRuntime<?> runtime;
    private final Map.Entry<Index, Resource> resourceEntry;
    private final Set<UUID> cancelledFutures = new HashSet<>();
    private final List<UUID> pulledFutures = new ArrayList<>();
    private final HashMap<Index, T> createdResources = new HashMap<>();
    private final Mutable<Optional<T>> debugExportObject;
    private final HashMap<Index, List<Mutable<T>>> capturedFiles = new HashMap<>();

    protected EventContext(T file, Index index, EventEntry<T> entry, MixsonRuntime<?> runtime, Map.Entry<Index, Resource> resourceEntry, boolean markedForDeletion) {
        this.file = new MutableObject<>(file);
        this.debugExportObject = new MutableObject<>(Optional.empty());
        this.index = index;
        this.entry = entry;
        this.markedForDeletion = markedForDeletion;
        this.runtime = runtime;
        this.resourceEntry = resourceEntry;
    }

    public T getFile() {
        return this.file.get();
    }

    public void setFile(T file) {
        this.file.setValue(file);
    }

    public Index getIndex() {
        return this.index;
    }

    public String getEventName() {
        return getEvent().eventName();
    }

    public MixsonEvent<T> getEvent() {
        return entry.event();
    }

    public UUID getEventUUID() {
        return entry.event().uuid();
    }

    public int getPriority() {
        return entry.priority();
    }

    public void markForDeletion(boolean shouldDelete) {
        this.markedForDeletion = shouldDelete;
    }

    public void createResource(Index id, T elem) {
        this.createdResources.put(id, elem);
    }

    public void cancelFutureEvent(UUID uuid) {
        this.cancelledFutures.add(uuid);
    }

    public void pullIntoRuntime(UUID uuid) {
        this.pulledFutures.add(uuid);
    }

    public List<Mutable<T>> captureFiles(Index id) {
        if(overlappingIndices(capturedFiles.keySet(), id)) {
            runtime.error(new MixsonException("cannot capture same file twice"), this.getEvent(), resourceEntry.getKey().id());
            return List.of();
        }
        Optional<List<Resource>> maybeResourceList = runtime.getHook().captureFiles(id, this.getEvent().codec().extensionAndDot());
        if(maybeResourceList.isEmpty()) return List.of();
        List<Resource> resourceList = maybeResourceList.get();
        List<Mutable<T>> resultList = new ArrayList<>();
        List<Mutable<T>> keepingList = new ArrayList<>();
        for(Resource r : resourceList) {
            T deserializedFile = deserializeFile(this.getEvent().codec(), r, error -> runtime.error(error, getEvent(), resourceEntry.getKey().id())).orElse(null);
            resultList.add(new MutableObject<>(deserializedFile));
            if(deserializedFile != null) keepingList.add(new MutableObject<>(deserializedFile));
        }
        capturedFiles.put(id, keepingList);
        return resultList;
    }

    public void setDebugExport(T result) {
        this.debugExportObject.setValue(Optional.of(result));
    }

    public void cancelDebugExport() {
        this.debugExportObject.setValue(null);
    }

    protected Optional<T> getDebugExport() {
        //noinspection OptionalAssignedToNull
        if(this.debugExportObject.get() == null)
            return Optional.empty();
        if(this.debugExportObject.get().isEmpty())
            return Optional.of(file.get());
        return this.debugExportObject.get();
    }

    protected boolean isMarkedForDeletion() {
        return this.markedForDeletion;
    }

    protected Set<UUID> getCancelledFutures() {
        return this.cancelledFutures;
    }

    protected List<UUID> getPulledFutures() {
        return pulledFutures;
    }

    protected HashMap<Index, T> getCreatedResources() {
        return this.createdResources;
    }

    protected void cleanupCapturedFiles() throws IOException {
        for(Map.Entry<Index, List<Mutable<T>>> captureEntry : capturedFiles.entrySet()) {
            List<Resource> resources = new ArrayList<>(captureEntry.getValue().size());
            for(Mutable<T> resource : captureEntry.getValue()) {
                T resourceFile = resource.get();
                if(resourceFile == null) {
                    runtime.error(new MixsonException("captured file with index {} cannot be null", captureEntry.getKey()), getEvent(), captureEntry.getKey().id());
                }
                resources.add(getEvent().codec().serialize(resourceEntry.getValue(), resourceFile));
            }
            runtime.getHook().insert(captureEntry.getKey(), resources, getEvent().codec().extensionAndDot(), true);
        }
    }
}
