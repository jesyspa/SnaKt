/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp

/**
 * Owns the path-and-predicate computation for a [FieldAccess][org.jetbrains.kotlin.formver.core.embeddings.expression.FieldAccess]
 * on a class hierarchy.
 *
 * To reach a field declared on a superclass we must walk the chain of unique-predicates
 * from the receiver's runtime type down to the declaring class, and for each class on
 * that path emit one predicate-access expression. The three linearizers consume that
 * sequence in their preferred shape:
 *  - imperative linearization emits a `Stmt.Unfold` per access;
 *  - SSA linearization collects them as invariants on the assignment;
 *  - pure-expression linearization nests them as `Exp.Unfolding` around the body.
 */
class FieldAccessLowering(
    private val typeResolver: TypeResolver,
    private val source: KtSourceElement?,
) {
    fun pathTo(receiverType: TypeEmbedding, field: FieldEmbedding): Sequence<ClassTypeEmbedding> =
        typeResolver.hierarchyPathTo(receiverType.pretype, field)

    fun predicateAccessFor(receiverViper: Exp, classOnPath: ClassTypeEmbedding): Exp.PredicateAccess =
        Exp.PredicateAccess(
            classOnPath.uniquePredicateName,
            listOf(receiverViper),
            PermExp.FullPerm(),
            source.asPosition,
        )
}
