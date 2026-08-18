/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.utils.addIfNotNull

fun StmtConversionContext.argumentDeclaration(
    arg: ExpEmbedding,
    callType: TypeEmbedding
): Pair<Declare?, ExpEmbedding> =
    when (arg.ignoringMetaNodes()) {
        is LambdaExp -> null to arg
        else -> {
            val argWithInvariants = arg.withNewTypeInvariants(callType, typeResolver) {
                proven = true
                access = true
            }
            // If `argWithInvariants` is `Cast(...(Cast(someVariable))...)` it is fine to use it
            // since in Viper it will always be translated to `someVariable`.
            // On other hand, `TypeEmbedding` and invariants in Viper are guaranteed
            // via previous line.
            if (argWithInvariants.underlyingVariable != null) null to argWithInvariants
            else declareAnonVar(callType, argWithInvariants).let {
                it to it.variable
            }
        }
    }

fun StmtConversionContext.getInlineFunctionCallArgs(
    args: List<ExpEmbedding>,
    formalArgTypes: List<TypeEmbedding>,
): Pair<List<Declare>, List<ExpEmbedding>> {
    val declarations = mutableListOf<Declare>()
    val storedArgs = args.zip(formalArgTypes).map { (arg, callType) ->
        argumentDeclaration(arg, callType).let { (declaration, usage) ->
            declarations.addIfNotNull(declaration)
            usage
        }
    }
    return Pair(declarations, storedArgs)
}

fun StmtConversionContext.insertInlineFunctionCall(
    calleeSignature: FunctionSignature,
    paramNames: List<SubstitutedArgument>,
    args: List<ExpEmbedding>,
    body: FirBlock,
    returnTargetName: String?,
    parentCtx: MethodConversionContext? = null,
): ExpEmbedding {
    // TODO: It seems like it may be possible to avoid creating a local here, but it is not clear how.
    val returnTarget = returnTargetProducer.getFresh(calleeSignature.callableType.returnType)
    assert(returnTarget.label != null) {
        "Return target label not found for function ${calleeSignature.callableType.name}"
    }
    val (declarations, callArgs) = getInlineFunctionCallArgs(args, calleeSignature.callableType.formalArgTypes)
    val subs = paramNames.zip(callArgs).toMap()
    val methodCtxFactory = MethodContextFactory(
        calleeSignature,
        InlineParameterResolver(subs, returnTargetName, returnTarget),
        parent = parentCtx,
    )

    return withMethodCtx(methodCtxFactory) {
        Block {
            add(Declare(returnTarget.variable, null))
            addAll(declarations)
            add(FunctionExp(null, convert(body), returnTarget.label!!))
            // if unit is what we return we might not guarantee it yet
            add(returnTarget.variable.withIsUnitInvariantIfUnit(typeResolver))
        }
    }
}

/**
 * Insert `ForAllEmbedding` where `forAll` function call was encountered.
 */
fun StmtConversionContext.insertForAllFunctionCall(
    symbol: FirValueParameterSymbol,
    block: FirBlock,
): ExpEmbedding {
    val anonVar = freshAnonBuiltinVar(embedType(symbol.resolvedReturnType))
    val methodCtxFactory = MethodContextFactory(
        signature,
        InlineParameterResolver(
            substitutions = mapOf(SubstitutedArgument.ValueParameter(symbol) to anonVar),
            labelName = null,
            // TODO: ideally, there shouldn't be a return target since return is prohibited
            defaultResolvedReturnTarget = defaultResolvedReturnTarget,
        ),
        parent = this,
    )
    return withNoScope {
        withMethodCtx(methodCtxFactory) {
            val (invariants, triggers) = collectInvariantsAndTriggers(block)
            ForAllEmbedding(anonVar, invariants, triggers)
        }
    }
}
