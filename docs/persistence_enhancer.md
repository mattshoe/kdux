# PersistenceEnhancer

The `PersistenceEnhancer` is an enhancer designed to automatically persist and restore the state of a store. This
enhancer ensures that the state of the store is saved to a persistent storage medium and restored upon initialization,
providing persistence across application restarts or other lifecycle events.

## Overview

The `PersistenceEnhancer` is used when you need to ensure that the state of your application, or specific parts of it,
is preserved even when the application is restarted or closed. It works by serializing the state to a file and
deserializing it on initialization, thereby maintaining a consistent state across application lifecycles.

## Configuration

- **key:** The `key` parameter determines the filename or unique identifier under which the state is stored. If multiple
  stores are being persisted, ensure the key is unique to avoid conflicts.
- **serializer:** A function that serializes the state into the provided `OutputStream`. It must write the state in a
  format that you can later deserialize.
- **deserializer:** A function that deserializes the state from the provided `InputStream`. It must return a state
  object that matches the type of the store's state.
- **onError:** This function is invoked any time an error goes uncaught during serialization/deserialization and File
  IO.

## Usage Example

The Kdux dsl provides full support for all popular serialization libraries, including Gson, Kotlinx Serialization, Moshi,
and more. Refer to the [Third Party Support](third_party_support.md) document for how to use them.

Here’s a basic example of how to use the `PersistenceEnhancer` in a **Kdux** store:

```kotlin
val store = kdux.store(
    initialState = MyState(),
    reducer = MyReducer()
) {
    persist(
        key = "myGloballyUniqueKey-${userId}",
        serializer = { state, outputStream -> /* Serialize state to be written to storage */ },
        deserializer = { inputStream -> /* Deserialize the inputStream into the proper state */ },
        onError = { state, error -> /* Handle error */ }
    )
}
```