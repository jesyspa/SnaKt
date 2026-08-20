/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION)
internal annotation class SpecificationHelper

private class FormverFunctionCalledInRuntimeException(offendingFunction: String) :
    RuntimeException("Function `$offendingFunction` should never be called in runtime.")

/**
 * Built-in function used to mark a boolean predicate to be verified in Viper.
 * This function hooks-in in the `formver` plugin, its invocation in a Kotlin
 * program does not do anything.
 */
@SpecificationHelper
fun verify(@Suppress("UNUSED_PARAMETER") vararg predicates: Boolean) = Unit

infix fun Boolean.implies(other: Boolean) = !this || other

@SpecificationHelper
fun loopInvariants(@Suppress("UNUSED_PARAMETER") body: @Borrowed () -> Unit) = Unit

@SpecificationHelper
fun preconditions(@Suppress("UNUSED_PARAMETER") body: @Borrowed () -> Unit) = Unit

@SpecificationHelper
fun <T> postconditions(@Suppress("UNUSED_PARAMETER") body: @Borrowed (T) -> Unit) = Unit

@SpecificationHelper
fun <T> forAll(@Suppress("UNUSED_PARAMETER") body: @Borrowed InvariantBuilder.(T) -> Unit): Boolean =
    throw FormverFunctionCalledInRuntimeException("forAll")

@SpecificationHelper
fun <T> old(@Suppress("UNUSED_PARAMETER") body: @Borrowed T): T =
    throw FormverFunctionCalledInRuntimeException("old")


/**
 * Requests access permission to the field denoted by [path] in a pre- or postcondition.
 *
 * [path] must be a field access such as `x.a`. The optional [permission] selects how much
 * permission is requested; use [write] for full (the default) or [read] for a read-only
 * (wildcard) fraction.
 */
@SpecificationHelper
fun acc(
    @Suppress("UNUSED_PARAMETER") path: Any?,
    @Suppress("UNUSED_PARAMETER") permission: Permission? = null
): Boolean =
    throw FormverFunctionCalledInRuntimeException("acc")

/**
 * An amount of permission to a location, such as full ([write]) or read-only ([read]).
 */
interface Permission

/**
 * A verification predicate over [exp].
 */
abstract class Predicate(val exp: Any)

/**
 * Denotes a read-only (wildcard) permission amount. Only meaningful as the second argument of [acc].
 */
@SpecificationHelper
fun read(): Permission =
    throw FormverFunctionCalledInRuntimeException("read")

/**
 * Denotes a full (write) permission amount. Only meaningful as the second argument of [acc].
 */
@SpecificationHelper
fun write(): Permission =
    throw FormverFunctionCalledInRuntimeException("write")

/**
 * The uniqueness predicate of [data]: exclusive access to [data] and its fields.
 */
data class UniquePred @SpecificationHelper constructor(val data: @Borrowed Any) : Predicate(data)

/**
 * Exchanges [exp] for access to its body, exposing the underlying fields. The inverse of [fold].
 *
 * [permission] is the amount of the predicate to unfold, defaulting to full ([write]).
 */
@SpecificationHelper
fun unfold(
    @Suppress("UNUSED_PARAMETER") exp: Predicate,
    @Suppress("UNUSED_PARAMETER") permission: Permission? = null
): Unit =
    throw FormverFunctionCalledInRuntimeException("unfold")

/**
 * Exchanges access to [exp]'s body for the predicate itself. The inverse of [unfold].
 *
 * [permission] is the amount of the predicate to fold, defaulting to full ([write]).
 */
@SpecificationHelper
fun fold(
    @Suppress("UNUSED_PARAMETER") exp: Predicate,
    @Suppress("UNUSED_PARAMETER") permission: Permission? = null
): Unit = throw FormverFunctionCalledInRuntimeException("fold")

class InvariantBuilder @SpecificationHelper constructor() {
    /**
     * Specifies trigger expressions for quantifiers.
     * This function should be called within a `forAll` block to provide user-defined triggers
     * for SMT solver guidance.
     */
    @SpecificationHelper
    fun triggers(@Suppress("UNUSED_PARAMETER") vararg expressions: Any?): Unit =
        throw FormverFunctionCalledInRuntimeException("triggers")
}
