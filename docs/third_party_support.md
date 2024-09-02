# Third Party Integration

Kdux allows you to mix-in support for third party libraries as you see fit. Kdux aims to support all major relevant 
third party libraries to provide a seamless and efficient development experience.

By breaking out support into numerous "add-on" dependencies, you can pick and choose only the support you need, so that
you can keep your application size as small as possible.

<br>
<br>

## Kotlinx Serialization

Kdux supports Kotlinx Serialization for its [State Persistence](persistence_enhancer.md) functionality.  

#### Dependency
```kotlin
dependencies {
    implementation("com.mattshoe.shoebox:Kdux.kotlinx-serialization:1.0.5")
}
```
#### Usage

```kotlin
val store = kdux.store(
    initialValue = MyState(),
    reducer = MyReducer()
) {
    persistWithKotlinxSerialization(key = "myGloballyUniqueKey") { state, error ->
        // handle error
    }
}
```




