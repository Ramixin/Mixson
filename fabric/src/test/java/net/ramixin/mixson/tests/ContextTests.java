package net.ramixin.mixson.tests;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.hooks.StandardHook;
import net.ramixin.mixson.util.Index;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.ramixin.mixson.tests.TestUtil.createDummyResource;
import static net.ramixin.mixson.tests.TestUtil.generateRandomJsonObject;

public class ContextTests {

    @Test
    public void testEventCancellation() {
        Index index = new Index("test:canceler");
        Identifier idWithExt = index.id().withSuffix(".json");

        Index otherIndex = new Index("test:canceled");
        Identifier otherIdWithExt = otherIndex.id().withSuffix(".json");

        AtomicBoolean cancelled = new AtomicBoolean(true);

        UUID eventId = Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY + 100,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "eventToBeCancelled",
                otherIndex::idEquals,
                _ -> cancelled.set(false)
        );

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "fileCancellationTest",
                index::idEquals,
                context -> context.cancelFutureEvent(eventId)
        );
        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(generateRandomJsonObject()));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
        assert cancelled.get() : "Event was not cancelled";
    }
}
