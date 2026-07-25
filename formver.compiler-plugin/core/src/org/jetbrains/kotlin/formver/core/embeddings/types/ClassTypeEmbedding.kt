/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.names.PredicateName
import org.jetbrains.kotlin.formver.core.names.ScopedName
import org.jetbrains.kotlin.formver.core.names.asScope
import org.jetbrains.kotlin.formver.viper.ast.DomainFunc
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate

// TODO: incorporate generic parameters.
data class ClassTypeEmbedding(override val name: ScopedName) : PretypeEmbedding {

    override val runtimeType: Exp = this.embedClassTypeFunc()()

    val uniquePredicateName = ScopedName(name.asScope(), PredicateName("unique"))

    context(ctx: TypeResolver)
    fun uniquePredicate(): Predicate = ClassPredicateBuilder.build(name, uniquePredicateName) {
        includeSubTypeInvariants()
        forEachPropertyField {
            forBackingField {
                if (!isAlwaysWriteable) {
                    addAccessPermissions(PermExp.FullPerm())

                    forType {
                        includeSubTypeInvariants()
                    }
                }
            }
            forType {
                if (isUnique) {
                    addAccessToUniquePredicate()
                }
            }
        }
        forEachSuperType {
            addAccessToUniquePredicate()
        }
    }

    override fun accessInvariants(ctx: TypeResolver): List<TypeInvariantEmbedding> =
        ctx.flatMapUniqueFields(name) { field ->
            field.accessInvariantsForParameter()
        }

    override fun uniquePredicateAccessInvariant(ctx: TypeResolver) =
        PredicateAccessTypeInvariantEmbedding(uniquePredicateName, PermExp.FullPerm())

}


fun ClassTypeEmbedding.embedClassTypeFunc(): DomainFunc = RuntimeTypeDomain.classTypeFunc(name)
