package net.ramixin.mixson.util;

import net.minecraft.resources.ResourceLocation;

public class ResourceLocationUtil {
    public static ResourceLocation parse(String string) {

        return bySeparator(string, ':');
    }

    public static ResourceLocation bySeparator(String string, char c) {
        int i = string.indexOf(c);
        if (i >= 0) {
            String string2 = string.substring(i + 1);
            if (i != 0) {
                String string3 = string.substring(0, i);
                return new ResourceLocation(string3, string2);
            } else {
                return new ResourceLocation("minecraft", string2);
            }
        } else {
            return new ResourceLocation("minecraft", string);
        }
    }

}
