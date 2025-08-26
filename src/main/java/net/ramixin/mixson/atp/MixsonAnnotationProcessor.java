package net.ramixin.mixson.atp;

import net.ramixin.mixson.atp.annotations.Codec;
import net.ramixin.mixson.atp.annotations.Generator;
import net.ramixin.mixson.atp.annotations.events.GenerativeMixsonEvent;
import net.ramixin.mixson.atp.annotations.events.MixsonEvent;
import net.ramixin.mixson.atp.processors.EventPreprocessors;
import net.ramixin.mixson.atp.processors.EventProcessors;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class MixsonAnnotationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger("mixsonAPT");

    public static void processClass(Class<?> clazz) {
        for(Method method : clazz.getDeclaredMethods()) preprocessMethod(method);
        for(Method method : clazz.getDeclaredMethods()) processMethod(method);
    }

    private static void processMethod(Method method) {
        enforceExclusivity(method, MixsonEvent.class, GenerativeMixsonEvent.class, Generator.class, Codec.class);
        processAnnotation(MixsonEvent.class, method, EventProcessors::handleMixsonEvent);
        processAnnotation(GenerativeMixsonEvent.class, method, EventProcessors::handleGenerativeMixsonEvent);
    }

    private static void preprocessMethod(Method method) {
        processAnnotation(Generator.class, method, EventPreprocessors::handleGenerator);
        processAnnotation(Codec.class, method, EventPreprocessors::handleCodec);
    }

    private static <T extends Annotation> void processAnnotation(Class<T> annotationClazz, Method method, TriConsumer<T, Method, Logger> callback) {
        T annotation = method.getAnnotation(annotationClazz);
        if(annotation == null) return;
        callback.accept(annotation, method, MixsonAnnotationProcessor.LOGGER);
    }

    @SafeVarargs
    private static void enforceExclusivity(Method method, Class<? extends Annotation>... annotations) {
        boolean safe = true;
        for(Class<? extends Annotation> annotation : annotations) if(method.isAnnotationPresent(annotation)) {
            if(safe) safe = false;
            else throw new IllegalStateException(String.format("method '%s' can only be annotated with one of the following: '%s'", method.getName(), Arrays.stream(annotations).map(Class::getName).collect(Collectors.toList())));
        }
    }
}
