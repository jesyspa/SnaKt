/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.allReceiverExpressions
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.text
import kotlin.collections.plus

object UniqueCheckVisitor : FirVisitor<UniqueLevel, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext): UniqueLevel = UniqueLevel.Shared

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: UniqueCheckerContext): UniqueLevel {
        simpleFunction.body?.accept(this, data)
        // Function definition doesn't have to return a unique level
        return UniqueLevel.Shared
    }

    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression, data: UniqueCheckerContext
    ): UniqueLevel {
        return UniqueLevel.Unique
    }

    override fun visitResolvedNamedReference(
        resolvedNamedReference: FirResolvedNamedReference, data: UniqueCheckerContext
    ): UniqueLevel {
        return data.resolveUniqueAnnotation(resolvedNamedReference.resolvedSymbol)
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: UniqueCheckerContext
    ): UniqueLevel {
        val currentAnnotation = propertyAccessExpression.calleeReference.accept(this, data)
        val previousLevels = propertyAccessExpression.allReceiverExpressions.map {
            it.accept(this, data)
        }

        return (previousLevels + currentAnnotation).max()
    }

    override fun visitBlock(block: FirBlock, data: UniqueCheckerContext): UniqueLevel {
        block.statements.forEach { statement ->
            when (statement) {
                is FirFunctionCall -> {
                    statement.accept(this, data)
                }
            }
        }
        return UniqueLevel.Shared
    }

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCall(functionCall: FirFunctionCall, data: UniqueCheckerContext): UniqueLevel {
        // To keep is simple, assume a functionCall always return Shared for now
        val symbol = functionCall.toResolvedCallableSymbol()
        val params = (symbol as FirFunctionSymbol<*>).fir.valueParameters
        val requiredUniqueLevels = params.map { data.resolveUniqueAnnotation(it) }
        // Skip merge of context for now
        val arguments = functionCall.arguments
        arguments.forEachIndexed { index, argument ->
            val requiredUnique = requiredUniqueLevels[index]
            val argumentUnique = argument.accept(this, data)
            if (requiredUnique == UniqueLevel.Unique && argumentUnique == UniqueLevel.Shared) {
                throw IllegalArgumentException("uniqueness level not match ${argument.source.text}")
            }
        }

        val callee = functionCall.toResolvedCallableSymbol()?.fir as FirSimpleFunction
        return data.resolveUniqueAnnotation(callee)
    }
}

object UniquenessCheckExceptionWrapper : FirVisitor<UniqueLevel, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext): UniqueLevel {
        try {
            return element.accept(UniqueCheckVisitor, data)
        } catch (e: Exception) {
            data.errorCollector.addErrorInfo("... while checking uniqueness level for ${element.source.text}")
            throw e
        }
    }
}
