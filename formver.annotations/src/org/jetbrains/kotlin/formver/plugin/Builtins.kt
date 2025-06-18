/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

private class FormverFunctionCalledInRuntimeException(offendingFunction: String) :
    RuntimeException("Function `$offendingFunction` should never be called in runtime.")

/**
 * Built-in function used to mark a boolean predicate to be verified in Viper.
 * This function hooks-in in the `formver` plugin, its invocation in a Kotlin
 * program does not do anything.
 */
fun verify(@Suppress("UNUSED_PARAMETER") vararg predicates: Boolean) = Unit

infix fun Boolean.implies(other: Boolean) = !this || other

fun loopInvariants(@Suppress("UNUSED_PARAMETER") body: InvariantBuilder.() -> Unit) = Unit

fun preconditions(@Suppress("UNUSED_PARAMETER") body: InvariantBuilder.() -> Unit) = Unit

fun <T> postconditions(@Suppress("UNUSED_PARAMETER") body: InvariantBuilder.(T) -> Unit) = Unit

/**
 * This class is designed as a receiver for lambda blocks of `loopInvariants`, `preconditions` and `postconditions`.
 */
class InvariantBuilder {
    fun <T> forAll(@Suppress("UNUSED_PARAMETER") body: (T) -> Unit): Boolean = throw FormverFunctionCalledInRuntimeException("forAll")
}
