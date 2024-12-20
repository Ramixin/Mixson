package net.ramixin.mixson;


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.*;

public class Mixson {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson");
    private static final TreeMap<Integer, Set<AssociatedMixsonEvent>> events = new TreeMap<>();
    public static final int DEFAULT_PRIORITY = 1000;

    public static void registerModificationEvent(Identifier resourceId, Identifier eventId, final MixsonEvent event) {
        registerModificationEvent(DEFAULT_PRIORITY, resourceId, eventId, event);
    }

    public static void registerModificationEvent(int priority, Identifier resourceId, Identifier eventId, final MixsonEvent event) {
        registerModificationEvent(priority, resourceId, eventId, event, false);
    }

    public static void registerModificationEvent(int priority, Identifier resourceId, Identifier eventId, final MixsonEvent event, boolean silentlyFail) {
        Set<AssociatedMixsonEvent> eventSet;
        if(events.get(priority) == null) eventSet = new HashSet<>();
        else eventSet = events.get(priority);
        eventSet.add(new AssociatedMixsonEvent(resourceId.withSuffixedPath(".json"), eventId, event, silentlyFail));
        events.put(priority, eventSet);
    }

    private record AssociatedMixsonEvent(Identifier resourceId, Identifier eventId, MixsonEvent event, boolean silentlyFail) {}

    public static Map<Identifier, Resource> runEvents(Map<Identifier, Resource> original) {
        HashMap<Identifier, JsonElement> modifiedEntries = new HashMap<>();
        for (Set<AssociatedMixsonEvent> eventSet : Mixson.events.sequencedValues()) for (AssociatedMixsonEvent event : eventSet) {
            if (!original.containsKey(event.resourceId())) continue;
            try {
                JsonElement elem = modifiedEntries.getOrDefault(event.resourceId(), JsonParser.parseReader(original.get(event.resourceId()).getReader()));
                modifiedEntries.put(event.resourceId(), event.event().run(elem));
            } catch (Exception e) {
                String errorString = String.format("Failed to modify json file '%s' with event '%s'\n", event.resourceId(), event.eventId());
                if(event.silentlyFail()) LOGGER.error(errorString, e);
                else throw new MixsonError(errorString+e);
            }
        }
        for (Map.Entry<Identifier, JsonElement> modifiedEntry : modifiedEntries.entrySet()) {
            Resource resource = original.get(modifiedEntry.getKey());
            if(resource == null) throw new IllegalStateException("resource was removed before modifications could be applied");
            original.put(modifiedEntry.getKey(), new Resource(resource.getPack(), () -> new ByteArrayInputStream(modifiedEntry.getValue().toString().getBytes()), resource::getMetadata));
        }
        return original;
    }

    @FunctionalInterface
    public interface MixsonEvent {

        JsonElement run(JsonElement elem);

    }
}
