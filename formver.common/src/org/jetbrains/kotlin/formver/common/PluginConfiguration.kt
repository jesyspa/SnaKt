/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

data class PluginConfiguration(
    val logLevel: LogLevel,
    val errorStyle: ErrorStyle,
    val behaviour: UnsupportedFeatureBehaviour,
    val conversionSelection: TargetsSelection,
    val verificationSelection: TargetsSelection,
    val checkLocality: Boolean = DEFAULT_CHECK_LOCALITY,
    val checkUniqueness: Boolean = DEFAULT_CHECK_UNIQUENESS,
    val dumpUniquenessCFG: Boolean = false,
) {
    // The Viper encoding reads `@Unique` and `@Borrowed` off types whether or not the checkers run, so leaving them
    // off gives a program whose annotations nothing validates.
    companion object {
        const val DEFAULT_CHECK_LOCALITY = true
        const val DEFAULT_CHECK_UNIQUENESS = true
    }

    init {
        require(conversionSelection >= verificationSelection) {
            "Conversion options may not be stricter than verification options; converting $conversionSelection but verifying $verificationSelection."
        }
    }
}
