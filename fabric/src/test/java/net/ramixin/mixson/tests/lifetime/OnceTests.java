package net.ramixin.mixson.tests.lifetime;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.hooks.StandardHook;
import net.ramixin.mixson.util.Index;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static net.ramixin.mixson.tests.TestUtil.createDummyResource;
import static net.ramixin.mixson.tests.TestUtil.generateRandomJsonObject;

public class OnceTests {

    @Test
    public void testStandardOnceLifetime() {
        Index index = new Index("test:standard_once");
        Identifier indexExt = index.id().withSuffix(".json");

        MutableInt runCount = new MutableInt();

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.ONCE,
                ErrorPolicy.THROW,
                "onceLifetimeEvent",
                index::equals,
                _ -> runCount.increment()
        );

        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(indexExt, createDummyResource(generateRandomJsonObject()));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
        Mixson.processHook(new StandardHook(resourceMap));

        assert (int) runCount.get() == 1 : "Event ran " + runCount.get() + " times";
    }

}
