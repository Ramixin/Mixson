package net.ramixin.mixson.inline;

import net.ramixin.mixson.inline.entries.AbstractEntry;
import net.ramixin.mixson.inline.entries.EventEntry;
import net.ramixin.mixson.inline.entries.ReferenceEntry;

import java.util.*;
import java.util.function.Function;

public class MixsonRuntime {


    private final List<AbstractEntry> queuedEvents = new ArrayList<>();
    private final HashMap<UUID, BuiltResourceReference<?>> runtimeReferences = new HashMap<>();


    protected MixsonRuntime(SortedMap<Integer, List<BuiltMixsonEvent<?>>> events, SortedMap<Integer, List<BuiltResourceReference<?>>> references) {
        TreeMap<Integer, List<AbstractEntry>> combinedEntries = new TreeMap<>();
        for(int priority : references.keySet()) {
            List<BuiltResourceReference<?>> builtReference = references.get(priority);
            List<AbstractEntry> entries = combinedEntries.computeIfAbsent(priority, k -> new ArrayList<>());
            builtReference.stream().map((reference) -> new ReferenceEntry<>(priority, reference)).forEach(entries::add);
        }
        for(int priority : events.keySet()) {
            List<BuiltMixsonEvent<?>> builtEvents = events.get(priority);
            List<AbstractEntry> entries = combinedEntries.computeIfAbsent(priority, k -> new ArrayList<>());
            builtEvents.stream().map((event) -> new EventEntry<>(priority, event)).forEach(entries::add);
        }
        combinedEntries.values().forEach(queuedEvents::addAll);
    }

    protected AbstractEntry pop() {
        return queuedEvents.remove(queuedEvents.size()-1);
    }

    protected boolean hasFinished() {
        return !queuedEvents.isEmpty();
    }

    protected void insertEntry(AbstractEntry entry) {
        if(entry instanceof ReferenceEntry<?> referenceEntry) runtimeReferences.put(referenceEntry.reference().getUuid(), referenceEntry.reference());
        int priority = entry.priority();
        for (int i = 0; i < queuedEvents.size(); i++) {
            AbstractEntry abstractEntry = queuedEvents.get(i);
            if(abstractEntry.priority() <= priority) continue;
            queuedEvents.add(i, entry);
            return;
        }
        queuedEvents.add(entry);
    }

    protected void cancelEvent(UUID uuid) {
        for (int i = 0; i < queuedEvents.size(); i++) {
            AbstractEntry abstractEntry = queuedEvents.get(i);
            if(abstractEntry instanceof EventEntry<?> eventEntry) if(eventEntry.event().uuid().equals(uuid)) {
                queuedEvents.remove(i);
                return;
            }
        }
    }

    protected BuiltResourceReference<?> getReference(UUID uuid, Function<UUID, BuiltResourceReference<?>> globalCallback) {
        BuiltResourceReference<?> reference;
        if(runtimeReferences.containsKey(uuid)) reference = runtimeReferences.get(uuid);
        else reference = globalCallback.apply(uuid);
        return reference;
    }

}
