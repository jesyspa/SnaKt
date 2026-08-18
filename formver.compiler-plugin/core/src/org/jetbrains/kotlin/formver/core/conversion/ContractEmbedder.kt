/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding

/**
 * Embeds function contracts (Kotlin `contract { }` blocks and Formver `requires`/`ensures`
 * specifications) as pre/postcondition expressions.
 */
class ContractEmbedder(private val programCtx: ProgramConversionContext) {

    fun embedContract(
        symbol: FirFunctionSymbol<*>, signature: NamedFunctionSignature, returnTarget: ReturnTarget
    ): Pair<List<ExpEmbedding>, List<ExpEmbedding>> {
        val kotlinContractPostcondition = embedKotlinContract(symbol, signature)
        val userContract = embedFormverContract(symbol, signature, returnTarget)
        return Pair(userContract.first, kotlinContractPostcondition + userContract.second)
    }

    private fun embedFormverContract(
        symbol: FirFunctionSymbol<*>, signature: NamedFunctionSignature, returnTarget: ReturnTarget
    ): Pair<List<ExpEmbedding>, List<ExpEmbedding>> {
        @OptIn(SymbolInternals::class) val declaration = symbol.fir
        val body = declaration.body

        /** Specifications are only allowed inside simple functions.
         * We are also unable to retrieve them when body is not visible,
         * although ideally we should be able to see preconditions and postconditions
         * from other modules.
         */
        if (declaration !is FirSimpleFunction || body == null) {
            return Pair(emptyList(), emptyList())
        }

        val firSpec = extractFirSpecification(body, declaration.symbol.resolvedReturnType, programCtx)


        val (preconditionContext, postconditionContext) = createContractConversionContext(
            symbol, signature, firSpec, returnTarget
        )

        val preconditions = firSpec.precond?.let { preconditionContext.collectInvariants(it) } ?: emptyList()
        val postconditions = firSpec.postcond?.let { postconditionContext.collectInvariants(it) } ?: emptyList()

        return Pair(preconditions, postconditions)
    }

    private fun embedKotlinContract(
        symbol: FirFunctionSymbol<*>, signature: NamedFunctionSignature
    ): List<ExpEmbedding> {
        val contractVisitor = ContractDescriptionConversionVisitor(programCtx, signature, symbol)
        return contractVisitor.getPostconditions()
    }

    private fun createContractConversionContext(
        symbol: FirFunctionSymbol<*>,
        signature: NamedFunctionSignature,
        firSpec: FirSpecification,
        returnTarget: ReturnTarget,
    ): Pair<StmtConversionContext, StmtConversionContext> {
        val rootResolver = RootParameterResolver(
            programCtx,
            signature,
            symbol.valueParameterSymbols,
            signature.labelName,
            returnTarget,
        )

        val wrappedResolver = firSpec.returnVar?.let { ReturnVarSubstitutor(it, signature.returns) }
            ?.let { ctx -> SubstitutedReturnParameterResolver(rootResolver, ctx) } ?: rootResolver

        val preconditionContext = MethodConverter(
            programCtx,
            signature,
            rootResolver,
            ScopeIndex.NoScope,
        ).statementCtxt()

        val postconditionContext = MethodConverter(
            programCtx,
            signature,
            wrappedResolver,
            ScopeIndex.NoScope,
        ).statementCtxt()

        return Pair(preconditionContext, postconditionContext)
    }
}
