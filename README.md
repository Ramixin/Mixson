# Mixson

A simple lightweight library for modded Minecraft
that allows for .json resource files to be accessed and edited in-code with an event system

---

## Supported MC Versions

| MC Version | Mod Version     |
|------------|-----------------|
| 1.21.4     | ✅ V:0.0.3       |
| <= 1.21.3  | ❌  Incompatible |

## Downloading the Project

This project can be installed through jitpack. First, add the following to the *first* `repositories` section in the `build.gradle`:
```gradle
repositories {
    ...
    mavenCentral()
    maven { url 'https://jitpack.io' }
    ...
}
```
After that is added,
the dependency can be added through inserting this into the `dependencies` section in the `build.gradle`:
```gradle

dependencies {
    ...
    modImplementation 'com.github.ramixin:mixson:TAG'
    ...
}
```

The `TAG` in the above section is where the specific version of Mixson will go. To find a version, go to https://jitpack.io/#ramixin/mixson and choose one of the versions to be pasted in where `TAG` is.

## Registering an Event

There are three methods made available to the user for declaring an event:

```java
    void registerModificationEvent(Identifier resourceId, Identifier eventId, final MixsonEvent event)

    void registerModificationEvent(int priority, Identifier resourceId, Identifier eventId, final MixsonEvent event)

void registerModificationEvent(int priority, Identifier resourceId, Identifier eventId, final MixsonEvent event, boolean failSilently)

```

The default priority is `1000`. Events with a lower priority value will be applied first.

The `resourceId` is the path to the .json file to be modified. This path excludes the extension of the file (which will only ever be `.json`)

The `eventId` is an identifier that will be associated with the event in case the event fails so that the programmer can easily figure out what happened. Mixson does *not* check if multiple events are registered with the same identifier, but having multiple of the same ids is heavily discouraged for readability and the programmer's sanity.

the `MixsonEvent` is a functional interface with the following method:

```java
    JsonElement run(JsonElement elem);
```

The method is expected to return the provided `JsonElement` with any modifications the event applied since events are chained and not doing so will override any previous events or redefine the .json file in a way that will cause the event to fail. 

The `failSilently` is a boolean value
for whether the event should halt the game if the event fails to apply the modifications.

Here is an example event that will change the texture of the golden chestplate item to that of the diamond chestplate
```java

Mixson.registerModificationEvent(
    Identifier.ofVanilla("models/item/golden_chestplate"),
    Identifier.of("modid", "eventid"),
    (elem) -> {
        elem.getAsJsonObject()
            .getAsJsonObject("textures")
            .addProperty("layer0", "minecraft:item/diamond_chestplate");
        return elem;
    }
);

```

## Error Handling

By default, Mixson will deadlock the game in the middle of reloading the resources when an event fails. The failure message will match the pattern below:
```
Failed to modify json file 'minecraft:file/path' with event 'modid:eventid'
```
This message will be followed by the full error stacktrace as well as the exception and message that causes the event failure.

However,
the error can be sent to the log without the exception
if the event was registered with the `failSilently` parameter set to true. Note that this will only work if the error is in the application of the modification. If the modification causes MC to fail decoding the file, an error will still be thrown.


---
## License

This project is under an MIT