/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.isInvariantBuilderFunctionNamed

private const val INVALID_STATEMENT_MSG =
    "Every statement in invariant block must be a pure boolean invariant."

data class InvariantsAndTriggers(
    val invariants: List<ExpEmbedding>,
    val triggers: List<ExpEmbedding>
)

private fun FirBlock.isEmptyLambdaBody(): Boolean {
    if (statements.isEmpty()) return false
    return (statements.size == 1 && (statements.first() as? FirReturnExpression)?.result?.resolvedType?.isUnit ?: false)
}

fun StmtConversionContext.collectInvariants(block: FirBlock) = buildList {
    if (block.isEmptyLambdaBody()) {
        return@buildList
    }
    block.statements.forEach { stmt ->
        check(stmt is FirExpression && stmt.resolvedType.isBoolean) {
            INVALID_STATEMENT_MSG
        }
        add(stmt.accept(StmtConversionVisitor, this@collectInvariants))
    }
}

/**
 * Attempts to extract trigger expressions from a triggers() function call.
 * Returns the list of trigger expressions if this is a triggers() call, or null otherwise.
 */
private fun StmtConversionContext.tryExtractTriggers(stmt: FirStatement): List<ExpEmbedding>? {
    if (stmt !is FirFunctionCall) return null

    val symbol = stmt.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
    if (symbol?.isInvariantBuilderFunctionNamed("triggers") != true) return null

    val varargs = stmt.arguments.firstOrNull() as? FirVarargArgumentsExpression
        ?: throw IllegalArgumentException("triggers() function must have a single varargs parameter.")

    // TODO: check whether trigger is valid in Viper.
    return varargs.arguments.map { expr ->
        expr.accept(StmtConversionVisitor, this)
    }
}

fun StmtConversionContext.collectInvariantsAndTriggers(block: FirBlock): InvariantsAndTriggers {
    val invariants = mutableListOf<ExpEmbedding>()
    val triggers = mutableListOf<ExpEmbedding>()

    block.statements.forEach { stmt ->
        val extractedTriggers = tryExtractTriggers(stmt)
        if (extractedTriggers != null) {
            triggers.addAll(extractedTriggers)
            return@forEach
        }

        // Otherwise, treat as invariant
        check(stmt is FirExpression && stmt.resolvedType.isBoolean) {
            INVALID_STATEMENT_MSG
        }
        invariants.add(stmt.accept(StmtConversionVisitor, this))
    }

    return InvariantsAndTriggers(invariants, triggers)
}
