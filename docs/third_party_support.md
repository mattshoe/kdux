# Third Party Integration

Kdux allows you to mix-in support for third party libraries as you see fit. Kdux aims to support all major relevant 
third party libraries to provide a seamless and efficient development experience.

By breaking out support into numerous "add-on" dependencies, you can pick and choose only the support you need, so that
you can keep your application size as small as possible.

<br>
<br>

# Android

Kdux provides full integration with the Android SDK, leveraging LogCat, on-device caching, `Parcelable` serialization,
`WorkManager` integration, and all other relevant features of the Android platform.

#### Dependency
![Maven Central Version](https://img.shields.io/maven-central/v/org.mattshoe.shoebox/Kdux-android)

```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-android:1.0.10")
}
```

#### Usage

```kotlin
// Configure Kdux globally to integrate with Android. Leverage LogCat, Parcelable, on-device caching, and more
class MyApplication: Application() {
    override fun onCreate() {
        kdux { 
           android(this@MyApplication)
        }
    }
}

// Now when constructing your stores, you can leverage Parcelable for serialization
val store = kdux.store(
    initialValue = MyState(),
    reducer = MyReducer()
) {
    persistAsParcelable(key = "myGloballyUniqueKey") { state, error ->
        // handle error
    }
}
```

<br>
<br>

# Serialization

## Kotlinx Serialization

Kdux supports [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md) for its [State Persistence](persistence_enhancer.md) functionality.  

#### Dependency
![Maven Central Version](https://img.shields.io/maven-central/v/org.mattshoe.shoebox/Kdux-kotlinx-serialization)

```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-kotlinx-serialization:1.0.10")
}
```

#### Usage

```kotlin
val store = kdux.store(
    initialValue = MyState(),
    reducer = MyReducer()
) {
    persistWithKotlinxSerialization(
        key = "myGloballyUniqueKey"
    ) { state, error ->
        // handle error
    }
}
```

## Gson

Kdux supports [Gson](https://github.com/google/gson) for its [State Persistence](persistence_enhancer.md) functionality.

#### Dependency
![Maven Central Version](https://img.shields.io/maven-central/v/org.mattshoe.shoebox/Kdux-gson)
```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-gson:1.0.10")
}
```

#### Usage

```kotlin
val store = kdux.store(
    initialValue = MyState(),
    reducer = MyReducer()
) {
    persistWithGson(
        key = "myGloballyUniqueKey"
    ) { state, error ->
        // handle error
    }
}
```

## Moshi

Kdux supports [Moshi](https://github.com/square/moshi) for its [State Persistence](persistence_enhancer.md) functionality.

#### Dependency![Maven Central Version](https://img.shields.io/maven-central/v/org.mattshoe.shoebox/Kdux-moshi)

```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-moshi:1.0.10")
}
```

#### Usage

```kotlin
val store = kdux.store(
    initialValue = MyState(),
    reducer = MyReducer()
) {
    persistWithMoshi(
        key = "myGloballyUniqueKey"
    ) { state, error ->
        // handle error
    }
}
```

## ProtoBuf (coming soon)

Kdux supports Google's [ProtoBuf](https://github.com/square/moshi) serialization for its [State Persistence](persistence_enhancer.md) functionality.

#### Dependency
```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-protobuf:1.0.10")
}
```

#### Usage

```kotlin
val store = kdux.store(
    initialValue = MyState(),
    reducer = MyReducer()
) {
    persistWithProtoBuf(
        key = "myGloballyUniqueKey"
    ) { state, error ->
        // handle error
    }
}
```

## Jackson (coming soon)

Kdux supports [Jackson](https://github.com/FasterXML/jackson-module-kotlin) serialization for its [State Persistence](persistence_enhancer.md) functionality.

#### Dependency
```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-jackson:1.0.10")
}
```

#### Usage

```kotlin
val store = kdux.store(
    initialValue = MyState(),
    reducer = MyReducer()
) {
    persistWithJackson(
        key = "myGloballyUniqueKey"
    ) { state, error ->
        // handle error
    }
}
```




