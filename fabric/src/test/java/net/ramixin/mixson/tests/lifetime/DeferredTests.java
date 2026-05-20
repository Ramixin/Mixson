package net.ramixin.mixson.tests.lifetime;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.hooks.StandardHook;
import net.ramixin.mixson.util.Index;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.ramixin.mixson.tests.TestUtil.createDummyResource;
import static net.ramixin.mixson.tests.TestUtil.generateRandomJsonObject;

public class DeferredTests {

    private static final Logger log = LoggerFactory.getLogger(DeferredTests.class);

    @Test
    public void testEventPulling() {
        Index index = new Index("test:pulling");
        Identifier idWithExt = index.id().withSuffix(".json");

        Index otherIndex = new Index("test:pulled");
        Identifier otherIdWithExt = otherIndex.id().withSuffix(".json");

        AtomicBoolean pulled = new AtomicBoolean(false);

        UUID eventId = Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.DEFERRED,
                ErrorPolicy.THROW,
                "eventToBePulled",
                otherIndex::idEquals,
                _ -> pulled.set(true)
        );
        log.info("Registered event with id {}", eventId);

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "eventPullTest",
                index::idEquals,
                context -> {
                    log.info("pulling event with id {}", eventId);
                    context.pullIntoRuntime(eventId);
                }
        );

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY + 100,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "eventPullErrorTest",
                index::idEquals,
                context -> context.pullIntoRuntime(eventId)
        );

        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(generateRandomJsonObject()));
        }};
        Exception e = Assertions.assertThrows(IllegalArgumentException.class, () -> Mixson.processHook(new StandardHook(resourceMap)));
        log.error("Caught exception", e);
        assert e.getMessage().contains("failed to locate event or reference with uuid of ") : "Caught wrong exception";
        assert pulled.get() : "Event was not pulled";
    }

    @Test
    public void testConcurrentPulling() {
        Index index = new Index("test:pulling");
        Identifier idWithExt = index.id().withSuffix(".json");

        Index otherIndex = new Index("test:pulled");
        Identifier otherIdWithExt = otherIndex.id().withSuffix(".json");

        AtomicBoolean pulled = new AtomicBoolean(false);

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "eventPullTest",
                index::idEquals,
                context -> {

                    UUID eventId = Mixson.registerEvent(
                            Mixson.DEFAULT_PRIORITY,
                            Lifetime.DEFERRED,
                            ErrorPolicy.THROW,
                            "eventToBePulled",
                            otherIndex::idEquals,
                            _ -> pulled.set(true)
                    );

                    log.info("pulling concurrent event with id {}", eventId);
                    context.pullIntoRuntime(eventId);
                }
        );

        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(generateRandomJsonObject()));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
        assert pulled.get() : "Event was not pulled";
    }
}
