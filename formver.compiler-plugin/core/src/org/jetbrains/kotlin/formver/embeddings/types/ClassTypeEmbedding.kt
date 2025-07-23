/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.types

import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.names.NameMatcher
import org.jetbrains.kotlin.formver.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.names.SpecialPackages
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.ast.DomainFunc
import org.jetbrains.kotlin.formver.viper.ast.Exp

// TODO: incorporate generic parameters.
data class ClassTypeEmbedding(val Name: ScopedKotlinName) : PretypeEmbedding {
    context(nameResolver: NameResolver)
    override val name: ScopedKotlinName
        get() = Name
    private var _details: ClassEmbeddingDetails? = null
    context(nameResolver: NameResolver)
    val details: ClassEmbeddingDetails
        get() = _details ?: error("Details of $name have not been initialised yet.")

    fun initDetails(details: ClassEmbeddingDetails) {
        require(_details == null) { "Class details already initialized" }
        _details = details
    }

    val hasDetails: Boolean
        get() = _details != null
    
    context(nameResolver: NameResolver)
    override val runtimeType: Exp
        get() = this.embedClassTypeFunc()()
    context(nameResolver: NameResolver)
    override fun accessInvariants(): List<TypeInvariantEmbedding> = details.accessInvariants()

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    context(nameResolver: NameResolver)
    override fun sharedPredicateAccessInvariant() = details.sharedPredicateAccessInvariant()
    context(nameResolver: NameResolver)
    override fun uniquePredicateAccessInvariant() = details.uniquePredicateAccessInvariant()
}
context(nameResolver: NameResolver)
fun PretypeEmbedding.isInheritorOfCollectionTypeNamed(name: String): Boolean {
    val classEmbedding = this as? ClassTypeEmbedding ?: return false
    return isCollectionTypeNamed(name) || classEmbedding.details.superTypes.any {
        it.isInheritorOfCollectionTypeNamed(name)
    }
}
context(nameResolver: NameResolver)
val PretypeEmbedding.isCollectionInheritor
    get() = isInheritorOfCollectionTypeNamed("Collection")
context(nameResolver: NameResolver)
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
context(nameResolver: NameResolver)
fun ClassTypeEmbedding.embedClassTypeFunc(): DomainFunc = RuntimeTypeDomain.classTypeFunc(name)