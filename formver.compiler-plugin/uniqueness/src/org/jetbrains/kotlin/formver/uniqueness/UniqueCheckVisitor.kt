/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object PathVisitor : FirVisitor<List<FirBasedSymbol<*>>, Unit>() {
    override fun visitElement(element: FirElement, data: Unit) =
        throw IllegalStateException("LocalVariableVisitor should not be called on general FIR elements")

    override fun visitResolvedNamedReference(
        resolvedNamedReference: FirResolvedNamedReference, data: Unit
    ): List<FirBasedSymbol<*>> = listOf(resolvedNamedReference.resolvedSymbol)

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: Unit
    ): List<FirBasedSymbol<*>> {
        val parent = propertyAccessExpression.explicitReceiver?.accept(this, data) ?: emptyList()
        return parent + propertyAccessExpression.calleeReference.accept(this, data)
    }
}

/**
 * Resolve the path of a property access expression.
 *
 * For example, `a.b.c.d` will be resolved to `local/a`, `A/b`, `B/c`, `C/d`.
 */
fun FirPropertyAccessExpression.resolvePath(): List<FirBasedSymbol<*>> = accept(PathVisitor, Unit)

object UniqueCheckVisitor : FirVisitor<Pair<UniqueLevel, UniquePathContext?>, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext) =
        throw IllegalStateException("UniqueCheckVisitor should not be called on general FIR elements")

    override fun visitSimpleFunction(
        simpleFunction: FirSimpleFunction, data: UniqueCheckerContext
    ): Pair<UniqueLevel, UniquePathContext?> {
        simpleFunction.body?.accept(this, data)
        // Function definition doesn't have to return a unique level
        return Pair(UniqueLevel.Shared, null)
    }

    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression, data: UniqueCheckerContext
    ): Pair<UniqueLevel, UniquePathContext?> = Pair(UniqueLevel.Unique, null)

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: UniqueCheckerContext
    ): Pair<UniqueLevel, UniquePathContext?> {
        val path = propertyAccessExpression.resolvePath()
        val last = data.getOrPutPath(path)

        return Pair(last.pathToRootLUB, last)
    }

    override fun visitBlock(block: FirBlock, data: UniqueCheckerContext): Pair<UniqueLevel, UniquePathContext?> {
        block.statements.forEach { statement ->
            when (statement) {
                is FirFunctionCall -> {
                    statement.accept(this, data)
                }
            }
        }
        return Pair(UniqueLevel.Shared, null)
    }

    private fun verifyPassingRules(
        functionCall: FirFunctionCall,
        argument: FirExpression,
        requirements: Pair<UniqueLevel, BorrowingLevel>,
        actual: Pair<UniqueLevel, BorrowingLevel>,
        pathContext: UniquePathContext?,
        data: UniqueCheckerContext,
    ) {
        val (requiredUnique, requiredBorrowing) = requirements
        val (argumentUnique, argumentBorrowing) = actual

        if (argumentUnique == UniqueLevel.Top) {
            throw UniquenessCheckException(
                argument.source,
                "cannot access expression as its uniqueness state is top"
            )
        }

        val argumentSubtreeLUB = pathContext?.subtreeLUB
        if (argumentSubtreeLUB == UniqueLevel.Top) {
            throw UniquenessCheckException(
                argument.source,
                "a partially moved object cannot be passed as an argument"
            )
        }

        if (argumentBorrowing > requiredBorrowing) {
            throw UniquenessCheckException(
                argument.source,
                "cannot pass borrowed value as non-borrowed"
            )
        }

        if (requiredUnique == UniqueLevel.Unique) {
            if (argumentUnique != UniqueLevel.Unique) {
                throw UniquenessCheckException(
                    argument.source,
                    "expected unique value, got ${argumentUnique.toString().lowercase()}"
                )
            }

            val subtreeChanged = with(data) { pathContext?.hasChanges ?: false }
            if (subtreeChanged) {
                throw UniquenessCheckException(
                    argument.source,
                    "cannot pass a partially shared argument"
                )
            }
        }
    }

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCall(
        functionCall: FirFunctionCall, data: UniqueCheckerContext
    ): Pair<UniqueLevel, UniquePathContext?> {
        // To keep it simple, assume a functionCall always return Shared for now
        val symbol = functionCall.toResolvedCallableSymbol()
        val params = (symbol as FirFunctionSymbol<*>).fir.valueParameters
        val requiredUniqueLevels = params.map { data.resolveUniqueAnnotation(it) }
        val paramBorrowingLevels = params.map { data.resolveBorrowingAnnotation(it) }

        val arguments = functionCall.arguments
        arguments.forEachIndexed { index, argument ->
            val requiredUnique = requiredUniqueLevels[index]
            val paramBorrowing = paramBorrowingLevels[index]

            val (argumentUnique, pathContext) = argument.accept(this, data)
            val argumentBorrowing = with(data) { pathContext?.borrowingLevel ?: BorrowingLevel.Plain }
            verifyPassingRules(
                functionCall,
                argument,
                requirements = Pair(requiredUnique, paramBorrowing),
                actual = Pair(argumentUnique, argumentBorrowing),
                pathContext,
                data,
            )

            if (paramBorrowing != BorrowingLevel.Borrowed) {
                when (requiredUnique) {
                    UniqueLevel.Unique -> pathContext?.level = UniqueLevel.Top
                    UniqueLevel.Shared -> pathContext?.level = UniqueLevel.Shared
                    else -> throw IllegalStateException("argument can't request unique level $requiredUnique")
                }
            }
        }

        val callee = functionCall.toResolvedCallableSymbol()?.fir as FirSimpleFunction
        return Pair(data.resolveUniqueAnnotation(callee), null)
    }
}

