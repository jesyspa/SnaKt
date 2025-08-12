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
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.text

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
        simpleFunction: FirSimpleFunction,
        data: UniqueCheckerContext
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

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCall(functionCall: FirFunctionCall, data: UniqueCheckerContext): Pair<UniqueLevel, UniquePathContext?> {
        // To keep it simple, assume a functionCall always return Shared for now
        val symbol = functionCall.toResolvedCallableSymbol()
        val params = (symbol as FirFunctionSymbol<*>).fir.valueParameters
        val requiredUniqueLevels = params.map { data.resolveUniqueAnnotation(it) }
        val paramBorrowingLevels = params.map { data.resolveBorrowingAnnotation(it) }

        val arguments = functionCall.arguments
        arguments.forEachIndexed { index, argument ->
            val requiredUnique = requiredUniqueLevels[index]
            val paramBorrowing = paramBorrowingLevels[index]
            val (argumentUnique, trie) = argument.accept(this, data)
            val argumentBorrowing =
                trie?.localVariable?.let { data.resolveBorrowingAnnotation(it) } ?: BorrowingLevel.Owned

            require(argumentUnique != UniqueLevel.Top) {
                "attempting to access a non-accessible argument ${argument.source.text} in ${functionCall.source.text}"
            }

            val argumentSubtreeLUB = trie?.subtreeLUB
            require(argumentSubtreeLUB != UniqueLevel.Top) {
                "attempting to pass a partially moved argument ${argument.source.text} in ${functionCall.source.text}"
            }

            require(argumentBorrowing <= paramBorrowing) {
                "attempting to pass argument ${argument.source.text} in ${functionCall.source.text} with incompatible borrowing level"
            }

            when (requiredUnique) {
                UniqueLevel.Unique -> {
                    require(argumentUnique == UniqueLevel.Unique) {
                        "uniqueness level not match ${argument.source.text}, required: Unique, actual: $argumentUnique"
                    }

                    val subtreeChanged = with(data) { trie?.hasChanges ?: false }
                    require(!subtreeChanged) {
                        "attempting to pass a partially shared argument ${argument.source.text} in ${functionCall.source.text}"
                    }

                    if (paramBorrowing != BorrowingLevel.Borrowed) trie?.level = UniqueLevel.Top
                }

                UniqueLevel.Shared -> if (paramBorrowing != BorrowingLevel.Borrowed) trie?.level = UniqueLevel.Shared
                else -> {
                    throw IllegalStateException("argument can't request unique level $requiredUnique")
                }
            }
        }

        val callee = functionCall.toResolvedCallableSymbol()?.fir as FirSimpleFunction
        return Pair(data.resolveUniqueAnnotation(callee), null)
    }
}

object UniquenessCheckExceptionWrapper : FirVisitor<Pair<UniqueLevel, UniquePathContext?>, UniqueCheckerContext>() {
    override fun visitElement(element: FirElement, data: UniqueCheckerContext): Pair<UniqueLevel, UniquePathContext?> {
        try {
            return element.accept(UniqueCheckVisitor, data)
        } catch (e: Exception) {
            data.errorCollector.addErrorInfo("... while checking uniqueness level for ${element.source.text}")
            throw e
        }
    }
}
