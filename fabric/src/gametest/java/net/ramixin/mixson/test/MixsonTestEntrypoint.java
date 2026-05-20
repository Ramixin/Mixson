package net.ramixin.mixson.test;

import net.fabricmc.loader.api.FabricLoader;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.enums.DebugOption;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.util.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MixsonTestEntrypoint {

    private static final List<String> MATCH_LIST = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MixsonTestEntrypoint.class);

    private static final String[] EXPECTED_MATCHES = {
            "advancement/",
            "banner_pattern/",
            "cat_sound_variant/",
            "cat_variant/",
            "chat_type/",
            "chicken_sound_variant/",
            "chicken_variant/",
            "cow_sound_variant/",
            "cow_variant/",
            "damage_type/",
            "dialog/",
            "dimension_type/",
            "enchantment/",
            "enchantment_provider/",
            "frog_variant/",
            "instrument/",
            "jukebox_song",
            "loot_table/",
            "painting_variant/",
            "pig_sound_variant/",
            "pig_variant/",
            "recipe/",
//            "structure/",
            "tags/",
            "timeline/",
            "trade_set/",
            "trial_spawner/",
            "trim_material/",
            "trim_pattern/",
            "villager_trade/",
            "wolf_sound_variant/",
            "wolf_variant/",
            "world_clock",
            "worldgen/",
            "zombie_nautilus_variant/"
    };

    public static void onInitialize() {
        Mixson.enableDebugOption(DebugOption.BASIC_LOGGING);
        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "CheckAllResourceMatches",
                MixsonTestEntrypoint::addToMatchList,
                _ -> {}
        );
    }

    @SuppressWarnings("SameReturnValue")
    private static boolean addToMatchList(Index id) {
        String idString = id.id().toString();
        String dirsOnly = idString.substring(0, idString.lastIndexOf('/')+1);
        if(MATCH_LIST.contains(dirsOnly)) return false;
        MATCH_LIST.add(dirsOnly);
        return false;
    }

    static boolean assertMatches() throws IOException {
        Path path = FabricLoader.getInstance().getGameDir().resolve("match_list.txt");
        System.out.println(path);
        FileWriter file = new FileWriter(path.toFile());
        file.write(String.join("\n", MATCH_LIST));
        file.close();

        List<String> missing = new ArrayList<>();

        big: for(String expected : EXPECTED_MATCHES) {
            String namedExpected = "minecraft:" + expected;
            for(String match : MATCH_LIST) {
                if(match.startsWith(namedExpected)) continue big;
            }
            missing.add(namedExpected);
        }

        if(missing.isEmpty()) {
            LOGGER.info("All matches found!");
            return true;
        }

        String missings = String.join("\n", missing);
        LOGGER.error("Missing matches: {}", missings);
        Path missingPath = FabricLoader.getInstance().getGameDir().resolve("missing_list.txt");
        System.out.println(path);
        FileWriter missingFile = new FileWriter(missingPath.toFile());
        missingFile.write(missings);
        missingFile.close();

        return false;
    }
}
