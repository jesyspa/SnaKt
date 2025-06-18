/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

/**
 * When converting Kotlin methods, we explicitly track what local variables are declared in each scope
 * to resolve shadowing. In scopes where local variables are not permitted, we use a special `NoScope`
 * placeholder that is treated as an error downstream.
 */
sealed class ScopeIndex {
    data class Indexed(val index: Int) : ScopeIndex()
    data object NoScope : ScopeIndex()
}