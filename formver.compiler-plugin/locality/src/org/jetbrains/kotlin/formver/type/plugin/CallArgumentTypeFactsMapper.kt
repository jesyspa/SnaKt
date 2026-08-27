/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.orEmpty

/**
 * Resolves the types of the invoke parameters from the receiver expression.
 *
 * These include the types of the context and receiver parameters. The type-facts are to be returned in the order:
 * context parameters, receiver parameter, value parameters.
 */
fun interface InvokeParameterTypeFactsResolver<TypeFact> {
    context(context: CheckerContext)
    fun resolveInvokeParametersOf(receiver: FirExpression): List<TypeFact>?
}

private val FirCall.invokeDispatchReceiver: FirExpression?
    get() =
        when (this) {
            is FirImplicitInvokeCall -> dispatchReceiver
            // This can happen if .invoke is called explicitly.
            is FirFunctionCall if calleeReference.name == OperatorNameConventions.INVOKE -> dispatchReceiver
            else -> null
        }

/**
 * Resolves the types of the parameters of a call.
 *
 * @param TypeFact the type-fact class of the parameters.
 * @param declaredParameterTypeFactResolver the resolver for resolving the declared type of a call parameter.
 * @param invokeParameterTypeFactsResolver the resolver for resolving the types of the parameters of an invoke call.
 */
class CallArgumentTypeFactsMapper<TypeFact>(
    private val declaredParameterTypeFactResolver: SymbolTypeFactResolver<TypeFact, FirValueParameterSymbol>,
    private val invokeParameterTypeFactsResolver: InvokeParameterTypeFactsResolver<TypeFact>
) {

    /**
     * Resolves the mapping between the argument expressions of [call] and their corresponding type-facts.
     */
    context(_: CheckerContext)
    fun mapArgumentTypeFactsOf(call: FirCall): List<Pair<FirExpression, TypeFact>> {
        val invokeReceiver = call.invokeDispatchReceiver

        if (invokeReceiver != null) {
            val invokeParameterTypeFacts = invokeParameterTypeFactsResolver.resolveInvokeParametersOf(invokeReceiver)

            if (invokeParameterTypeFacts != null) {
                // NOTE: In invoke calls context and receiver arguments are actually passed as explicit normal
                // arguments. For example:
                // val f: context (Any) (Any).(Any) -> (Any)
                // with (1) { 2.f(3) } // Ok
                // f(1,2,3) // Same
                return call.arguments.zip(invokeParameterTypeFacts)
            }
        }

        return call.resolvedArgumentMapping?.map { (argument, parameter) ->
            argument to declaredParameterTypeFactResolver.resolveTypeFactOf(parameter.symbol)
        }.orEmpty()
    }
}
