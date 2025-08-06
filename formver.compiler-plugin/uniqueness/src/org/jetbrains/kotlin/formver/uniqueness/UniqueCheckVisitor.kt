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
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.text

data class UniqueVisit(val computedLevel: UniqueLevel, val embedding: UniqueLevelEmbedding?)

object UniqueCheckVisitor : FirVisitor<UniqueVisit, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext) =
        UniqueVisit(UniqueLevel.Shared, null)

    override fun visitSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: UniqueCheckerContext
    ): UniqueVisit {
        simpleFunction.body?.accept(this, data)
        // Function definition doesn't have to return a unique level
        return UniqueVisit(UniqueLevel.Shared, null)
    }

    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression, data: UniqueCheckerContext
    ): UniqueVisit = UniqueVisit(UniqueLevel.Unique, null)

    override fun visitResolvedNamedReference(
        resolvedNamedReference: FirResolvedNamedReference, data: UniqueCheckerContext
    ): UniqueVisit {
        val embedding = data.uniqueLevelOf(resolvedNamedReference.resolvedSymbol)
        return UniqueVisit(embedding.level, embedding)
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: UniqueCheckerContext
    ): UniqueVisit {
        val (currentLevel, embedding) = propertyAccessExpression.calleeReference.accept(this, data)
        val previousLevel = propertyAccessExpression.explicitReceiver?.accept(this, data)?.computedLevel

        val resultingLevel = listOfNotNull(currentLevel, previousLevel).max()

        return UniqueVisit(resultingLevel, embedding)
    }

    override fun visitBlock(block: FirBlock, data: UniqueCheckerContext): UniqueVisit {
        block.statements.forEach { statement ->
            when (statement) {
                is FirFunctionCall -> {
                    statement.accept(this, data)
                }
            }
        }
        return UniqueVisit(UniqueLevel.Shared, null)
    }

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCall(functionCall: FirFunctionCall, data: UniqueCheckerContext): UniqueVisit {
        // To keep it simple, assume a functionCall always return Shared for now
        val symbol = functionCall.toResolvedCallableSymbol()
        val params = (symbol as FirFunctionSymbol<*>).fir.valueParameters
        val requiredUniqueLevels = params.map { data.resolveUniqueAnnotation(it) }
        // Skip merge of context for now
        val arguments = functionCall.arguments
        arguments.forEachIndexed { index, argument ->
            val requiredUnique = requiredUniqueLevels[index]
            val (argumentUnique, embedding) = argument.accept(this, data)
            require (argumentUnique != UniqueLevel.Top) {
                "attempting to access a non-accessible argument in ${argument.source.text}"
            }

            when (requiredUnique) {
                UniqueLevel.Unique -> {
                    require(argumentUnique == UniqueLevel.Unique) {
                        "uniqueness level not match ${argument.source.text}, required: Unique, actual: $argumentUnique"
                    }

                    embedding?.level = UniqueLevel.Top
                }
                UniqueLevel.Shared -> embedding?.level = UniqueLevel.Shared
                else -> {
                    throw IllegalStateException("argument can't request unique level $requiredUnique")
                }
            }
        }

        val callee = functionCall.toResolvedCallableSymbol()?.fir as FirSimpleFunction
        return UniqueVisit(data.resolveUniqueAnnotation(callee), null)
    }
}

object UniquenessCheckExceptionWrapper : FirVisitor<UniqueVisit, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext): UniqueVisit {
        try {
            return element.accept(UniqueCheckVisitor, data)
        } catch (e: Exception) {
            data.errorCollector.addErrorInfo("... while checking uniqueness level for ${element.source.text}")
            throw e
        }
    }
}
