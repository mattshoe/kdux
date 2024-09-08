# Third Party Integration

Kdux allows you to mix-in support for third party libraries as you see fit. Kdux aims to support all major relevant 
third party libraries to provide a seamless and efficient development experience.

By breaking out support into numerous "add-on" dependencies, you can pick and choose only the support you need, so that
you can keep your application size as small as possible.

<br>
<br>

## Android SDK

Kdux provides full integration with the Android SDK, leveraging LogCat, on-device caching, `Parcelable` serialization,
`WorkManager` integration, and all other relevant features of the Android platform.

#### Dependency

```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-android:1.0.8")
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

## Room

Kdux supports using [Room](https://developer.android.com/training/data-storage/room) for its [State Persistence](persistence_enhancer.md) functionality.

#### Dependency
```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-room:1.0.8")
}
```

#### Usage

```kotlin
val store = kdux.store(
    initialValue = MyState(),
    reducer = MyReducer()
) {
    persistWithRoom(
        key = "myGloballyUniqueKey", 
        dao = myDao
    ) { state, error ->
        // handle error
    }
}
```


<br>
<br>

## Realm

Kdux supports using [Realm](https://github.com/realm/realm-java) for its [State Persistence](persistence_enhancer.md) functionality.

#### Dependency
```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-realm:1.0.8")
}
```

#### Usage

```kotlin
val store = kdux.store(
    initialValue = MyState(),
    reducer = MyReducer()
) {
    persistWithRealm(
        key = "myGloballyUniqueKey", 
        realm = { Realm.getDefaultInstance() }
    ) { state, error ->
        // handle error
    }
}
```


<br>
<br>

## Kotlinx Serialization

Kdux supports [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md) for its [State Persistence](persistence_enhancer.md) functionality.  

#### Dependency
```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-kotlinx-serialization:1.0.8")
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

<br>
<br>

## Gson

Kdux supports [Gson](https://github.com/google/gson) for its [State Persistence](persistence_enhancer.md) functionality.

#### Dependency
```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-gson:1.0.8")
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

<br>
<br>

## ProtoBuf

Kdux supports Google's [ProtoBuf](https://github.com/square/moshi) serialization for its [State Persistence](persistence_enhancer.md) functionality.

#### Dependency
```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-protobuf:1.0.8")
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

<br>
<br>

## Moshi

Kdux supports [Moshi](https://github.com/square/moshi) for its [State Persistence](persistence_enhancer.md) functionality.

#### Dependency
```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-moshi:1.0.8")
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



<br>
<br>

## Jackson

Kdux supports [Jackson](https://github.com/FasterXML/jackson-module-kotlin) serialization for its [State Persistence](persistence_enhancer.md) functionality.

#### Dependency
```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-jackson:1.0.8")
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




