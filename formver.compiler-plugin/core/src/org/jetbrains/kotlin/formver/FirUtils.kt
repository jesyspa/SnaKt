/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.formver.embeddings.SourceRole
import org.jetbrains.kotlin.formver.names.SpecialPackages
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

fun formverCallableId(className: String?, name: String): CallableId = callableId(SpecialPackages.formver, className, name)
fun kotlinCallableId(className: String?, name: String): CallableId = callableId(SpecialPackages.kotlin, className, name)

fun FirBasedSymbol<*>.isUnique(session: FirSession) = hasAnnotation(annotationId("Unique"), session)

fun FirBasedSymbol<*>.isBorrowed(session: FirSession) = hasAnnotation(annotationId("Borrowed"), session)

fun FirAnnotationContainer.isUnique(session: FirSession) = hasAnnotation(annotationId("Unique"), session)

fun FirAnnotationContainer.isBorrowed(session: FirSession) = hasAnnotation(annotationId("Borrowed"), session)

fun FirFunctionSymbol<*>.isFormverFunctionNamed(name: String) =
    this is FirNamedFunctionSymbol && callableId == formverCallableId(className = null, name)

fun FirFunctionSymbol<*>.isInvariantBuilderFunctionNamed(name: String) =
    this is FirNamedFunctionSymbol && callableId == formverCallableId("InvariantBuilder", name)

@OptIn(SymbolInternals::class)
val FirFunctionSymbol<*>.shouldBeInlined
    get() = isInline && fir.body != null
