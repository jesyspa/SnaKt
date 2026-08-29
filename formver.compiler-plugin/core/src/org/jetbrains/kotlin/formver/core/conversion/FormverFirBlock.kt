/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.formver.core.isFormverFunctionNamed

enum class SpecBlockKind(val functionName: String) {
    PRECONDITIONS("preconditions"),
    POSTCONDITIONS("postconditions"),
    LOOP_INVARIANTS("loopInvariants"),
}

/** The specification block [this] calls, or `null` if it is not such a call. */
fun FirStatement.specBlockKind(): SpecBlockKind? {
    if (this !is FirFunctionCall) return null
    val symbol = toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return null
    return SpecBlockKind.entries.firstOrNull { symbol.isFormverFunctionNamed(it.functionName) }
}

private fun FirFunctionCall.specBlockLambda(): FirAnonymousFunction {
    val argument = argument
    if (argument !is FirAnonymousFunctionExpression)
        error("Only lambdas are allowed as arguments of ${specBlockKind()?.functionName}.")
    return argument.anonymousFunction
}

/** What a statement list is, which is what decides where specification blocks may sit in it. */
enum class SpecBlockPosition { FUNCTION_BODY, LOOP_BODY, NONE }

/**
 * The specification-block calls conversion picks up from [block].
 *
 * The single authority on where a specification block counts: extraction reads what this returns,
 * target selection asks whether it returns anything, and [findIgnoredSpecBlocks] treats everything
 * else as dropped. Resolves names only, so it is safe to ask about a body conversion may yet reject.
 */
fun usedSpecBlocks(block: FirBlock, position: SpecBlockPosition): Map<SpecBlockKind, FirFunctionCall> {
    fun statementAt(index: Int, kind: SpecBlockKind): FirFunctionCall? =
        (block.statements.getOrNull(index) as? FirFunctionCall)?.takeIf { it.specBlockKind() == kind }

    return when (position) {
        SpecBlockPosition.NONE -> emptyMap()
        SpecBlockPosition.LOOP_BODY -> buildMap {
            statementAt(0, SpecBlockKind.LOOP_INVARIANTS)?.let { put(SpecBlockKind.LOOP_INVARIANTS, it) }
        }
        SpecBlockPosition.FUNCTION_BODY -> buildMap {
            val precond = statementAt(0, SpecBlockKind.PRECONDITIONS)
            if (precond != null) put(SpecBlockKind.PRECONDITIONS, precond)
            val postcond = statementAt(if (precond != null) 1 else 0, SpecBlockKind.POSTCONDITIONS)
            if (postcond != null) put(SpecBlockKind.POSTCONDITIONS, postcond)
        }
    }
}

/**
 * Every specification-block call inside [body] that conversion will not pick up, in source order.
 *
 * Conversion resolves these calls by name wherever they appear and no-ops them, so a block outside
 * the position [usedSpecBlocks] reads is dropped without a word. Nested statement lists count: a
 * block in an `if` branch is ignored just as surely as one further down the body. Lambdas and nested
 * functions are skipped, having their own specification blocks in their own right.
 */
fun findIgnoredSpecBlocks(body: FirBlock): List<Pair<SpecBlockKind, FirFunctionCall>> =
    IgnoredSpecBlockCollector().run {
        visitStatements(body, SpecBlockPosition.FUNCTION_BODY)
        ignored
    }

private class IgnoredSpecBlockCollector : FirVisitorVoid() {
    val ignored = mutableListOf<Pair<SpecBlockKind, FirFunctionCall>>()
    private val used = mutableListOf<FirFunctionCall>()

    fun visitStatements(block: FirBlock, position: SpecBlockPosition) {
        used.addAll(usedSpecBlocks(block, position).values)
        block.statements.forEach { it.accept(this) }
    }

    override fun visitBlock(block: FirBlock) = visitStatements(block, SpecBlockPosition.NONE)

    override fun visitWhileLoop(whileLoop: FirWhileLoop) {
        whileLoop.condition.accept(this)
        visitStatements(whileLoop.block, SpecBlockPosition.LOOP_BODY)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        val kind = functionCall.specBlockKind()
        if (kind == null) {
            visitElement(functionCall)
            return
        }
        if (used.none { it === functionCall }) ignored.add(kind to functionCall)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) = Unit

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) = Unit

    override fun visitElement(element: FirElement) = element.acceptChildren(this)
}

fun extractLoopInvariants(parentBlock: FirBlock): FirBlock? =
    usedSpecBlocks(parentBlock, SpecBlockPosition.LOOP_BODY)[SpecBlockKind.LOOP_INVARIANTS]?.specBlockLambda()?.body

data class FirSpecification(val precond: FirBlock?, val postcond: FirBlock?, val returnVar: FirValueParameterSymbol?) {
    constructor() : this(null, null, null)
}

private fun FirAnonymousFunction.extractFormverReturnVar(returnType: ConeKotlinType): FirValueParameterSymbol {
    val param = valueParameters.first()
    if (param.symbol.resolvedReturnType != returnType)
        error("Expected type ${returnType} based on signature, got ${param.symbol.resolvedReturnType}")
    return param.symbol
}

/** Whether callers of a function with body [parentBlock] may assume a SnaKt specification of it. */
fun hasFirSpecification(parentBlock: FirBlock): Boolean =
    usedSpecBlocks(parentBlock, SpecBlockPosition.FUNCTION_BODY).isNotEmpty()

fun extractFirSpecification(parentBlock: FirBlock, returnType: ConeKotlinType): FirSpecification {
    val blocks = usedSpecBlocks(parentBlock, SpecBlockPosition.FUNCTION_BODY)
    val precond = blocks[SpecBlockKind.PRECONDITIONS]?.specBlockLambda()
    val postcond = blocks[SpecBlockKind.POSTCONDITIONS]?.specBlockLambda()
    return FirSpecification(precond?.body, postcond?.body, postcond?.extractFormverReturnVar(returnType))
}
