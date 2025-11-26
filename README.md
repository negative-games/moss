
# moss

> **Spring-powered dependency injection and lifecycle management for Minecraft plugins**  
> PaperMC · BungeeCord · Velocity

`moss` is a tiny library that embeds a Spring `ApplicationContext` inside your Minecraft plugin and wires everything together for you.

Instead of:
- Huge monolithic main classes  
- Static singletons everywhere  
- Manual wiring of listeners, services, commands  

…you get:
- Constructor injection with real DI  
- Clear lifecycle hooks (`onLoad`, `onEnable`, `onReload`, `onDisable`)  
- Simple helpers to invoke and register beans across your plugin

This repository contains:
- Core Spring integration (`moss`)
- Platform bootstraps for Paper, BungeeCord, and Velocity
- Fully working example plugins for each platform

---

## Example Use-Cases:
* [Bungeecord Example](https://github.com/negative-games/moss/tree/main/example-plugins/bungeecord-plugin)
* [PaperMC Example](https://github.com/negative-games/moss/tree/main/example-plugins/paper-plugin)
* [Velocity Example](https://github.com/negative-games/moss/tree/main/example-plugins/velocity-plugin)

## Table of Contents
1. [Requirements](#requirements)
2. [Installation](#installation)
   - [Gradle (Groovy)](#gradle-groovy)
   - [Gradle (Kotlin DSL)](#gradle-kotlin-dsl)
   - [Maven](#maven)
4. [Core Concepts](#core-concepts)
   - [`@SpringComponent`](#springcomponent)
   - Lifecycle interfaces ([`Loadable`](#loadable) / [`Enableable`](#enableable) / [`Reloadable`](#reloadable) / [`Disableable`](#disableable))
   - `invokeBeans(...)`
8. [Building from Source](#building-from-source)

---

## Requirements

- **Java:** 21
- **Build tool:** Gradle or Maven
- **Minecraft platforms:**  
  - Paper ≥ 1.20+ / 1.21+ (example uses `1.21.4` API)
  - BungeeCord (example uses `1.20-R0.2` API)
  - Velocity (example uses `3.4.0-SNAPSHOT` API)
- **Spring:** `spring-context` 6.x

---

## Installation

`moss` is designed to be used as a shaded dependency in your plugin JAR:

1. Add `moss-<platform>` and `spring-context` as **implementation** dependencies.
2. Use **Shadow** (or equivalent) to **shade + relocate** Moss and Spring into your plugin.

> Replace `<version>` with the current release (e.g. `1.0.0` or whatever is published in Negative Games Repo / your repo).

### Gradle (Groovy)

```gradle
plugins {
    id 'java'
    id 'com.gradleup.shadow' version '9.2.2'
}

group = 'com.example.myplugin'
version = '1.0.0'

repositories {
    mavenCentral()
    
    maven {
        name = "negative-games-repo"
        url = "https://repo.negative.games/repository/maven-releases/"
    }
}

dependencies {
    // Platform API
    compileOnly "io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT"

    // Spring
    implementation "org.springframework:spring-context:6.2.13"

    // Moss (Paper platform)
    implementation "games.negative.moss:moss-paper:<version>"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    build {
        dependsOn shadowJar
    }
}

shadowJar {
    archiveBaseName.set("MyPlugin")
    archiveClassifier.set("")
    archiveVersion.set("")

    // Very important: relocate Moss (and optionally Spring)
    relocate "games.negative.moss", "${project.group}.libs.moss"
    // relocate "org.springframework", "${project.group}.libs.spring"
}
```

### Gradle (Kotlin DSL)

```kotlin
plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "com.example.myplugin"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.negative.games/repository/maven-releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    implementation("org.springframework:spring-context:6.2.13")
    implementation("games.negative.moss:moss-paper:<version>")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    named("build") {
        dependsOn(named("shadowJar"))
    }
}

tasks.shadowJar {
    archiveBaseName.set("MyPlugin")
    archiveClassifier.set("")
    archiveVersion.set("")

    relocate("games.negative.moss", "${project.group}.libs.moss")
}
```

### Maven

Conceptually similar:

* Add `moss-paper`, `moss-bungeecord`, or `moss-velocity` as a dependency.
* Use the Maven Shade plugin to relocate `games.negative.moss` (and optionally Spring) into your plugin jar.

```xml
<repositories>
    <repository>
        <id>negative-games-repo</id>
        <url>https://repo.negative.games/repository/maven-releases/</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>games.negative.moss</groupId>
    <artifactId>moss-paper</artifactId>
    <version>&lt;version&gt;</version>
</dependency>
```

(Then configure `maven-shade-plugin` with a relocation rule from `games.negative.moss` → `com.example.myplugin.libs.moss`.)

---

## Core Concepts

### `@SpringComponent`

```java
package games.negative.moss.spring;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface SpringComponent {
    String value() default "";
}
```

`@SpringComponent` is a thin wrapper around Spring’s `@Component`. You use it on any class that should be created and managed by the Spring `ApplicationContext` inside your plugin:

* Listeners
* Commands
* Services / Managers
* Repositories, etc.

**Important:**
Moss scans the package of your main plugin class and its sub-packages:

```java
private String basePackage() {
    return this.getClass().getPackageName();
}
```

So make sure your components live under the same package root as your main class, e.g.:

* Main plugin class: `com.example.myplugin.MyPlugin`
* Components: `com.example.myplugin.listener.*`, `com.example.myplugin.service.*`, etc.

---

### Lifecycle Interfaces

All lifecycle interfaces live in `games.negative.moss.spring`:

#### `Loadable`

```java
public interface Loadable {
    void onLoad(GenericApplicationContext context);
}
```

* Called during **plugin load**, after the Spring context is created and refreshed.
* Use this for:

    * Registering additional beans programmatically
    * Early initialization that depends on the `ApplicationContext`
    * Setting up things that must exist before `onEnable` (configurations, database connections, etc.)

#### `Enableable`

```java
public interface Enableable {
    void onEnable();
}
```

* Called when the plugin is **enabled** (Paper/Bungee) or when the proxy finishes initialization (Velocity).
* Use this for:

    * Starting schedulers
    * Registering commands
    * Registering listeners
    * Registering stuff that should exist while the plugin is “up”

#### `Reloadable`

```java
public interface Reloadable {
    void onReload();
}
```

* Called when `reload()` is invoked on the base Moss class.
* Use this for:

    * Config reloads
    * Rebuilding caches
    * Re-binding configuration-driven services

> Each platform base class calls `reload()` once during `onEnable` / initialization as an “initial reload”.

#### `Disableable`

```java
public interface Disableable {
    void onDisable();
}
```

* Called when the plugin is **disabled** / proxy stops.
* Use this for:

    * Flushing data
    * Closing connections
    * Cleaning up resources

---

### `invokeBeans(...)`

All platform base classes expose:

```java
public <T> void invokeBeans(
        Class<T> clazz,
        Consumer<T> consumer,
        BiConsumer<T, Exception> onFailure
)

public <T> void invokeBeans(
        Class<T> clazz,
        Consumer<T> consumer
)
```

* Finds **all** beans of type `clazz` in the Spring context.
* Invokes `consumer.accept(bean)` for each bean.
* If `consumer` throws, `onFailure.accept(bean, ex)` is called.

Typical use cases:

* Register every `Listener` with the platform’s event system
* Register every `CommandExecutor` or similar
* Run an initialization pass over multiple services

---

## Building from Source

Clone the repository and run:

```bash
gradle publishToMavenLocal
```

To build one of the example plugins (shaded JAR):

```bash
# Paper example plugin
gradle :example-plugins:paper-plugin:build

# Bungee example plugin
gradle :example-plugins:bungeecord-plugin:build

# Velocity example plugin
gradle :example-plugins:velocity-plugin:build
```

The shaded JARs will be in `example-plugins/<module>/build/libs`.

---
