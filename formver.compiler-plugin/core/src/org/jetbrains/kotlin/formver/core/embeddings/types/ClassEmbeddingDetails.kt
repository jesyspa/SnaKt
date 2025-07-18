/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.names.PredicateKotlinName
import org.jetbrains.kotlin.formver.core.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.core.names.SimpleKotlinName
import org.jetbrains.kotlin.formver.core.names.asScope
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

class ClassEmbeddingDetails(
    val type: ClassTypeEmbedding,
    val isInterface: Boolean,
) : TypeInvariantHolder {
    private var _superTypes: List<PretypeEmbedding>? = null
    val superTypes: List<PretypeEmbedding>
        get() = _superTypes ?: error("Super types of ${type.name} have not been initialised yet.")

    private val classSuperTypes: List<ClassTypeEmbedding>
        get() = superTypes.filterIsInstance<ClassTypeEmbedding>()

    fun initSuperTypes(newSuperTypes: List<PretypeEmbedding>) {
        check(_superTypes == null) { "Super types of ${type.name} are already initialised." }
        _superTypes = newSuperTypes
    }

    private var _fields: Map<SimpleKotlinName, FieldEmbedding>? = null
    private var _sharedPredicate: Predicate? = null
    private var _uniquePredicate: Predicate? = null
    val fields: Map<SimpleKotlinName, FieldEmbedding>
        get() = _fields ?: error("Fields of ${type.name} have not been initialised yet.")
    val sharedPredicate: Predicate
        get() = _sharedPredicate ?: error("Predicate of ${type.name} has not been initialised yet.")
    val uniquePredicate: Predicate
        get() = _uniquePredicate ?: error("Unique Predicate of ${type.name} has not been initialised yet.")

    fun initFields(newFields: Map<SimpleKotlinName, FieldEmbedding>) {
        check(_fields == null) { "Fields of ${type.name} are already initialised." }
        _fields = newFields
        _sharedPredicate = ClassPredicateBuilder.build(this, sharedPredicateName) {
            forEachField {
                if (isAlwaysReadable) {
                    addAccessPermissions(PermExp.WildcardPerm())
                    forType {
                        addAccessToSharedPredicate()
                        includeSubTypeInvariants()
                    }
                }
            }
            forEachSuperType {
                addAccessToSharedPredicate()
            }
        }
        _uniquePredicate = ClassPredicateBuilder.build(this, uniquePredicateName) {
            forEachField {
                if (isAlwaysReadable) {
                    addAccessPermissions(PermExp.WildcardPerm())
                } else {
                    addAccessPermissions(PermExp.FullPerm())
                }
                forType {
                    addAccessToSharedPredicate()
                    if (isUnique) {
                        addAccessToUniquePredicate()
                    }
                    includeSubTypeInvariants()
                }
            }
            forEachSuperType {
                addAccessToUniquePredicate()
            }
        }
    }

    private val sharedPredicateName = ScopedKotlinName(type.name.asScope(), PredicateKotlinName("shared"))
    private val uniquePredicateName = ScopedKotlinName(type.name.asScope(), PredicateKotlinName("unique"))

    /**
     * Find an embedding of a backing field by this name amongst the ancestors of this type.
     *
     * While in Kotlin only classes can have backing fields, and so searching interface supertypes is not strictly necessary,
     * due to the way we handle list size we need to search all types.
     */
    fun findField(name: SimpleKotlinName): FieldEmbedding? = fields[name]

    fun <R> flatMapFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> =
        classSuperTypes.flatMap { it.details.flatMapFields(action) } + fields.flatMap { (name, field) ->
            action(
                name,
                field
            )
        }

    // We can't easily implement this by recursion on the supertype structure since some supertypes may be seen multiple times.
    // TODO: figure out a nicer way to handle this.
    override fun accessInvariants(): List<TypeInvariantEmbedding> =
        flatMapUniqueFields { _, field -> field.accessInvariantsForParameter() }

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun sharedPredicateAccessInvariant() =
        PredicateAccessTypeInvariantEmbedding(sharedPredicateName, PermExp.WildcardPerm())

    override fun uniquePredicateAccessInvariant() =
        PredicateAccessTypeInvariantEmbedding(uniquePredicateName, PermExp.FullPerm())

    override fun subTypeInvariant(): TypeInvariantEmbedding = type.subTypeInvariant()

    // Returns the sequence of classes in a hierarchy that need to be unfolded in order to access the given field
    fun hierarchyUnfoldPath(field: FieldEmbedding): Sequence<ClassTypeEmbedding> = sequence {
        val className = field.containingClass?.name
        require(className != null) { "Cannot find hierarchy unfold path of a field with no class information" }
        if (className == type.name) {
            yield(this@ClassEmbeddingDetails.type)
        } else {
            val sup = classSuperTypes.firstOrNull { !it.details.isInterface }
                ?: throw IllegalArgumentException("Reached top of the hierarchy without finding the field")

            yield(this@ClassEmbeddingDetails.type)
            yieldAll(sup.details.hierarchyUnfoldPath(field))
        }
    }

    fun <R> flatMapUniqueFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> {
        val seenFields = mutableSetOf<SimpleKotlinName>()
        return flatMapFields { name, field ->
            seenFields.add(name).ifTrue {
                action(name, field)
            } ?: listOf()
        }
    }
}
