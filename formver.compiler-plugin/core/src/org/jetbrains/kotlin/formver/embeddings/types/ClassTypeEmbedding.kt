/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.types

import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.names.NameMatcher
import org.jetbrains.kotlin.formver.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.names.SpecialPackages
import org.jetbrains.kotlin.formver.viper.ast.DomainFunc
import org.jetbrains.kotlin.formver.viper.ast.Exp

// TODO: incorporate generic parameters.
data class ClassTypeEmbedding(override val name: ScopedKotlinName) : PretypeEmbedding {
    private var _details: ClassEmbeddingDetails? = null
    val details: ClassEmbeddingDetails
        get() = _details ?: error("Details of $name have not been initialised yet.")

    fun initDetails(details: ClassEmbeddingDetails) {
        require(_details == null) { "Class details already initialized" }
        _details = details
    }

    val hasDetails: Boolean
        get() = _details != null

    override val runtimeType: Exp = this.embedClassTypeFunc()()

    override fun accessInvariants(): List<TypeInvariantEmbedding> = details.accessInvariants()

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun sharedPredicateAccessInvariant() = details.sharedPredicateAccessInvariant()

    override fun uniquePredicateAccessInvariant() = details.uniquePredicateAccessInvariant()
}

fun PretypeEmbedding.isInheritorOfCollectionTypeNamed(name: String): Boolean {
    val classEmbedding = this as? ClassTypeEmbedding ?: return false
    return isCollectionTypeNamed(name) || classEmbedding.details.superTypes.any {
        it.isInheritorOfCollectionTypeNamed(name)
    }
}

val PretypeEmbedding.isCollectionInheritor
    get() = isInheritorOfCollectionTypeNamed("Collection")

private fun PretypeEmbedding.isCollectionTypeNamed(name: String): Boolean {
    val classEmbedding = this as? ClassTypeEmbedding ?: return false
    NameMatcher.Companion.matchGlobalScope(classEmbedding.name) {
        ifInCollectionsPkg {
            ifClassName(name) {
                return true
            }
        }
        return false
    }
}

fun ClassTypeEmbedding.embedClassTypeFunc(): DomainFunc = RuntimeTypeDomain.classTypeFunc(name)