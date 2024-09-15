# Kdux DevTools Guide

## Overview

**Kdux DevTools** is an advanced debugging and state management tool for Kdux-based Kotlin applications. It allows
developers to gain full visibility into state transitions and actions, offering capabilities like time-travel debugging,
action replay, and even real-time action editing. These features help you quickly diagnose issues, reproduce bugs, and
test alternative scenarios without having to restart your application or manually recreate states.

## Why Kdux DevTools?

- **Time-Travel Debugging**: Move back and forth through previous state transitions and actions to see how the state
  evolved.
- **Action Replay**: Re-dispatch previous actions to see how they affect the current state.
- **Dispatch Snapshot Replay**: Restore the state at the start of a previous dispatch and replay the entire dispatch (
  state + action) as it occurred, simulating a "time-travel" effect to debug past flows.
- **Real-Time Action Editing**: Modify actions as they are dispatched, allowing you to test alternative inputs or
  scenarios on the fly during debugging.
- **State Override**: Override the current state with a new state or restore a previous state to inspect how the
  application behaves with that state.

## How to Use Kdux DevTools

To get started with **Kdux DevTools**, you need to follow two main steps:

1. **Install the Gradle Dependency** and integrate the `devtools(..)` extension function in the Kdux store.
2. **Install the Kdux DevTools IntelliJ Plugin** to enable the debugging interface in your IDE.

### 1. Install the Gradle Dependency

First, add the Kdux DevTools library to your `build.gradle.kts` dependencies:

```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux-devtools:1.0.9") // Replace with the correct version
}
```

Next, apply the devtools(..) extension function to your store configuration.

```kotlin
import org.mattshoe.shoebox.devtools.DevToolsSerializer

// Serializer for Actions and State
val serializer = object : DevToolsSerializer<MyState, MyAction> {
    override suspend fun serializeAction(action: MyAction) = Json.encodeToString(action)
    override suspend fun deserializeAction(data: String) = Json.decodeFromString(data)
    override suspend fun serializeState(state: MyState) = Json.encodeToString(state)
    override suspend fun deserializeState(data: String) = Json.decodeFromString(data)
}

// Configure the store
val store = kdux.store(
    initialState = MyState(),
    reducer = MyReducer()
) {
    // Add the DevTools
    devtools(serializer)
}
```

In this example, the devtools(serializer) extension integrates the store with Kdux DevTools, enabling all the powerful
debugging features.

### 2. Install the Kdux DevTools IntelliJ Plugin

Once you've integrated the DevTools into your store, install the **Kdux DevTools IntelliJ Plugin** to enable debugging
within the IDE. You can find the plugin on the JetBrains Marketplace or install it directly from your IntelliJ IDE.

#### Features of the Kdux DevTools Plugin:

- **View and Step Through State Transitions**: Each dispatched action and the resulting state change is visible, helping
  you trace exactly how your app is reacting to different actions.

- **Time-Travel Debugging**: Navigate backward and forward through previous state transitions and actions, allowing you
  to see the state of your application at any point in time.

- **Action Replay**: Select any action from the history and replay it in the current state. This is useful for testing
  how the same action affects the app under different conditions.

- **Dispatch Snapshot Replay**: Replay an entire dispatch cycle by restoring the state at the start of a dispatch and
  replaying the action. This simulates "time-travel," giving you a chance to observe how the app behaved at a specific
  point in time.

- **Real-Time Action Editing**: While stepping through your dispatch history, you can edit actions before replaying
  them. This feature allows you to test alternative scenarios by modifying the action payload.

- **State Overrides**: Manually override the current state to test specific conditions or edge cases, without needing to
  re-run the entire app or recreate scenarios from scratch.

### How to Install the Plugin

1. Open your IntelliJ IDEA or Android Studio.
2. Go to `Settings` > `Plugins` > `Marketplace`.
3. Search for **Kdux DevTools**.
4. Install the plugin and restart the IDE.

Once installed, you will have access to the **Kdux DevTools** window. This window provides a detailed interface where
you can monitor and control the state and actions in real time.

#### Example Workflow

1. Open the **Kdux DevTools** window from the sidebar.
2. Start interacting with your app.
3. Watch as actions are dispatched and the state updates in real time.
4. Use the time-travel debugging feature to step through past actions and states.
5. Replay actions or entire dispatch cycles to simulate scenarios.
6. If needed, modify actions or override the current state directly from the DevTools window.

With **Kdux DevTools**, you have powerful debugging capabilities that give you full control over your application's
state management, enabling faster and more efficient development workflows.
