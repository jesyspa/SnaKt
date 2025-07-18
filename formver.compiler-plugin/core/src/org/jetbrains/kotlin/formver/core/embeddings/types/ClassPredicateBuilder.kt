/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.UserFieldEmbedding
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.DispatchReceiverName
import org.jetbrains.kotlin.formver.core.names.SimpleKotlinName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

internal class ClassPredicateBuilder private constructor(private val details: ClassEmbeddingDetails) {
    private val subject = PlaceholderVariableEmbedding(DispatchReceiverName, details.type.asTypeEmbedding())
    private val body = mutableListOf<ExpEmbedding>()

    companion object {
        fun build(
            classType: ClassEmbeddingDetails, predicateName: MangledName,
            action: ClassPredicateBuilder.() -> Unit,
        ): Predicate {
            val builder = ClassPredicateBuilder(classType)
            builder.action()
            return Predicate(
                predicateName,
                listOf(builder.subject.toLocalVarDecl()),
                builder.body.toConjunction().pureToViper(toBuiltin = true)
            )
        }
    }

    fun forEachField(action: FieldAssertionsBuilder.() -> Unit) =
        details.fields.values
            .filterIsInstance<UserFieldEmbedding>()
            .forEach { field ->
                val builder = FieldAssertionsBuilder(subject, field)
                builder.action()
                body.addAll(builder.toAssertionsList())
            }

    fun forUserFieldNamed(name: String, action: FieldAssertionsBuilder.() -> Unit) {
        when (val field = details.fields[SimpleKotlinName(Name.identifier(name))]) {
            is UserFieldEmbedding -> {
                val builder = FieldAssertionsBuilder(subject, field)
                builder.action()
                body.addAll(builder.toAssertionsList())
            }
        }
    }

    fun forEachSuperType(action: TypeInvariantsBuilder.() -> Unit) =
        details.superTypes.forEach { type ->
            val builder = TypeInvariantsBuilder(type.asTypeEmbedding())
            builder.action()
            body.addAll(builder.toInvariantsList().fillHoles(subject))
        }
}

class FieldAssertionsBuilder(private val subject: VariableEmbedding, private val field: UserFieldEmbedding) {
    private val assertions = mutableListOf<ExpEmbedding>()
    fun toAssertionsList() = assertions.toList()

    val isAlwaysReadable = field.accessPolicy == AccessPolicy.ALWAYS_READABLE
    val isUnique = field.isUnique
    val nameAsString: String = field.name.name.mangledBaseName

    fun forType(action: TypeInvariantsBuilder.() -> Unit) {
        val builder = TypeInvariantsBuilder(field.type)
        builder.action()
        assertions.addAll(builder.toInvariantsList().fillHoles(PrimitiveFieldAccess(subject, field)))
    }

    fun addAccessPermissions(perm: PermExp) =
        assertions.add(FieldAccessTypeInvariantEmbedding(field, perm).fillHole(subject))

    fun addEqualsGuarantee(block: ExpEmbedding.() -> ExpEmbedding) {
        assertions.add(FieldEqualsInvariant(field, subject.block()).fillHole(subject))
    }
}

class TypeInvariantsBuilder(private val type: TypeEmbedding) {
    private val invariants = mutableListOf<TypeInvariantEmbedding>()
    fun toInvariantsList() = invariants.toList()

    fun addAccessToSharedPredicate() = invariants.addIfNotNull(
        type.sharedPredicateAccessInvariant()
    )

    fun addAccessToUniquePredicate() = invariants.addIfNotNull(
        type.uniquePredicateAccessInvariant()
    )

    fun includeSubTypeInvariants() = invariants.add(
        SubTypeInvariantEmbedding(type)
    )
}