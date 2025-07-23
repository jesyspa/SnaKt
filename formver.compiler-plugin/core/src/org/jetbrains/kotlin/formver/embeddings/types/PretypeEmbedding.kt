/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.types

import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.names.PretypeName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.mangled

/**
 * A representation of a Kotlin type without nullability and uniqueness information.
 *
 * We explicitly choose not to make this a subtype of `TypeEmbedding`, even though there is a simple way of treating
 * every `PretypeEmbedding` as a `TypeEmbedding`: the goal of the separation into types and pretypes is to avoid
 * one showing up where the other is expected.  For example, the naming systems are different, and the equality
 * comparisons would not work.
 *
 * All pretype embeddings must be `data` classes or objects!
 */
interface PretypeEmbedding : RuntimeTypeHolder, TypeInvariantHolder {
    context(nameResolver: NameResolver)
    val name: MangledName

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = PlaintextLeaf(name.mangled)

    override fun subTypeInvariant(): TypeInvariantEmbedding = SubTypeInvariantEmbedding(this)
}

data object UnitTypeEmbedding : PretypeEmbedding {
    context(nameResolver: NameResolver)
    override val runtimeType
        get() = RuntimeTypeDomain.unitType()
    context(nameResolver: NameResolver)
    override val name: PretypeName
        get() = PretypeName("Unit")
}

data object NothingTypeEmbedding : PretypeEmbedding {
    context(nameResolver: NameResolver)
    override val runtimeType
        get() = RuntimeTypeDomain.nothingType()
    context(nameResolver: NameResolver)
    override val name: PretypeName
        get() = PretypeName("Nothing")

    override fun pureInvariants(): List<TypeInvariantEmbedding> = listOf(FalseTypeInvariant)
}

data object AnyTypeEmbedding : PretypeEmbedding {
    context(nameResolver: NameResolver)
    override val runtimeType
        get() = RuntimeTypeDomain.anyType()
    context(nameResolver: NameResolver)
    override val name: PretypeName
        get() = PretypeName("Any")
}

data object IntTypeEmbedding : PretypeEmbedding {
    context(nameResolver: NameResolver)
    override val runtimeType
        get() = RuntimeTypeDomain.intType()
    context(nameResolver: NameResolver)
    override val name: PretypeName
        get() = PretypeName("Int")
}

data object BooleanTypeEmbedding : PretypeEmbedding {
    context(nameResolver: NameResolver)
    override val runtimeType
        get() = RuntimeTypeDomain.boolType()
    context(nameResolver: NameResolver)
    override val name: PretypeName
        get() = PretypeName("Boolean")
}

data object CharTypeEmbedding : PretypeEmbedding {
    context(nameResolver: NameResolver)
    override val runtimeType
        get() = RuntimeTypeDomain.charType()
    context(nameResolver: NameResolver)
    override val name: PretypeName
        get() = PretypeName("Char")
}

data object StringTypeEmbedding : PretypeEmbedding {
    context(nameResolver: NameResolver)
    override val runtimeType
        get() = RuntimeTypeDomain.stringType()
    context(nameResolver: NameResolver)
    override val name: PretypeName
        get() = PretypeName("String")
}

fun PretypeEmbedding.asTypeEmbedding() = TypeEmbedding(this, TypeEmbeddingFlags(nullable = false))

