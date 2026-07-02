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

fun loopInvariants(@Suppress("UNUSED_PARAMETER") body: () -> Unit) = Unit

fun preconditions(@Suppress("UNUSED_PARAMETER") body: () -> Unit) = Unit

fun <T> postconditions(@Suppress("UNUSED_PARAMETER") body: (T) -> Unit) = Unit


fun <T> forAll(@Suppress("UNUSED_PARAMETER") body: InvariantBuilder.(T) -> Unit): Boolean =
    throw FormverFunctionCalledInRuntimeException("forAll")


fun <T> exists(@Suppress("UNUSED_PARAMETER") body: InvariantBuilder.(T) -> Unit): Boolean =
    throw FormverFunctionCalledInRuntimeException("exists")


fun <T> old(@Suppress("UNUSED_PARAMETER") body: T): T =
    throw FormverFunctionCalledInRuntimeException("old")


/**
 * Requests access permission to the field denoted by [path] in a pre- or postcondition.
 *
 * [path] must be a field access such as `x.a`. The optional [permission] selects how much
 * permission is requested; use [write] for full (the default) or [read] for a read-only
 * (wildcard) fraction.
 */
fun acc(@Suppress("UNUSED_PARAMETER") path: Any?, @Suppress("UNUSED_PARAMETER") permission: Any? = null): Boolean =
    throw FormverFunctionCalledInRuntimeException("acc")

/**
 * Denotes a read-only (wildcard) permission amount. Only meaningful as the second argument of [acc].
 */
fun read(): Any? =
    throw FormverFunctionCalledInRuntimeException("read")

/**
 * Denotes a full (write) permission amount. Only meaningful as the second argument of [acc].
 */
fun write(): Any? =
    throw FormverFunctionCalledInRuntimeException("write")

class InvariantBuilder {
    /**
     * Specifies trigger expressions for quantifiers.
     * This function should be called within a `forAll` block to provide user-defined triggers
     * for SMT solver guidance.
     */
    fun triggers(@Suppress("UNUSED_PARAMETER") vararg expressions: Any?): Unit =
        throw FormverFunctionCalledInRuntimeException("triggers")
}
