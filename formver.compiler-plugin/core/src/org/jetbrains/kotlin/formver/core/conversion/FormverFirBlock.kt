/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.withAttributes
import org.jetbrains.kotlin.formver.core.isFormverFunctionNamed
import org.jetbrains.kotlin.formver.locality.plugin.LocalityAttribute
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessAttribute

fun FirStatement.extractFormverFirBlock(predicate: FirFunctionSymbol<*>.() -> Boolean): FirAnonymousFunction? {
    if (this !is FirFunctionCall) return null
    val firFunction = toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return null
    if (!predicate(firFunction)) return null
    val formverInvariantsArgument = argument
    if (formverInvariantsArgument !is FirAnonymousFunctionExpression)
        error("Only lambdas are allowed as arguments of ${firFunction.name}.")
    return formverInvariantsArgument.anonymousFunction
}

fun extractLoopInvariants(parentBlock: FirBlock): FirBlock? {
    val firstStmt = parentBlock.statements.firstOrNull() ?: return null
    return firstStmt.extractFormverFirBlock { isFormverFunctionNamed("loopInvariants") }?.body
}

data class FirSpecification(val precond: FirBlock?, val postcond: FirBlock?, val returnVar: FirValueParameterSymbol?) {
    constructor() : this(null, null, null)
}

/**
 * Drops the attributes that a specification binder inherits rather than restates.
 *
 * `@Unique` and `@Borrowed` are rejected on a type argument, so a `postconditions` binder cannot spell out the
 * uniqueness and locality of the value it stands for; it takes them from the signature instead.
 */
private fun ConeKotlinType.withoutInheritedAttributes(): ConeKotlinType =
    withAttributes(attributes.remove(UniquenessAttribute).remove(LocalityAttribute))

private fun FirAnonymousFunction.extractFormverReturnVar(returnType: ConeKotlinType): FirValueParameterSymbol {
    val param = valueParameters.first()
    val declaredType = param.symbol.resolvedReturnType
    if (declaredType.withoutInheritedAttributes() != returnType.withoutInheritedAttributes())
        error("Expected type ${returnType} based on signature, got ${declaredType}")
    return param.symbol
}

/**
 * The lambda of the `postconditions` block of [this] body, if it has one.
 */
private fun FirBlock.extractPostconditionsLambda(): FirAnonymousFunction? {
    val firstStmt = statements.firstOrNull() ?: return null

    firstStmt.extractFormverFirBlock { isFormverFunctionNamed("postconditions") }?.let { return it }
    firstStmt.extractFormverFirBlock { isFormverFunctionNamed("preconditions") } ?: return null

    return statements.getOrNull(1)?.extractFormverFirBlock { isFormverFunctionNamed("postconditions") }
}

/**
 * The parameter that the `postconditions` block of [this] function binds its return value to, if it has one.
 */
@OptIn(SymbolInternals::class)
fun FirFunctionSymbol<*>.extractPostconditionsReturnVar(): FirValueParameterSymbol? {
    val body = (fir as? FirSimpleFunction)?.body ?: return null
    return body.extractPostconditionsLambda()?.valueParameters?.firstOrNull()?.symbol
}

fun extractFirSpecification(parentBlock: FirBlock, returnType: ConeKotlinType): FirSpecification {
    val firstStmt = parentBlock.statements.firstOrNull() ?: return FirSpecification()

    val precond = firstStmt.extractFormverFirBlock { isFormverFunctionNamed("preconditions") }
    val postcond = parentBlock.extractPostconditionsLambda()
    if (precond == null && postcond == null) return FirSpecification()

    return FirSpecification(precond?.body, postcond?.body, postcond?.extractFormverReturnVar(returnType))
}
