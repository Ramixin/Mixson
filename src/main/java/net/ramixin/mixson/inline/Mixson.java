package net.ramixin.mixson.inline;


import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.MixsonError;
import net.ramixin.mixson.debug.CallCountEntry;
import net.ramixin.mixson.debug.DebugMode;
import net.ramixin.mixson.debug.MixsonCommand;
import net.ramixin.mixson.inline.entries.AbstractEntry;
import net.ramixin.mixson.inline.entries.EventEntry;
import net.ramixin.mixson.inline.entries.ReferenceEntry;
import net.ramixin.mixson.util.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static net.ramixin.mixson.util.MixsonUtil.*;

@SuppressWarnings("unused")
public final class Mixson implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson");
    private static DebugMode debugMode = DebugMode.OFF;
    private static final Map<UUID, BuiltMixsonEvent<?>> events = Collections.synchronizedMap(new ConcurrentHashMap<>());
    private static final SortedMap<Integer, List<BuiltMixsonEvent<?>>> orderedEvents = Collections.synchronizedSortedMap(new TreeMap<>());
    private static final Map<UUID, CallCountEntry> callCounts = Collections.synchronizedMap(new HashMap<>());
    private static final Map<UUID, BuiltResourceReference<?>> references = Collections.synchronizedMap(new ConcurrentHashMap<>());
    private static final SortedMap<Integer, List<BuiltResourceReference<?>>> orderedReferences = Collections.synchronizedSortedMap(new TreeMap<>());
    public static final int DEFAULT_PRIORITY = 1000;

    /**
     * Use {@link MixsonCodecs#JSON_ELEMENT}
     * instead
     * **/
    @Deprecated(forRemoval = true)
    public static final MixsonCodec<JsonElement> JSON_ELEMENT_CODEC = MixsonCodecs.JSON_ELEMENT;

    // EVENT REGISTRATION METHODS

    /**
     * Use {@link #registerEvent(int, ResourceLocator, String, MixsonEvent, boolean, ResourceReference...)}
     * instead
     * **/
    @Deprecated(forRemoval = true)
    public static UUID registerEvent(int priority, String resourceId, String eventName, MixsonEvent<JsonElement> event, ResourceReference... references) {
        return registerEvent(priority, resourceId, eventName, event, false, references);
    }

    /**
     * Use {@link #registerEvent(int, ResourceLocator, String, MixsonEvent, boolean, ResourceReference...)}
     * instead
     * **/
    @Deprecated(forRemoval = true)
    public static UUID registerEvent(int priority, String resourceId, String eventName, MixsonEvent<JsonElement> event, boolean silentlyFail, ResourceReference... references) {
        return registerEvent(MixsonCodecs.JSON_ELEMENT, priority, resourceId, eventName, event, silentlyFail, references);
    }

    public static UUID registerEvent(int priority, ResourceLocator resourceLocator, String eventName, MixsonEvent<JsonElement> event, boolean silentlyFail, ResourceReference... references) {
        return registerEvent(MixsonCodecs.JSON_ELEMENT, priority, resourceLocator, eventName, event, silentlyFail, references);
    }

    /**
     * Use {@link #registerEvent(MixsonCodec, int, ResourceLocator, String, MixsonEvent, boolean, ResourceReference...)}
     * instead
     * **/
    @Deprecated(forRemoval = true)
    public static <T> UUID registerEvent(MixsonCodec<T> codec, int priority, String resourceId, String eventName, MixsonEvent<T> event, boolean silentlyFail, ResourceReference... references) {
        return registerEvent(codec, priority, getLocatorFromString(resourceId), eventName, event, silentlyFail, references);
    }

    public static <T> UUID registerEvent(MixsonCodec<T> codec, int priority, ResourceLocator resourceLocator, String eventName, MixsonEvent<T> event, boolean silentlyFail, ResourceReference... references) {
        BuiltMixsonEvent<T> builtEvent = new MixsonEventBuilder<T>()
                .setCodec(codec)
                .setResourceLocator(resourceLocator)
                .setEventName(eventName)
                .setEvent(event)
                .setSilentlyFail(silentlyFail)
                .setReferences(references)
                .build(priority, Mixson::finalizeReferenceRegistration);
        return finalizeEventRegistration(priority, builtEvent);
    }

    public static <T> UUID registerEvent(int priority, MixsonEventBuilder<T> builder) {
        BuiltMixsonEvent<T> builtEvent = builder.build(priority, Mixson::finalizeReferenceRegistration);
        if(builtEvent.assertive())
            throw new IllegalStateException("non-runtime events cannot be assertive");
        return finalizeEventRegistration(priority, builtEvent);
    }


    // SEPARATED REGISTRATION

    private static <T> UUID finalizeEventRegistration(int priority, BuiltMixsonEvent<T> builtEvent) {
        addComponent(builtEvent, priority, builtEvent.uuid(), events, orderedEvents);
        return builtEvent.uuid();
    }

    private static <T> void finalizeReferenceRegistration(int priority, BuiltResourceReference<T> builtReference) {
        addComponent(builtReference, priority, builtReference.getUuid(), references, orderedReferences);
    }

    // INIT

    @Override
    public void onInitialize() {
        loadATPMixsonEntries("mixson");

        if(!FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        if(!FabricLoader.getInstance().isModLoaded("fabric")) return;
        MixsonCommand.onInitialize();
    }

    // EXTERNAL RUN METHODS

    public static Map<ResourceLocation, Resource> runStandardEvents(Map<ResourceLocation, Resource> original) {
        MixsonRuntime runtime = new MixsonRuntime(orderedEvents, orderedReferences);
        final Set<ResourceLocation> markedForDeletion = new HashSet<>();
        final Set<UUID> filledReferences = new HashSet<>();
        while(runtime.hasFinished()) {
            AbstractEntry entry = runtime.pop();
            switch (entry) {
                case ReferenceEntry<?> referenceEntry -> {
                    int ordinal = entry.getOrdinal();
                    BuiltResourceReference<?> ref = referenceEntry.reference();
                    ResourceLocation resourceId = ref.getResourceId().withSuffix(ref.getCodec().extensionAndDot());
                    if(!original.containsKey(resourceId)) continue;
                    if(ordinal >= 1) ordinalError(ordinal, 0, ref, resourceId);
                    fulfillReference(original.get(resourceId), ref, filledReferences);
                }

                case EventEntry<?> eventEntry -> beginEventProcessing(Mixson::processStandardEvent, original, eventEntry, runtime, markedForDeletion);

                default -> throw new IllegalStateException("Unexpected value: " + entry);
            }

        }
        filledReferences.forEach(uuid-> {
            if(references.containsKey(uuid))
                references.get(uuid).clear();
        });
        for(ResourceLocation id : markedForDeletion) original.remove(id);
        return original;
    }

    public static Map<ResourceLocation, List<Resource>> runListEvents(Map<ResourceLocation, List<Resource>> original) {
        MixsonRuntime runtime = new MixsonRuntime(orderedEvents, orderedReferences);
        final Set<Pair<ResourceLocation, Integer>> markedForDeletion = new HashSet<>();
        final Set<UUID> filledReferences = new HashSet<>();
        while(runtime.hasFinished()) {
            AbstractEntry entry = runtime.pop();
            switch (entry) {
                case ReferenceEntry<?> referenceEntry -> {
                    int ordinal = entry.getOrdinal();
                    BuiltResourceReference<?> ref = referenceEntry.reference();
                    ResourceLocation resourceId = ref.getResourceId().withSuffix(ref.getCodec().extensionAndDot());
                    if(!original.containsKey(resourceId)) continue;
                    List<Resource> resources = original.get(resourceId);
                    if(ordinal >= resources.size()) ordinalError(ordinal, resources.size()-1, ref, resourceId);
                    Resource resource = resources.get(ordinal);
                    fulfillReference(resource, ref, filledReferences);
                }

                case EventEntry<?> eventEntry -> beginEventProcessing(Mixson::prepareListEventProcessing, original, eventEntry, runtime, markedForDeletion);

                default -> throw new IllegalStateException("Unexpected value: " + entry);
            }
        }
        filledReferences.forEach(uuid-> {
            if(references.containsKey(uuid))
                references.get(uuid).clear();
        });

        for(Pair<ResourceLocation, Integer> pairId : markedForDeletion) {
            List<Resource> resources = original.get(pairId.getFirst());
            resources.set(pairId.getSecond(), null);
        }
        for(ResourceLocation resourceId : original.keySet()) {
            List<Resource> resources = original.get(resourceId);
            resources.removeIf(Objects::isNull);
        }
        return original;
    }

    public static List<Resource> runNamespaceEvents(List<Resource> original, ResourceLocation id) {
        MixsonRuntime runtime = new MixsonRuntime(orderedEvents, orderedReferences);
        final Set<Integer> markedForDeletion = new HashSet<>();
        final Set<UUID> filledReferences = new HashSet<>();
        while(runtime.hasFinished()) {
            AbstractEntry entry = runtime.pop();
            switch(entry) {
                case ReferenceEntry<?> referenceEntry -> {
                    int ordinal = entry.getOrdinal();
                    BuiltResourceReference<?> ref = referenceEntry.reference();
                    if(!ref.getResourceId().withSuffix(ref.getCodec().extensionAndDot()).equals(id)) continue;
                    if(ordinal >= original.size()) ordinalError(ordinal, original.size()-1, ref, ref.getResourceId());
                    Resource resource = original.get(ordinal);
                    fulfillReference(resource, ref, filledReferences);
                }

                case EventEntry<?> eventEntry -> {
                    BuiltMixsonEvent<?> event = eventEntry.event();
                    int fileOperations = 0;
                    if(!event.isApplicable(id)) continue;
                    logVerboseAction("begun processing event '{}'", entry.getName());
                    ReadableTimer timer = new ReadableTimer();
                    int ordinal = eventEntry.getOrdinal();
                    if(ordinal >= original.size()) ordinalError(ordinal, original.size()-1, event, id);
                    if(ordinal == -1) {
                        int toIter = original.size();
                        for (int i = 0; i < toIter; i++) {
                            processNamespaceEvent(original, runtime, markedForDeletion, eventEntry, id, i);
                            fileOperations++;
                        }
                    }else {
                        processNamespaceEvent(original, runtime, markedForDeletion, eventEntry, id, ordinal);
                        fileOperations++;
                    }
                    incrementCallCounts(event, fileOperations);
                    logVerboseAction("successfully finished processing event '{}' in {}", entry.getName(), timer.timestamp());
                }

                default -> throw new IllegalStateException("Unexpected value: " + entry);
            }
        }
        filledReferences.forEach(uuid-> {
            if(references.containsKey(uuid))
                references.get(uuid).clear();
        });

        markedForDeletion.stream().sorted(Comparator.reverseOrder()).forEach(clazzInt -> original.remove((int) clazzInt));
        return original;
    }

    // INTERNAL RUN METHODS

    private static <T, M, K> void beginEventProcessing(QuintFunction<Map<ResourceLocation, K>, MixsonRuntime, Set<T>, EventEntry<M>, ResourceLocation, Boolean> processor, Map<ResourceLocation, K> original, EventEntry<M> entry, MixsonRuntime runtime, Set<T> markedForDeletion) {
        BuiltMixsonEvent<M> event = entry.event();
        int fileOperations = 0;
        List<ResourceLocation> keys = original.keySet().stream().filter(event::isApplicable).sorted(ResourceLocation::compareTo).toList();
        if(keys.isEmpty() && entry.event().assertive()) throw new MixsonError("assertion on event '%s' failed", entry.getName());
        if(keys.isEmpty()) return;
        logEventProcessingStart(entry.getName());
        ReadableTimer timer = new ReadableTimer();
        for(ResourceLocation resourceId : keys) {
            fileOperations++;
            if(processor.accept(original, runtime, markedForDeletion, entry, resourceId)) {
                logAction("event '{}' cancelled further processing", event.eventName());
                break;
            }
        }
        if(fileOperations != 0) incrementCallCounts(event, fileOperations);
        logVerboseAction("successfully finished processing event '{}' in {}", entry.getName(), timer.timestamp());
    }

    private static <T> void processNamespaceEvent(List<Resource> original, MixsonRuntime runtime, Set<Integer> markedForDeletion, EventEntry<T> eventEntry, ResourceLocation resourceId, int i) {
        BuiltMixsonEvent<T> event = eventEntry.event();
        Resource resource = original.get(i);
        Optional<T> file = getFile(event.codec(), resource, event, resourceId, Mixson::runtimeError);
        if (file.isEmpty()) return;
        try {
            EventContext<T> context = processContext(runtime, markedForDeletion, eventEntry, event, resourceId, i, file.get(), (stringId, ordinal) -> {
                if(!stringId.equals(resourceId.toString())) runtimeError(new IllegalStateException(String.format("cannot capture resource with id '%s' if event id is '%s'", stringId, resourceId)), event, resourceId);
                if(ordinal < 0 || ordinal > original.size() - 1) ordinalError(ordinal, original.size()-1, eventEntry.event(), resourceId);
                ResourceLocation id = ResourceLocationUtil.parse(stringId);
                Resource refResource = original.get(ordinal);
                if(refResource == null) return null;
                try {
                    return eventEntry.event().codec().deserialize(refResource);
                } catch (IOException e) {
                    runtimeError(e, event, id);

                }
                return null;
            });
            for(T createdEntry : context.getIndexedCreatedResources())
                original.add(event.codec().serialize(resource, createdEntry));
            original.set(i, event.codec().serialize(resource, context.getFile()));
        } catch (Exception e) {
            runtimeError(e, event, resourceId);
        }
    }

    private static <T> boolean prepareListEventProcessing(Map<ResourceLocation, List<Resource>> original, MixsonRuntime runtime, Set<Pair<ResourceLocation, Integer>> markedForDeletion, EventEntry<T> eventEntry, ResourceLocation resourceId) {
        BuiltMixsonEvent<T> event = eventEntry.event();
        List<Resource> resources = original.get(resourceId);
        int ordinal = eventEntry.getOrdinal();
        if(ordinal >= resources.size()) ordinalError(ordinal, resources.size()-1, event, resourceId);
        if(ordinal == -1) {
            for (int i = 0; i < resources.size(); i++)
                if (processListEvent(original, resourceId, resources, i, event, runtime, eventEntry, markedForDeletion))
                    return true;
        } else
            return processListEvent(original, resourceId, resources, ordinal, event, runtime, eventEntry, markedForDeletion);
        return false;
    }

    private static <T> boolean processListEvent(Map<ResourceLocation, List<Resource>> original, ResourceLocation resourceId, List<Resource> resources, int ordinal, BuiltMixsonEvent<T> event, MixsonRuntime runtime, EventEntry<T> eventEntry, Set<Pair<ResourceLocation, Integer>> markedForDeletion) {
        Resource resource = resources.get(ordinal);
        Pair<ResourceLocation, Integer> pairedId = Pair.of(resourceId, ordinal);
        Optional<T> file = getFile(event.codec(), resource, event, resourceId, Mixson::runtimeError);
        if (file.isEmpty()) return false;
        try {
            EventContext<T> context = processContext(runtime, markedForDeletion, eventEntry, event, resourceId, pairedId, file.get(), (stringId, captureOrdinal) -> {
                if(captureOrdinal < 0 || captureOrdinal > original.size()-1) ordinalError(captureOrdinal, original.size()-1, eventEntry.event(), resourceId);
                ResourceLocation id = ResourceLocationUtil.parse(stringId);
                List<Resource> resourceList = original.get(id.withSuffix(event.codec().extensionAndDot()));
                if(resourceList == null) return null;
                Resource refResource = resourceList.get(captureOrdinal);
                if(refResource == null) return null;
                try {
                    return eventEntry.event().codec().deserialize(refResource);
                } catch (IOException e) {
                    runtimeError(e, event, id);

                }
                return null;
            });

            for(Map.Entry<ResourceLocation, T> createdEntry : context.getIdentifiedCreatedResources().entrySet()) {
                ResourceLocation createdId = createdEntry.getKey();
                if(!createdId.getPath().endsWith(event.codec().extensionAndDot()))
                    logWarning("created resource '{}' does not end with its codec's extension '{}'", createdId, event.codec().extensionAndDot());
                List<Resource> createdResources = original.computeIfAbsent(createdId, (unused) -> new ArrayList<>());
                createdResources.add(event.codec().serialize(resource, createdEntry.getValue()));
            }
            resources.set(ordinal, event.codec().serialize(resource, context.getFile()));
            return context.getCancelledFutures().contains(event.uuid());
        } catch (Exception e) {
            runtimeError(e, event, resourceId);
        }
        return false;
    }


    private static <T> boolean processStandardEvent(Map<ResourceLocation, Resource> original, MixsonRuntime runtime, Set<ResourceLocation> markedForDeletion, EventEntry<T> eventEntry, ResourceLocation resourceId) {
        BuiltMixsonEvent<T> event = eventEntry.event();
        Resource resource = original.get(resourceId);
        Optional<T> file = getFile(event.codec(), resource, event, resourceId, Mixson::runtimeError);
        if (file.isEmpty()) return false;
        try {
            EventContext<T> context = processContext(runtime, markedForDeletion, eventEntry, event, resourceId, resourceId, file.get(), (stringId, ordinal) -> {
                if(ordinal > 0) ordinalError(ordinal, 0, eventEntry.event(), resourceId);
                ResourceLocation id = ResourceLocationUtil.parse(stringId);
                Resource refResource = original.get(id.withSuffix(event.codec().extensionAndDot()));
                if(refResource == null) return null;
                try {
                    return eventEntry.event().codec().deserialize(refResource);
                } catch (IOException e) {
                    runtimeError(e, event, id);

                }
                return null;
            });
            for(Map.Entry<ResourceLocation, T> createdEntry : context.getIdentifiedCreatedResources().entrySet()) {
                ResourceLocation createdId = createdEntry.getKey();
                if(!createdId.getPath().endsWith(event.codec().extensionAndDot()))
                    logWarning("created resource '{}' does not end with its codec's extension '{}'", createdId, event.codec().extensionAndDot());
                original.put(createdId, event.codec().serialize(resource, createdEntry.getValue()));
            }
            original.put(resourceId, event.codec().serialize(resource, context.getFile()));
            return context.getCancelledFutures().contains(event.uuid());
        } catch (Exception e) {
            runtimeError(e, event, resourceId);
        }
        return false;
    }

    private static <N, T> @NotNull EventContext<T> processContext(MixsonRuntime runtime, Set<N> markedForDeletion, EventEntry<T> eventEntry, BuiltMixsonEvent<T> event, ResourceLocation resourceId, N indexer, T file, BiFunction<String, Integer, T> captureCallback) {
        EventContext<T> context = createContext(ContextCreationType.IDENTIFIED, resourceId, file, eventEntry, markedForDeletion.contains(indexer), uuid -> runtime.getReference(uuid, references::get), captureCallback);
        logEventRun(event, resourceId);
        ReadableTimer timer = new ReadableTimer();
        event.event().runEvent(context);
        logEventExit(event, resourceId, timer);
        T debugExport = context.getDebugExportObject();
        if(debugExport != null)
            exportDebugFile(event.codec()::serializeOutputFile, debugExport, event.eventName(), resourceId.toString(), event.codec().extensionAndDot());
        if(context.isMarkedForDeletion()) markedForDeletion.add(indexer);
        else markedForDeletion.remove(indexer);
        context.getCancelledFutures().forEach(runtime::cancelEvent);
        List<AbstractEntry> appendable = new ArrayList<>();
        for(Map.Entry<MixsonEventBuilder<T>, Integer> createdEvent : context.getCreatedEvents().entrySet()) {
            BuiltMixsonEvent<T> builtEvent = createdEvent.getKey().build(createdEvent.getValue(), (priority, reference) -> {
                appendable.add(new ReferenceEntry<>(priority, reference));
                finalizeReferenceRegistration(priority, reference);
            });
            appendable.add(new EventEntry<>(createdEvent.getValue(), builtEvent));
            finalizeEventRegistration(createdEvent.getValue(), builtEvent);
        }
        for(Map.Entry<MixsonEventBuilder<T>, Integer> createdEvent : context.getCreatedRuntimeEvents().entrySet()) {
            BuiltMixsonEvent<T> builtEvent = createdEvent.getKey().build(createdEvent.getValue(), (priority, reference) -> appendable.add(new ReferenceEntry<>(priority, reference)));
            appendable.add(new EventEntry<>(createdEvent.getValue(), builtEvent));
        }
        appendable.forEach(runtime::insertEntry);
        return context;
    }

    private static <T> void fulfillReference(Resource resource, BuiltResourceReference<T> ref, Set<UUID> filledReferences) {
        try {
            T file = ref.getCodec().deserialize(resource);
            ref.fulfill(file);
            filledReferences.add(ref.getUuid());
        } catch (IOException e) {
            runtimeError(e, ref, ref.getResourceId());
        }
    }

    // ERRORS

    static void registrationError(Exception e, ErrorMessageProvider errorMessageProvider) {
        if(errorMessageProvider.failSilently()) LOGGER.error(errorMessageProvider.getRegistrationMessage(), e);
        else throw new MixsonError(errorMessageProvider.getRegistrationMessage()+e);
    }

    private static void runtimeError(Exception e, ErrorMessageProvider errorMessageProvider, ResourceLocation resourceId) {
        if(errorMessageProvider.failSilently()) LOGGER.error(errorMessageProvider.getRuntimeMessage(resourceId), e);
        else throw new MixsonError(errorMessageProvider.getRuntimeMessage(resourceId)+e);
    }

    private static void ordinalError(int ordinal, int maxOrdinal, ErrorMessageProvider errorMessageProvider, ResourceLocation resourceId) {
        runtimeError(new MixsonError("ordinal value '"+ordinal+"' points to no value. Max Ordinal Value: "+maxOrdinal), errorMessageProvider, resourceId);
    }

    // MISC. PUBLICS

    public static boolean removeEvent(UUID uuid) {
        for(List<BuiltMixsonEvent<?>> eventSet : orderedEvents.values()) eventSet.removeIf(event -> event.uuid().equals(uuid));
        return events.remove(uuid) != null;
    }

    public static String getEventName(UUID uuid) {
        return events.get(uuid).eventName();
    }

    // DEBUGGING STUFF

    public static void setDebugMode(DebugMode debugMode) {
        Mixson.debugMode = debugMode;
        LOGGER.info("Mixson Debug Mode has been set to: {}", debugMode);
    }

    private static void logEventRun(BuiltMixsonEvent<?> event, ResourceLocation resourceId) {
        logAction("Running '{}' on resource '{}'", event.eventName(), resourceId);
    }

    private static void logEventExit(BuiltMixsonEvent<?> event, ResourceLocation resourceId, ReadableTimer timer) {
        logVerboseAction("Finished running '{}' on resource '{}' in {}", event.eventName(), resourceId, timer.timestamp());
    }

    static void logEventRegistration(String eventName, int priority) {
        logAction("Registering '{}' with priority {}", eventName, priority);
    }

    private static void logEventProcessingStart(String eventName) {
        logVerboseAction("begun processing event '{}'", eventName);
    }

    private static void logEventProcessingEnd(String eventName) {
        logVerboseAction("begun processing event '{}'", eventName);
    }

    private static void logAction(String action, Object... args) {
        if(debugMode.ordinal() > 0) LOGGER.info(action, args);
    }
    private static void logWarning(String warning, Object... args) {
        if(debugMode.ordinal() > 0) LOGGER.warn(warning, args);
    }

    private static void logVerboseAction(String action, Object... args) {
        if(debugMode.ordinal() >= DebugMode.VERBOSE.ordinal()) LOGGER.info(action, args);
    }

    private static void incrementCallCounts(BuiltMixsonEvent<?> event, int fileOperations) {
        CallCountEntry pair = callCounts.getOrDefault(event.uuid(), CallCountEntry.DEFAULT);
        callCounts.put(event.uuid(), pair.update(fileOperations));
    }

    private static <T> void exportDebugFile(ResourceExporter<T> resourceExporter, T resource, String eventName, String resourceId, String extension) {
        if(debugMode.ordinal() <= 1) return;
        Path dir = FabricLoader.getInstance().getGameDir().resolve(".mixson").resolve(identifierToPathString(resourceId, extension));
        try {
            Files.createDirectories(dir);
            FileOutputStream fos = new FileOutputStream(dir.resolve(stringToUsablePath(eventName)+extension).toFile());
            fos.write(resourceExporter.export(resource).toByteArray());
            fos.close();
        } catch (IOException e) {
            LOGGER.error("failed to export debug file", e);
        }

    }

    public static void clearCalls() {
        callCounts.clear();
    }

    public static List<UUID> getCallCountsOrder() {
        return callCounts
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static CallCountEntry getCallCount(UUID uuid) {
        return callCounts.getOrDefault(uuid, CallCountEntry.DEFAULT);
    }

    static {

        try {
            FileUtils.deleteDirectory(FabricLoader.getInstance().getGameDir().resolve(".mixson").toFile());
        } catch (IOException e) {
            LOGGER.error("failed to delete .mixson debug directory", e);
        }

    }

}
