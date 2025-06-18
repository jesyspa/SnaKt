/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.types

import org.jetbrains.kotlin.formver.embeddings.expression.debug.DebugPrintable
import org.jetbrains.kotlin.formver.viper.ast.Exp

interface RuntimeTypeHolder : DebugPrintable {
    /**
     * A Viper expression with the runtime representation of the type.
     *
     * The Viper values are defined in RuntimeTypeDomain and are used for casting, subtyping and the `is` operator.
     */
    val runtimeType: Exp
}