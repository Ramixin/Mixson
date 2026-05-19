package net.ramixin.mixson.test;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ResourceProcessingGameTest implements CustomTestMethodInvoker {

    private boolean assertion = false;

    @Override
    public void invokeTestMethod(@NonNull GameTestHelper context, @NonNull Method method) throws InvocationTargetException, IllegalAccessException {
        try {
            assertion = MixsonTestEntrypoint.assertMatches();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //context.getLevel().tickRateManager().setFrozen(true);
        method.invoke(this, context);
    }

    @GameTest
    void test(GameTestHelper context) {
        context.assertTrue(assertion, "Not All Matches Found");
        context.succeed();
    }

}
