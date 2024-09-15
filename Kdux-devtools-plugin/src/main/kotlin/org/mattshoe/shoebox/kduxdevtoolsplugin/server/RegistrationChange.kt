package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration

data class RegistrationChange(
    val value: Registration,
    val removed: Boolean = false
)