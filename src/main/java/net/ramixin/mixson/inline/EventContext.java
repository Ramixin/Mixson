package net.ramixin.mixson.inline;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.MixsonError;
import net.ramixin.mixson.inline.entries.EventEntry;
import net.ramixin.mixson.util.MixsonUtil;
import net.ramixin.mixson.util.ResourceLocationUtil;
import net.ramixin.mixson.util.ResourceLocator;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.*;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public class EventContext<T> {

    private final ContextCreationType creationType;
    private final T file;
    private final ResourceLocation resourceId;
    private final EventEntry<T> entry;
    private final HashMap<ResourceLocation, BuiltResourceReference<T>> references = new HashMap<>();
    private boolean markedForDeletion;
    private final Set<UUID> cancelledFutures = new HashSet<>();
    private final HashMap<ResourceLocation, T> identifiedCreatedResources = new HashMap<>();
    private final List<T> indexedCreatedResources = new ArrayList<>();
    private final HashMap<MixsonEventBuilder<T>, Integer> createdRuntimeEvents = new HashMap<>();
    private final HashMap<MixsonEventBuilder<T>, Integer> createdEvents = new HashMap<>();
    private final Mutable<T> debugExportObject;
    private final BiFunction<String, Integer, T> captureCallback;

    public EventContext(ContextCreationType creationType, T file, ResourceLocation resourceId, EventEntry<T> entry, boolean markedForDeletion, BuiltResourceReference<T>[] references, BiFunction<String, Integer, T> captureCallback) {
        this.creationType = creationType;
        this.file = file;
        this.debugExportObject = new MutableObject<>(this.file);
        this.resourceId = resourceId;
        this.entry = entry;
        this.markedForDeletion = markedForDeletion;
        this.captureCallback = captureCallback;
        for(BuiltResourceReference<T> ref : references) this.references.put(ref.getReferenceId(), ref);
    }

    public T getFile() {
        return this.file;
    }

    public ResourceLocation getResourceId() {
        return this.resourceId;
    }

    public String getEventName() {
        return getEvent().eventName();
    }

    public BuiltMixsonEvent<T> getEvent() {
        return entry.event();
    }

    public int getPriority() {
        return entry.priority();
    }

    public BuiltResourceReference<T> getReference(String id) {
        return this.references.get(ResourceLocationUtil.parse(id));
    }

    public void markForDeletion(boolean shouldDelete) {
        this.markedForDeletion = shouldDelete;
    }

    public void createResource(ResourceLocation id, T elem) {
        if(creationType != ContextCreationType.IDENTIFIED) throw new IllegalCallerException(String.format("cannot created identified resources for event '%s'", getEventName()));
        this.identifiedCreatedResources.put(id, elem);
    }

    public void createResource(T elem) {
        if(creationType != ContextCreationType.INDEXED) throw new IllegalCallerException(String.format("cannot created indexed resources for event '%s'", getEventName()));
        this.indexedCreatedResources.add(elem);
    }

    protected HashMap<ResourceLocation, T> getIdentifiedCreatedResources() {
        return this.identifiedCreatedResources;
    }

    protected List<T> getIndexedCreatedResources() {
        return indexedCreatedResources;
    }

    public void cancelFutureEvent(UUID uuid) {
        this.cancelledFutures.add(uuid);
    }

    /**
     * Use {@link #registerRuntimeEvent(int, ResourceLocator, String, MixsonEvent, boolean, ResourceReference...)}
     * to register runtime events.
     * **/
    @Deprecated
    public void registerRuntimeEvent(int priority, String resourceId, String eventName, MixsonEvent<T> event, boolean failSilently, ResourceReference... references) {
        registerRuntimeEvent(priority, MixsonUtil.getLocatorFromString(resourceId), eventName, event, failSilently, references);
    }

    /**
     * Use {@link #registerDualEvent(int, ResourceLocator, String, MixsonEvent, boolean, ResourceReference...)}
     * to register dual events.
     * **/
    @Deprecated
    public void registerDualEvent(int priority, String resourceId, String eventName, MixsonEvent<T> event, boolean failSilently, ResourceReference... references) {
        registerDualEvent(priority, MixsonUtil.getLocatorFromString(resourceId), eventName, event, failSilently, references);
    }

    public void registerDualEvent(int priority, ResourceLocator resourceLocator, String eventName, MixsonEvent<T> event, boolean failSilently, ResourceReference... references) {
        MixsonEventBuilder<T> eventBuilder = new MixsonEventBuilder<T>()
                .setCodec(this.getEvent().codec())
                .setResourceLocator(resourceLocator)
                .setEventName(eventName)
                .setEvent(event)
                .setSilentlyFail(failSilently)
                .setReferences(references);
        createdEvents.put(eventBuilder, priority);
    }

    public void registerRuntimeEvent(int priority, ResourceLocator resourceLocator, String eventName, MixsonEvent<T> event, boolean failSilently, boolean assertive, ResourceReference... references) {
        MixsonEventBuilder<T> eventBuilder = new MixsonEventBuilder<T>()
                .setCodec(this.getEvent().codec())
                .setResourceLocator(resourceLocator)
                .setEventName(eventName)
                .setEvent(event)
                .setSilentlyFail(failSilently)
                .setReferences(references)
                .setAssertive(assertive);
        createdRuntimeEvents.put(eventBuilder, priority);
    }

    public void registerRuntimeEvent(int priority, ResourceLocator resourceLocator, String eventName, MixsonEvent<T> event, boolean failSilently, ResourceReference... references) {
        registerRuntimeEvent(priority, resourceLocator, eventName, event, failSilently, false, references);
    }

    public void registerRuntimeEvent(int priority, MixsonEventBuilder<T> eventBuilder) {
        if(eventBuilder.hasDifferentCodec(this.getEvent().codec()))
            throw new MixsonError("attempted to register runtime event with different codec than the event");
        createdRuntimeEvents.put(eventBuilder, priority);
    }

    public void registerEvent(int priority, MixsonEventBuilder<T> eventBuilder) {
        if(eventBuilder.hasDifferentCodec(this.getEvent().codec()))
            throw new MixsonError("attempted to register event with different codec than the event");
        createdEvents.put(eventBuilder, priority);
    }

    public Optional<T> captureFile(String resourceId) {
        return captureFile(resourceId, 0);
    }
    public Optional<T> captureFile(String resourceId, int ordinal) {
        return Optional.ofNullable(captureCallback.apply(resourceId, ordinal));
    }

    public void setDebugExport(T result) {
        this.debugExportObject.setValue(result);
    }

    public void cancelDebugExport() {
        this.debugExportObject.setValue(null);
    }

    protected T getDebugExportObject() {
        return this.debugExportObject.getValue();
    }

    protected HashMap<MixsonEventBuilder<T>, Integer> getCreatedRuntimeEvents() {
        return createdRuntimeEvents;
    }

    protected HashMap<MixsonEventBuilder<T>, Integer> getCreatedEvents() {
        return createdEvents;
    }


    protected boolean isMarkedForDeletion() {
        return this.markedForDeletion;
    }

    protected Set<UUID> getCancelledFutures() {
        return this.cancelledFutures;
    }

    public ContextCreationType getCreationType() {
        return creationType;
    }
}
