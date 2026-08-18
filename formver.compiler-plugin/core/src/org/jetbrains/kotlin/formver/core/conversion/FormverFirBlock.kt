/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.core.isFormverFunctionNamed

fun FirStatement.extractFormverFirBlock(
    ctx: ProgramConversionContext,
    predicate: FirFunctionSymbol<*>.() -> Boolean,
): FirAnonymousFunction? {
    if (this !is FirFunctionCall) return null
    val firFunction = toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return null
    if (!predicate(firFunction)) return null
    val formverInvariantsArgument = argument
    if (formverInvariantsArgument !is FirAnonymousFunctionExpression) {
        return ctx.handleUnsupportedFeature(
            source, "Only lambdas are allowed as arguments of ${firFunction.name}."
        ) { null }
    }
    return formverInvariantsArgument.anonymousFunction
}

fun extractLoopInvariants(parentBlock: FirBlock, ctx: ProgramConversionContext): FirBlock? {
    val firstStmt = parentBlock.statements.firstOrNull() ?: return null
    return firstStmt.extractFormverFirBlock(ctx) { isFormverFunctionNamed("loopInvariants") }?.body
}

data class FirSpecification(val precond: FirBlock?, val postcond: FirBlock?, val returnVar: FirValueParameterSymbol?) {
    constructor() : this(null, null, null)
}

private fun FirAnonymousFunction.extractFormverReturnVar(
    returnType: ConeKotlinType,
    ctx: ProgramConversionContext,
): FirValueParameterSymbol? {
    val param = valueParameters.first()
    if (param.symbol.resolvedReturnType != returnType) {
        return ctx.handleUnsupportedFeature(
            source, "Expected type ${returnType} based on signature, got ${param.symbol.resolvedReturnType}"
        ) { null }
    }
    return param.symbol
}

fun extractFirSpecification(parentBlock: FirBlock, returnType: ConeKotlinType, ctx: ProgramConversionContext): FirSpecification {
    val firstStmt = parentBlock.statements.firstOrNull() ?: return FirSpecification()

    firstStmt.extractFormverFirBlock(ctx) { isFormverFunctionNamed("postconditions") }?.let { lambda ->
        return FirSpecification(null, lambda.body, lambda.extractFormverReturnVar(returnType, ctx))
    }

    val precond = firstStmt.extractFormverFirBlock(ctx) { isFormverFunctionNamed("preconditions") }
        ?: return FirSpecification()
    val postcond =
        parentBlock.statements.getOrNull(1)?.extractFormverFirBlock(ctx) { isFormverFunctionNamed("postconditions") }
    return FirSpecification(precond.body, postcond?.body, postcond?.extractFormverReturnVar(returnType, ctx))
}
