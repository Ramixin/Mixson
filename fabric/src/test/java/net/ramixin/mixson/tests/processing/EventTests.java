package net.ramixin.mixson.tests.processing;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.hooks.ListHook;
import net.ramixin.mixson.hooks.NamespaceHook;
import net.ramixin.mixson.hooks.StandardHook;
import net.ramixin.mixson.util.Index;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.ramixin.mixson.tests.TestUtil.*;

public class EventTests {

    @Test
    public void testStandardEventProcessing() {
        Index index = new Index("test:standard_run");
        Identifier indexExt = index.id().withSuffix(".json");

        Index badIndex = new Index("test:standard_not_run");

        MutableInt correctRan = new MutableInt();
        MutableInt wrongRan = new MutableInt();

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Standard - eventThatShouldRun",
                index::equals,
                _ -> correctRan.increment()
        );
        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Standard - eventThatShouldNotRun",
                badIndex::equals,
                _ -> wrongRan.increment()
        );
        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(indexExt, createDummyResource(generateRandomJsonObject()));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
        assert (int) correctRan.get() == 1 : "Event was not run 1 time, but " + correctRan.get() + " times";
        assert (int) wrongRan.get() == 0 : "Wrong event was ran " + wrongRan.get() + " time(s)";
    }

    @Test
    public void testListEventProcessing() {
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(new JsonObject(), net);

        Index index = new Index("test:list_run", (int) net.get());
        Identifier indexExt = index.id().withSuffix(".json");

        Index badIndex = new Index("test:list_not_run", (int) net.get());


        MutableInt correctRan = new MutableInt();
        MutableInt wrongRan = new MutableInt();

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "List - eventThatShouldRun",
                index::equals,
                _ -> correctRan.increment()
        );
        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "List - eventThatShouldNotRun",
                badIndex::equals,
                _ -> wrongRan.increment()
        );
        Map<Identifier, List<Resource>> resourceMap = new HashMap<>(){{
            put(indexExt, resourceList);
        }};
        Mixson.processHook(new ListHook(resourceMap));
        assert (int) correctRan.get() == 1 : "Event was not run 1 time, but " + correctRan.get() + " times";
        assert (int) wrongRan.get() == 0 : "Wrong event was run " + wrongRan.get() + " time(s)";
    }

    @Test
    public void testNamespaceEventProcessing() {
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(new JsonObject(), net);

        Index index = new Index("test:namespace_run", (int) net.get());
        Identifier indexExt = index.id().withSuffix(".json");

        Index badIndex = new Index("test:namespace_not_run", (int) net.get());

        MutableInt correctRan = new MutableInt();
        MutableInt wrongRan = new MutableInt();

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Namespace - eventThatShouldRun",
                index::equals,
                _ -> correctRan.increment()
        );
        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "eventThatShouldNotRun",
                badIndex::equals,
                _ -> wrongRan.increment()
        );
        Mixson.processHook(new NamespaceHook(resourceList, indexExt));
        assert (int) correctRan.get() == 1 : "Event was not run 1 time, but " + correctRan.get() + " times";
        assert (int) wrongRan.get() == 0 : "Wrong event was ran " + wrongRan.get() + " time(s)";
    }


}
