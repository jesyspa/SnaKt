/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

/**
 * The unique-predicate access on [classOnPath] that must be unfolded to reach a field on a superclass
 * through [receiver]. Takes a raw [Exp] rather than going through `fillHole`/`pureToViper`.
 */
fun hierarchyPredicateAccess(
    receiver: Exp,
    classOnPath: ClassTypeEmbedding,
    source: KtSourceElement?,
): Exp.PredicateAccess =
    Exp.PredicateAccess(
        classOnPath.uniquePredicateName,
        listOf(receiver),
        PermExp.FullPerm(),
        source.asPosition,
    )

/**
 * The unique-predicate accesses to unfold to reach [field] through [receiver], in walk order
 * (outermost class first). Returned ordered top-down: callers that need bottom-up nesting
 * (e.g. [Exp.Unfolding] fold) reverse via `foldRight` or `toList().asReversed()`.
 */
fun LinearizationContext.hierarchyPredicateAccesses(
    receiver: Exp,
    receiverType: TypeEmbedding,
    field: FieldEmbedding,
): Sequence<Exp.PredicateAccess> =
    typeResolver.hierarchyPathTo(receiverType.pretype, field)
        .map { hierarchyPredicateAccess(receiver, it, source) }

/**
 * Emits a `Stmt.Unfold` for each unique-predicate on the hierarchy path from [receiverType]
 * down to the class declaring [field]. Shared by the imperative linearizer and the
 * `FieldModification` visitor; both want the same imperative unfold shape.
 */
fun LinearizationContext.unfoldHierarchyPredicates(receiver: Exp, receiverType: TypeEmbedding, field: FieldEmbedding) {
    for (predicateAccess in hierarchyPredicateAccesses(receiver, receiverType, field)) {
        addStatement { Stmt.Unfold(predicateAccess, source.asPosition) }
    }
}
