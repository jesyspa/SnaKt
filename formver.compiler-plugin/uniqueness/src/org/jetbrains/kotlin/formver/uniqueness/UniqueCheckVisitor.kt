/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.uniqueness.UniqueCheckerContext.Companion.mergeContextFrames
import org.jetbrains.kotlin.formver.uniqueness.UniqueCheckerContext.Companion.symbolFromExpression
import org.jetbrains.kotlin.text

object UniqueCheckVisitor : FirVisitor<UniqueLevel, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext): UniqueLevel = UniqueLevel.Shared

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: UniqueCheckerContext): UniqueLevel {
        data.pushUniquenessFrame()
        for (param in simpleFunction.valueParameters) {
            data.assignUniqueness(param.symbol, data.resolveUniquenessAnnotation(param))
        }

        simpleFunction.body?.accept(this, data)

        data.popUniquenessFrame()
        // Function definition doesn't have to return a unique level
        return UniqueLevel.Shared
    }

    override fun visitResolvedNamedReference(
        resolvedNamedReference: FirResolvedNamedReference, data: UniqueCheckerContext
    ): UniqueLevel {
        val symbol = resolvedNamedReference.resolvedSymbol
        return if (symbol is FirVariableSymbol<*>) {
            data.resolveUniqueness(symbol)
        } else {
            UniqueLevel.Shared
        }
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: UniqueCheckerContext
    ): UniqueLevel {
        return propertyAccessExpression.calleeReference.accept(this, data)
    }

    override fun visitProperty(property: FirProperty, data: UniqueCheckerContext): UniqueLevel {
        val initializer = property.initializer
        data.assignVariable(this, property.symbol, initializer)
        return UniqueLevel.Shared
    }

    override fun visitVariableAssignment(
        variableAssignment: FirVariableAssignment, data: UniqueCheckerContext,
    ): UniqueLevel {
        val lhs = symbolFromExpression(variableAssignment.lValue) ?: return UniqueLevel.Shared
        data.assignVariable(
            this,
            lhs,
            variableAssignment.rValue
        )
        return UniqueLevel.Shared
    }

    override fun visitBlock(block: FirBlock, data: UniqueCheckerContext): UniqueLevel {
        data.pushUniquenessFrame()
        block.statements.forEach { statement ->
            statement.accept(this, data)
        }
        data.popUniquenessFrame()

        // Don't support returning data from blocks yet
        return UniqueLevel.Shared
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: UniqueCheckerContext): UniqueLevel {
        whileLoop.condition.accept(this, data)

        var (currentContext, nextContext) = (data.copy() to data.copy())
        do {
            currentContext = nextContext.copy()

            whileLoop.block.accept(this, nextContext)
        } while (currentContext != nextContext)

        return UniqueLevel.Shared
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: UniqueCheckerContext): UniqueLevel {
        if (whenExpression.usedAsExpression) {
            throw IllegalArgumentException("when expression used as expression is not supported yet")
        }

        whenExpression.branches.forEach { it.condition.accept(this, data) }

        // We can't know which of the branches would run, so we need to merge their contexts
        val contexts = whenExpression.branches.map { data.runBlock(it.result, this) }
        data.resolvedUniquenessFrames = contexts.map { it.resolvedUniquenessFrames }.reduce(::mergeContextFrames)

        // Don't support expression-like when expressions for now
        return UniqueLevel.Shared
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: UniqueCheckerContext): UniqueLevel {
        // First, the try block runs
        tryExpression.tryBlock.accept(this, data)

        // We can't know which of the catch expressions would run, so we need to merge their contexts
        val contexts = tryExpression.catches.map { data.runBlock(it.block, this) }
        data.resolvedUniquenessFrames = contexts.map { it.resolvedUniquenessFrames }.reduce(::mergeContextFrames)

        // Finally block always runs, so we run through it using our merged context
        tryExpression.finallyBlock?.accept(this, data)
        return UniqueLevel.Shared
    }

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCall(functionCall: FirFunctionCall, data: UniqueCheckerContext): UniqueLevel {
        // To keep it simple, assume a functionCall always return Shared for now
        val symbol = functionCall.toResolvedCallableSymbol()
        val params = (symbol as FirFunctionSymbol<*>).fir.valueParameters
        val requiredUniqueLevels = params.map { data.resolveUniquenessAnnotation(it) }
        // Skip merge of context for now
        val arguments = functionCall.arguments
        arguments.forEachIndexed { index, argument ->
            val argumentUniqueness = visitExpression(argument, data)

            if (argumentUniqueness == UniqueLevel.Top) {
                throw IllegalArgumentException("attempting to pass an inaccessible property ${argument.source.text}")
            }

            val requiredUnique = requiredUniqueLevels[index]
            if (requiredUnique == UniqueLevel.Unique) {
                if (argumentUniqueness == UniqueLevel.Shared) {
                    throw IllegalArgumentException("uniqueness level not match ${argument.source.text}")
                }

                // Consume uniques
                symbolFromExpression(argument)?.let {
                    data.assignUniqueness(it, UniqueLevel.Top)
                }
            } else {
                symbolFromExpression(argument)?.let { data.assignUniqueness(it, UniqueLevel.Shared) }
            }
        }

        val callee = functionCall.toResolvedCallableSymbol()?.fir as FirSimpleFunction
        return data.resolveUniquenessAnnotation(callee)
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
