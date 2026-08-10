/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.core.isFormverFunctionNamed

fun FirStatement.extractFormverFirBlock(predicate: FirFunctionSymbol<*>.() -> Boolean): FirAnonymousFunction? {
    if (this !is FirFunctionCall) return null
    val firFunction = toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return null
    if (!predicate(firFunction)) return null
    val formverInvariantsArgument = argument
    if (formverInvariantsArgument !is FirAnonymousFunctionExpression)
        error("Only lambdas are allowed as arguments of ${firFunction.name}.")
    return formverInvariantsArgument.anonymousFunction
}

private fun FirStatement.isFormverCallNamed(name: String): Boolean {
    if (this !is FirFunctionCall) return false
    val firFunction = toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return false
    return firFunction.isFormverFunctionNamed(name)
}

/**
 * Conversion only ever looks for [name] at [usedIndex] (or nowhere, if [usedIndex] is -1); any other
 * occurrence in [statements] is silently no-op'd by the special-function machinery instead of being
 * picked up as part of the specification. Report each such occurrence via [report] so it isn't silently
 * ignored.
 */
private fun reportMisplacedSpecBlocks(
    statements: List<FirStatement>,
    name: String,
    usedIndex: Int,
    report: (String, KtSourceElement?) -> Unit,
) {
    for ((index, stmt) in statements.withIndex()) {
        if (index != usedIndex && stmt.isFormverCallNamed(name)) report(name, stmt.source)
    }
}

fun extractLoopInvariants(parentBlock: FirBlock, reportMisplaced: (KtSourceElement?) -> Unit): FirBlock? {
    val statements = parentBlock.statements
    val invariants = statements.firstOrNull()?.extractFormverFirBlock { isFormverFunctionNamed("loopInvariants") }
    reportMisplacedSpecBlocks(statements, "loopInvariants", usedIndex = if (invariants != null) 0 else -1) { _, source ->
        reportMisplaced(source)
    }
    return invariants?.body
}

data class FirSpecification(val precond: FirBlock?, val postcond: FirBlock?, val returnVar: FirValueParameterSymbol?) {
    constructor() : this(null, null, null)
}

private fun FirAnonymousFunction.extractFormverReturnVar(returnType: ConeKotlinType): FirValueParameterSymbol {
    val param = valueParameters.first()
    if (param.symbol.resolvedReturnType != returnType)
        error("Expected type ${returnType} based on signature, got ${param.symbol.resolvedReturnType}")
    return param.symbol
}

/**
 * Picks out the `preconditions{}`/`postconditions{}` blocks that head [parentBlock]: `preconditions` must
 * be the first statement, and `postconditions` must be the first statement (if there is no `preconditions`)
 * or the second (right after `preconditions`). Any occurrence outside that position is invisible to the
 * rest of conversion, which no-ops such calls wherever they appear rather than erroring — so [reportMisplaced]
 * is called for each one instead of letting it disappear silently.
 */
fun extractFirSpecification(
    parentBlock: FirBlock,
    returnType: ConeKotlinType,
    reportMisplaced: (String, KtSourceElement?) -> Unit,
): FirSpecification {
    val statements = parentBlock.statements
    val firstStmt = statements.firstOrNull() ?: return FirSpecification()

    val leadingPostcond = firstStmt.extractFormverFirBlock { isFormverFunctionNamed("postconditions") }
    if (leadingPostcond != null) {
        reportMisplacedSpecBlocks(statements, "preconditions", usedIndex = -1, reportMisplaced)
        reportMisplacedSpecBlocks(statements, "postconditions", usedIndex = 0, reportMisplaced)
        return FirSpecification(null, leadingPostcond.body, leadingPostcond.extractFormverReturnVar(returnType))
    }

    val precond = firstStmt.extractFormverFirBlock { isFormverFunctionNamed("preconditions") }
    if (precond == null) {
        reportMisplacedSpecBlocks(statements, "preconditions", usedIndex = -1, reportMisplaced)
        reportMisplacedSpecBlocks(statements, "postconditions", usedIndex = -1, reportMisplaced)
        return FirSpecification()
    }
    reportMisplacedSpecBlocks(statements, "preconditions", usedIndex = 0, reportMisplaced)

    val postcond = statements.getOrNull(1)?.extractFormverFirBlock { isFormverFunctionNamed("postconditions") }
    reportMisplacedSpecBlocks(statements, "postconditions", usedIndex = if (postcond != null) 1 else -1, reportMisplaced)
    return FirSpecification(precond.body, postcond?.body, postcond?.extractFormverReturnVar(returnType))
}
