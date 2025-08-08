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
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.text

fun FirPropertyAccessExpression.calleeSymbol() =
    calleeReference.resolved?.resolvedSymbol ?: throw IllegalStateException("callee not resolved")

data class UniqueVisit(val computedLevel: UniqueLevel, val embedding: UniqueLevelEmbedding?)

object PartiallyMovedMarker : FirVisitor<Unit, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext) =
        throw IllegalStateException("PartiallyMovedVisitor should not be called on general FIR elements")

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: UniqueCheckerContext
    ) {
        propertyAccessExpression.explicitReceiver?.accept(this, data)
        data.markPartiallyMoved(
            LocalPath(
                propertyAccessExpression.accept(LocalVariableVisitor, data), propertyAccessExpression.calleeSymbol()
            )
        )
    }
}

object IsPartiallyMovedVisitor : FirVisitor<Boolean, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext) = false

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: UniqueCheckerContext
    ): Boolean {
        val local = propertyAccessExpression.accept(LocalVariableVisitor, data)
        return data.isPartiallyMoved(LocalPath(local, propertyAccessExpression.calleeSymbol()))
    }
}

object LocalVariableVisitor : FirVisitor<FirBasedSymbol<*>, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext) =
        throw IllegalStateException("LocalVariableVisitor should not be called on general FIR elements")

    override fun visitResolvedNamedReference(
        resolvedNamedReference: FirResolvedNamedReference, data: UniqueCheckerContext
    ): FirBasedSymbol<*> = resolvedNamedReference.resolvedSymbol

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: UniqueCheckerContext
    ): FirBasedSymbol<*> = propertyAccessExpression.explicitReceiver?.accept(this, data)
        ?: propertyAccessExpression.calleeReference.accept(this, data)
}

object UniqueCheckVisitor : FirVisitor<UniqueVisit, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext) =
        throw IllegalStateException("UniqueCheckVisitor should not be called on general FIR elements")

    override fun visitSimpleFunction(
        simpleFunction: FirSimpleFunction, data: UniqueCheckerContext
    ): UniqueVisit {
        simpleFunction.body?.accept(this, data)
        // Function definition doesn't have to return a unique level
        return UniqueVisit(UniqueLevel.Shared, null)
    }

    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression, data: UniqueCheckerContext
    ): UniqueVisit = UniqueVisit(UniqueLevel.Unique, null)

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: UniqueCheckerContext
    ): UniqueVisit {
        val path = LocalPath(
            propertyAccessExpression.accept(LocalVariableVisitor, data), propertyAccessExpression.calleeSymbol()
        )

        val embedding = data.uniqueLevelOf(path)

        val currentLevel = embedding.level
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
            require(argumentUnique != UniqueLevel.Top) {
                "attempting to access a non-accessible argument ${argument.source.text} in ${functionCall.source.text}"
            }

            require(!argument.accept(IsPartiallyMovedVisitor, data)) {
                "attempting to pass a partially moved argument ${argument.source.text} in ${functionCall.source.text}"
            }

            when (requiredUnique) {
                UniqueLevel.Unique -> {
                    require(argumentUnique == UniqueLevel.Unique) {
                        "uniqueness level not match ${argument.source.text}, required: Unique, actual: $argumentUnique"
                    }

                    embedding?.let {
                        it.level = UniqueLevel.Top
                        argument.accept(PartiallyMovedMarker, data)
                    }
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
