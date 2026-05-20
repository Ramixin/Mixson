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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.ramixin.mixson.tests.TestUtil.createDummyResource;
import static net.ramixin.mixson.tests.TestUtil.generateRandomJsonObject;

public class LockTests {

    @Test
    public void testMixsonLock() throws InterruptedException {
        Index index = new Index("test:locked");
        Identifier idWithExt = index.id().withSuffix(".json");
        AtomicBoolean eventRan = new AtomicBoolean(false);

        Runnable releaseLock = Mixson.lockEventProcessing();

        Mixson.registerEvent(
              Mixson.DEFAULT_PRIORITY,
              Lifetime.PERSISTENT,
              ErrorPolicy.THROW,
              "testMixsonLockingEvent",
              _ -> true,
              _ -> eventRan.set(true)
        );
        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
        }};

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            Mixson.processHook(new StandardHook(resourceMap));
        });

        Thread.sleep(100);

        assert !eventRan.get() : "Event was run";
        assert !future.isDone() : "Future ran while lock was held";
        releaseLock.run();
        Thread.sleep(100);

        assert eventRan.get() : "Event was not run";
        assert future.isDone() : "Future did not run after lock was released";
    }

}
