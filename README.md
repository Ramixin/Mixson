<p align="center">

<img src="brand.png" alt="Mixson fabricated brand icon">

### A simple lightweight library for modded Minecraft that allows for .json resource files to be modified, created, or deleted at runtime through an event-based system.

</p>

---

## Downloading the Project

This project is no longer hosted via jitpack. Instead, Mixson can either be downloaded as a .jar from the
Modrinth page or can be added as a dependency to a project via the Modrinth Maven:
```gradle
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        filter {
            includeGroup "maven.modrinth"
        }
    }
}
```
After that is added,
the dependency can be added through inserting this into the `dependencies` section in the `build.gradle`:
```gradle

dependencies {
    ...
    implementation "maven.modrinth:mixson:VERSION"
    ...
}
```
Check the modrinth page for the latest version.
For more information, see the [Modrinth maven support article](https://support.modrinth.com/en/articles/8801191-modrinth-maven).

## Usage

See the [wiki](https://moddedmc.wiki/en/project/mixson/latest/docs) for indepth usage instructions and examples.
## License

This project is under an MIT
