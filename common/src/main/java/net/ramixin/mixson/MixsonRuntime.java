package net.ramixin.mixson;

import net.minecraft.resources.Identifier;
import net.ramixin.mixson.entries.AbstractEntry;
import net.ramixin.mixson.entries.EventEntry;
import net.ramixin.mixson.entries.ReferenceEntry;
import net.ramixin.mixson.hooks.AbstractHook;
import net.ramixin.mixson.util.interfaces.ErrorMessageProvider;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.BiConsumer;

@ApiStatus.Internal
public class MixsonRuntime<T> {

    private final AbstractHook<T> hook;
    private final BiConsumer<String, Exception> errorCallback;
    private final List<AbstractEntry> queuedEvents = new ArrayList<>();

    protected MixsonRuntime(AbstractHook<T> hook, MixsonRegistry<MixsonEvent<?>> eventRegistry, MixsonRegistry<ResourceReference<?>> referenceRegistry, BiConsumer<String, Exception> logCallback) {
        this.hook = hook;
        this.errorCallback = logCallback;
        TreeMap<Integer, List<AbstractEntry>> combinedEntries = new TreeMap<>();
        SortedMap<Integer, List<ResourceReference<?>>> references = referenceRegistry.captureSnapshot();
        for(int priority : references.keySet()) {
            List<ResourceReference<?>> builtReference = references.get(priority);
            List<AbstractEntry> entries = combinedEntries.computeIfAbsent(priority, _ -> new ArrayList<>());
            builtReference.stream().map((reference) -> new ReferenceEntry<>(priority, reference)).forEach(entries::add);
        }
        SortedMap<Integer, List<MixsonEvent<?>>> events = eventRegistry.captureSnapshot();
        for(int priority : events.keySet()) {
            List<MixsonEvent<?>> builtEvents = events.get(priority);
            List<AbstractEntry> entries = combinedEntries.computeIfAbsent(priority, _ -> new ArrayList<>());
            builtEvents.stream().map((event) -> new EventEntry<>(priority, event)).forEach(entries::add);
        }
        combinedEntries.sequencedValues().forEach(queuedEvents::addAll);
    }

    public AbstractHook<T> getHook() {
        return hook;
    }

    protected AbstractEntry pop() {
        return queuedEvents.removeFirst();
    }

    protected boolean isRunning() {
        return !queuedEvents.isEmpty();
    }

    protected void insertEntry(AbstractEntry entry) {
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

    protected void error(Exception e, ErrorMessageProvider errorProvider, Identifier resourceId) {
        switch(errorProvider.getErrorPolicy()) {
            case LOG -> errorCallback.accept(errorProvider.getRuntimeErrorMessage(resourceId), e);
            case THROW -> throw new MixsonException(errorProvider.getRuntimeErrorMessage(resourceId), e);
        }
    }
}
