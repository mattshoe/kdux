package org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel

import kotlinx.coroutines.flow.Flow

interface ViewModel<State: Any, UserIntent: Any> {
    val state: Flow<State>
    fun handleIntent(intent: UserIntent)
    fun dispose()
}