/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.purity

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.SharingContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.WithPosition
import org.jetbrains.kotlin.formver.core.exhaustiveAll

/**
 * Positioning information for a node is stored in the closest parent `WithPosition` node.
 *
 * @return a preordered sequence of pairs, each containing the `ExpEmbedding` and its FIR source as a `KtSourceElement`
 */
fun ExpEmbedding.preorder(currentSource: KtSourceElement? = null): Sequence<Pair<ExpEmbedding, KtSourceElement?>> =
    sequence {
        val nextSource = (this@preorder as? WithPosition)?.source ?: currentSource

        if (this@preorder !is WithPosition) yield(Pair(this@preorder, nextSource))

        yieldAll(this@preorder.children().flatMap { it.preorder(currentSource = nextSource) })
    }

/**
 * Validates all nodes using the isValid function.
 * Avoids `all` to prevent short-circuiting, ensuring all errors are reported.
 */
fun ExpEmbedding.allNodesSatisfyPurityRequirements(source: KtSourceElement?, errorCollector: ErrorCollector): Boolean =
    preorder(source)
        .exhaustiveAll {
            val embedding = it.first
            val source = checkNotNull(it.second) {
                "Purity-check expected a KtSourceElement, but none was present"
            }
            embedding.satisfiesPurityRequirements(DefaultPurityContext(source, errorCollector))
        }

/**
 * Runs the purity check on a provided embedding
 */
fun ExpEmbedding.isPure(): Boolean = this.accept(ExprPurityVisitor)

/**
 * Returns the `KtSourceElement` of the outermost `WithPosition`
 * inside this expression; returns [fallback] if none is found.
 */
internal fun ExpEmbedding.expressionSource(fallback: KtSourceElement): KtSourceElement {
    var curr: ExpEmbedding = this@expressionSource
    while (true) {
        if (curr is WithPosition) return curr.source
        else if (curr is SharingContext) curr = curr.inner
        else break
    }
    return fallback
}