/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import kotlin.reflect.KClass

/**
 * Marker FIR type attribute for `@Unique`.
 *
 * This attribute encodes only the presence of the uniqueness annotation on a type; it does not store path-sensitive
 * uniqueness state.
 *
 * Symbol-level uniqueness checking is computed separately by the uniqueness analysis pipeline. See [scopeUniqueness]
 * and [parameterUniqueness] for how a type-level attribute is interpreted into a [Uniqueness] value.
 */
data object UniquenessAttribute : ConeAttribute<UniquenessAttribute>() {
    override fun union(other: UniquenessAttribute?): UniquenessAttribute =
        this

    override fun intersect(other: UniquenessAttribute?): UniquenessAttribute? =
        other

    override fun add(other: UniquenessAttribute?): UniquenessAttribute =
        this

    override fun isSubtypeOf(other: UniquenessAttribute?): Boolean =
        other != null

    override val key: KClass<out UniquenessAttribute>
        get() = UniquenessAttribute::class

    val uniqueness: Uniqueness
        get() = Uniqueness.Unique

    override val keepInInferredDeclarationType: Boolean
        get() = true

    override val implementsEquality: Boolean
        get() = true

    override fun toString(): String = "UniquenessAttribute"
}

val ConeAttributes.uniquenessAttribute: UniquenessAttribute? by ConeAttributes.attributeAccessor<UniquenessAttribute>()
