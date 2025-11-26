/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildFunctionPretype
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.embeddings.types.nullableAny
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.DispatchReceiverName
import org.jetbrains.kotlin.formver.core.names.FunctionResultVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*

interface FullNamedFunctionSignature : NamedFunctionSignature {
    /**
     * Preconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    fun getPreconditions(): List<ExpEmbedding>

    /**
     * Postconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    fun getPostconditions(returnVariable: VariableEmbedding): List<ExpEmbedding>

    val declarationSource: KtSourceElement?
}

/**
 * We generate very reduced methods for getters and setters.
 * They don't have bodies or any invariants.
 * Since after the call to getter, invariants for the result will be inhaled based on the difference
 * between the returned `TypeEmbedding` and expected, we return the broadest possible type here.
 * Types of the arguments don't matter at all, but intuitively they must be `NullableAnyTypeEmbedding` as well.
 */
abstract class PropertyAccessorFunctionSignature(
    override val name: SymbolicName,
    propertySymbol: FirPropertySymbol,
) : FullNamedFunctionSignature, GenericFunctionSignatureMixin() {
    override fun getPreconditions() = emptyList<ExpEmbedding>()
    override fun getPostconditions(returnVariable: VariableEmbedding) = emptyList<ExpEmbedding>()
    override val dispatchReceiver: VariableEmbedding
        get() = PlaceholderVariableEmbedding(DispatchReceiverName, buildType { nullableAny() })
    override val extensionReceiver = null
    override val declarationSource: KtSourceElement? = propertySymbol.source
}

class GetterFunctionSignature(name: SymbolicName, symbol: FirPropertySymbol) :
    PropertyAccessorFunctionSignature(name, symbol) {
    override val symbol: FirFunctionSymbol<*>
        get() = error {
            "Getter symbol should not be accessed directly as it is allowed to be null in some cases."
        }
    override val callableType: FunctionTypeEmbedding = buildFunctionPretype {
        withDispatchReceiver { nullableAny() }
        withReturnType { nullableAny() }
    }
}

class SetterFunctionSignature(name: SymbolicName, symbol: FirPropertySymbol) :
    PropertyAccessorFunctionSignature(name, symbol) {
    override val symbol: FirFunctionSymbol<*>
        get() = error {
            "Setter symbol should not be accessed directly as it is allowed to be null in some cases."
        }
    override val callableType: FunctionTypeEmbedding = buildFunctionPretype {
        withDispatchReceiver { nullableAny() }
        withParam { nullableAny() }
        withReturnType { unit() }
    }
}

fun FullNamedFunctionSignature.toViperMethod(
    body: Stmt.Seqn?,
    returnVariable: VariableEmbedding,
) = UserMethod(
    name,
    formalArgs.map { it.toLocalVarDecl() },
    returnVariable.toLocalVarDecl(),
    getPreconditions().pureToViper(toBuiltin = true),
    getPostconditions(returnVariable).pureToViper(toBuiltin = true),
    body,
    declarationSource.asPosition
)

fun FullNamedFunctionSignature.toViperFunction(
    body: Exp?,
) = UserFunction(
    name,
    formalArgs.map { it.toLocalVarDecl() },
    // TODO: Be explicit about the return types of functions instead of boxing them into a Ref
    Type.Ref,
    getPreconditions().pureToViper(toBuiltin = true),
    getPostconditions(
        PlaceholderVariableEmbedding(
            FunctionResultVariableName,
            this.callableType.returnType
        )
    ).pureToViper(toBuiltin = true),
    body,
    declarationSource.asPosition
)