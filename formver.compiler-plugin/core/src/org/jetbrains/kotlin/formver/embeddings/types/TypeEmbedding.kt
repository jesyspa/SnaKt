/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.types

import org.jetbrains.kotlin.formver.domains.Injection
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain.Companion.stringType
import org.jetbrains.kotlin.formver.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.names.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.mangled

/**
 * Represents our representation of a Kotlin type.
 *
 * Due to name mangling, the mapping between Kotlin types and TypeEmbeddings must be 1:1.
 */
data class TypeEmbedding(val pretype: PretypeEmbedding, val flags: TypeEmbeddingFlags) : RuntimeTypeHolder, TypeInvariantHolder {
    /**
     * Name representing the type, used for distinguishing overloads.
     *
     * It may at some point necessary to make a `TypeName` hierarchy of some sort to
     * represent these names, but we do it inline for now.
     */
    val name: MangledName
        get() = TypeName(pretype, flags.nullable)

    /**
     * Get a nullable version of this type embedding.
     *
     * Note that nullability doesn't stack, hence nullable types must return themselves.
     */
    fun getNullable(): TypeEmbedding = copy(flags = flags.copy(nullable = true))

    /**
     * Drop nullability, if it is present.
     */
    fun getNonNullable(): TypeEmbedding = copy(flags = flags.copy(nullable = false))

    val isNullable: Boolean
        get() = flags.nullable

    override val runtimeType: Exp = flags.adjustRuntimeType(pretype.runtimeType)

    override fun accessInvariants(): List<TypeInvariantEmbedding> = flags.adjustManyInvariants(pretype.accessInvariants())
    override fun pureInvariants(): List<TypeInvariantEmbedding> = flags.adjustManyInvariants(pretype.pureInvariants())

    override fun sharedPredicateAccessInvariant(): TypeInvariantEmbedding? =
        flags.adjustOptionalInvariant(pretype.sharedPredicateAccessInvariant())

    override fun uniquePredicateAccessInvariant(): TypeInvariantEmbedding? =
        flags.adjustOptionalInvariant(pretype.uniquePredicateAccessInvariant())

    override fun subTypeInvariant(): TypeInvariantEmbedding = SubTypeInvariantEmbedding(this)

    override val debugTreeView: TreeView
        get() = PlaintextLeaf(name.mangled)
}

data class TypeEmbeddingFlags(val nullable: Boolean) {
    fun adjustRuntimeType(runtimeType: Exp): Exp =
        if (nullable) RuntimeTypeDomain.nullable(runtimeType)
        else runtimeType

    fun adjustInvariant(invariant: TypeInvariantEmbedding): TypeInvariantEmbedding =
        if (nullable) IfNonNullInvariant(invariant)
        else invariant

    fun adjustManyInvariants(invariants: List<TypeInvariantEmbedding>): List<TypeInvariantEmbedding> =
        invariants.map { adjustInvariant(it) }

    fun adjustOptionalInvariant(invariant: TypeInvariantEmbedding?): TypeInvariantEmbedding? =
        invariant?.let { adjustInvariant(it) }
}

inline fun TypeEmbedding.injectionOr(default: (TypeEmbedding) -> Injection): Injection {
    if (flags.nullable) return default(this)
    return when (this.pretype) {
        StringTypeEmbedding -> RuntimeTypeDomain.stringInjection
        CharTypeEmbedding -> RuntimeTypeDomain.charInjection
        IntTypeEmbedding -> RuntimeTypeDomain.intInjection
        BooleanTypeEmbedding -> RuntimeTypeDomain.boolInjection
        else -> default(this)
    }
}

val TypeEmbedding.injection
    get() = injectionOr {
        error("Type ${it.name} has no injection specified.")
    }

