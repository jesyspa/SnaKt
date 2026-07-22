/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.names.SpecialPackages
import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.locality.plugin.locality
import org.jetbrains.kotlin.formver.uniqueness.plugin.Uniqueness
import org.jetbrains.kotlin.formver.uniqueness.plugin.scopeUniqueness
import org.jetbrains.kotlin.formver.viper.ast.Position
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// val FirElement.calleeSymbol: FirBasedSymbol<*>
//     get() = toReference()?.toResolvedBaseSymbol()!!
// val FirElement.calleeCallableSymbol: FirCallableSymbol<*>
//     get() = calleeReference?.toResolvedCallableSymbol()!!
@OptIn(SymbolInternals::class)
val FirPropertySymbol.isCustom: Boolean
    get() {
        val getter = getterSymbol?.fir
        val setter = setterSymbol?.fir
        return if (isVal) getter !is FirDefaultPropertyGetter
        else getter !is FirDefaultPropertyGetter || setter !is FirDefaultPropertySetter
    }

val FirFunctionCall.functionCallArguments: List<FirExpression>
    get() = listOfNotNull(dispatchReceiver, extensionReceiver) + argumentList.arguments

val FirFunctionSymbol<*>.effects: List<FirEffectDeclaration>
    get() = this.resolvedContractDescription?.effects ?: emptyList()
val KtSourceElement?.asPosition: Position
    get() = when (this) {
        null -> Position.NoPosition
        else -> Position.Wrapped(this)
    }
val FirBasedSymbol<*>.asSourceRole: SourceRole
    get() = SourceRole.FirSymbolHolder(this)

fun annotationId(name: String): ClassId =
    ClassId(FqName.fromSegments(SpecialPackages.formver), Name.identifier(name))

private fun callableId(packageName: List<String>, className: String?, name: String): CallableId =
    CallableId(
        FqName.fromSegments(packageName),
        className?.let { FqName.fromSegments(listOf(it)) },
        Name.identifier(name)
    )

fun formverCallableId(className: String?, name: String): CallableId =
    callableId(SpecialPackages.formver, className, name)

fun kotlinCallableId(className: String?, name: String): CallableId = callableId(SpecialPackages.kotlin, className, name)

val FirBasedSymbol<*>.isUnique: Boolean
    get() = when (this) {
        is FirReceiverParameterSymbol -> resolvedType.scopeUniqueness == Uniqueness.Unique
        is FirCallableSymbol<*> -> resolvedReturnType.scopeUniqueness == Uniqueness.Unique
        else -> false
    }

val FirBasedSymbol<*>.isBorrowed: Boolean
    get() = when (this) {
        is FirReceiverParameterSymbol -> resolvedType.locality == Locality.Local
        is FirCallableSymbol<*> -> resolvedReturnType.locality == Locality.Local
        else -> false
    }

/**
 * Returns `true` if [this] function represents one of the following specification functions: `preconditions`,
 * `postconditions`, `loopInvariants`, `verify`.
 */
context(context: CheckerContext)
private fun FirFunctionSymbol<*>.isSpecificationFunction(): Boolean =
    hasAnnotation(annotationId("SpecificationHelper"), context.session)

context(context: CheckerContext)
fun FirStatement.isSpecificationCall(): Boolean =
    ((this as? FirFunctionCall)
        ?.toResolvedCallableSymbol() as? FirFunctionSymbol<*>)
        ?.isSpecificationFunction() ?: false

fun FirBasedSymbol<*>.isPure(session: FirSession) = hasAnnotation(annotationId("Pure"), session)

fun FirBasedSymbol<*>.isManual(session: FirSession) = hasAnnotation(annotationId("Manual"), session)

fun FirFunctionSymbol<*>.neverConvert(session: FirSession) = hasAnnotation(annotationId("NeverConvert"), session)

fun FirFunctionSymbol<*>.isFormverFunctionNamed(name: String) =
    this is FirNamedFunctionSymbol && callableId == formverCallableId(className = null, name)

fun FirFunctionSymbol<*>.isInvariantBuilderFunctionNamed(name: String) =
    this is FirNamedFunctionSymbol && callableId == formverCallableId("InvariantBuilder", name)

@OptIn(SymbolInternals::class)
val FirFunctionSymbol<*>.shouldBeInlined
    get() = isInline && fir.body != null

private object ViperProgram : FirDeclarationDataKey()
private object ShouldVerify : FirDeclarationDataKey()

var FirSimpleFunction.viperProgram: viper.silver.ast.Program? by FirDeclarationDataRegistry.data(ViperProgram)
var FirSimpleFunction.shouldVerify: Boolean? by FirDeclarationDataRegistry.data(ShouldVerify)
