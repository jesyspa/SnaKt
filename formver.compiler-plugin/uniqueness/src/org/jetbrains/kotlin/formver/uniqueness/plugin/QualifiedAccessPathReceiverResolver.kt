package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

/**
 * Resolves the receiver of [this] qualified access expression targeting a property.
 */
val FirQualifiedAccessExpression.pathReceiver: FirExpression?
    get() =
        when(this) {
            is FirPropertyAccessExpression -> {
                val symbol = this.calleeReference.symbol ?: return null

                if (symbol !is FirPropertySymbol) {
                    return null
                }

                if (symbol.backingFieldSymbol != null) {
                    explicitReceiver ?: extensionReceiver ?: dispatchReceiver
                } else {
                    null
                }
            }
            else -> null
        }
