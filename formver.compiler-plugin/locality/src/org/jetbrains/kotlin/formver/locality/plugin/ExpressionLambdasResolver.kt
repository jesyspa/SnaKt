/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeFactResolver
import org.jetbrains.kotlin.formver.type.plugin.UnifyingExpressionTypeFactResolver

private object TerminalLambdasResolver : ExpressionTypeFactResolver<Set<FirAnonymousFunction>> {
    context(context: CheckerContext)
    override fun resolveTypeFactOf(expression: FirExpression): Set<FirAnonymousFunction> =
        setOfNotNull((expression as? FirAnonymousFunctionExpression)?.anonymousFunction)
}

class ExpressionLambdasResolver(session: FirSession) :
    ExpressionTypeFactResolver<Set<FirAnonymousFunction>> by UnifyingExpressionTypeFactResolver(
        session.firCachesFactory,
        { left, right -> left + right },
        TerminalLambdasResolver
    ), FirExtensionSessionComponent(session) {
    companion object : ExpressionTypeFactResolver<Set<FirAnonymousFunction>> {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionLambdasResolver(session) }
        }

        context(context: CheckerContext)
        override fun resolveTypeFactOf(expression: FirExpression): Set<FirAnonymousFunction> =
            context.session.expressionLambdasResolver.resolveTypeFactOf(expression)
    }
}

private val FirSession.expressionLambdasResolver: ExpressionLambdasResolver
    by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveLambdas(): Set<FirAnonymousFunction> =
    ExpressionLambdasResolver.resolveTypeFactOf(this)
