package net.ramixin.mixson.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.server.packs.resources.Resource;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public interface TestUtil {

    Random RANDOM = new Random();

    @SuppressWarnings("DataFlowIssue")
    static Resource createDummyResource(JsonElement element) {
        return new Resource(null, () -> new ByteArrayInputStream(element.toString().getBytes()), null);
    }

    static ArrayList<Resource> generateRandomResourceList(JsonObject object, MutableInt indexNet) {
        int size = RANDOM.nextInt(8) + 3;
        int loc = RANDOM.nextInt(size);
        indexNet.setValue(loc);
        ArrayList<Resource> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if(i == loc)
                list.add(createDummyResource(object));
            else
                list.add(createDummyResource(new JsonObject()));
        }
        indexNet.setValue(loc);
        return list;
    }

    static JsonObject generateRandomJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", UUID.randomUUID().toString());
        obj.addProperty("count", RANDOM.nextInt(1000));
        obj.addProperty("active", RANDOM.nextBoolean());
        obj.addProperty("score", RANDOM.nextDouble() * 100);

        JsonArray arr = new JsonArray();
        int size = RANDOM.nextInt(5) + 1;
        for (int i = 0; i < size; i++) {
            arr.add(new JsonPrimitive(UUID.randomUUID().toString().substring(0, 8)));
        }
        obj.add("tags", arr);

        return obj;
    }

    static int randomListOrdinal(int max, int... exclude) {
        outer: while(true) {
            int num = RANDOM.nextInt(max);
            for(int item : exclude)
                if(num == item) continue outer;
            return num;
        }
    }

}
