/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.names.NameMatcher

sealed interface Condition

sealed interface FunctionCondition : Condition {
    context(typeResolver: TypeResolver)
    fun matches(funcName: NamedFunctionSignature): Boolean
}

sealed interface TypeCondition : Condition {
    context(typeResolver: TypeResolver)
    fun matches(type: TypeEmbedding): Boolean
}

/**
 * The dispatch receiver must satisfy all [conditions].
 * Returns false if the function has no dispatch receiver.
 */
data class ReceiverSatisfies(val conditions: List<TypeCondition>) : FunctionCondition {
    context(typeResolver: TypeResolver)
    override fun matches(funcName: NamedFunctionSignature): Boolean {
        val receiverType = funcName.callableType.dispatchReceiverType ?: return false
        return conditions.all { it.matches(receiverType) }
    }
}

/** Matches functions with no dispatch receiver */
data object HasNoReceiver : FunctionCondition {
    context(typeResolver: TypeResolver)
    override fun matches(funcName: NamedFunctionSignature): Boolean {
        var result = false
        NameMatcher.matchClassScope(funcName.name) {
            ifNoReceiver { result = true }
            return result
        }
    }
}

/** Matches functions whose simple name equals [name]. */
data class HasFunctionName(val name: String) : FunctionCondition {
    context(typeResolver: TypeResolver)
    override fun matches(funcName: NamedFunctionSignature): Boolean {
        var result = false
        NameMatcher.matchClassScope(funcName.name) {
            ifFunctionName(this@HasFunctionName.name) { result = true }
            return result
        }
    }
}

/** Matches constructors of the class named [className]. */
data class IsConstructorOf(val className: String) : FunctionCondition {
    context(typeResolver: TypeResolver)
    override fun matches(funcName: NamedFunctionSignature): Boolean {
        var result = false
        NameMatcher.matchClassScope(funcName.name) {
            ifConstructorOf(this@IsConstructorOf.className) { result = true }
            return result
        }
    }
}

/** Matches functions / types whose declaring package equals [pkg]. */
data class InPackage(val pkg: List<String>) : FunctionCondition, TypeCondition {
    context(typeResolver: TypeResolver)
    override fun matches(funcName: NamedFunctionSignature): Boolean {
        var result = false
        NameMatcher.matchClassScope(funcName.name) {
            ifPackageName(pkg) { result = true }
            return result
        }
    }

    context(typeResolver: TypeResolver)
    override fun matches(type: TypeEmbedding): Boolean {
        var result = false
        NameMatcher.matchClassScope(type.pretype.name) {
            ifPackageName(pkg) { result = true }
            return result
        }
    }
}

/** Matches types that are a subtype of [pkg].[className]. */
data class IsSubtype(val pkg: List<String>, val className: String) : TypeCondition {
    context(typeResolver: TypeResolver)
    override fun matches(type: TypeEmbedding): Boolean =
        typeResolver.isInheritorOf(type.pretype, pkg, className)
}
