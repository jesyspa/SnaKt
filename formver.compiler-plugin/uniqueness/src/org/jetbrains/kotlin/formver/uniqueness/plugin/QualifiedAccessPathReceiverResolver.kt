package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

/**
 * Resolves the receiver of [this] qualified access expression targeting a property.
 *
 * The receiver is defined only for [FirPropertyAccessExpression]s that resolve to a [FirPropertySymbol] with a non-null
 * backing field. In that case, FIR field access is receiver-based only through the dispatch receiver.
 *
 * Returns `null` for non-property accesses, properties without backing fields, and static/top-level property accesses
 * with no dispatch receiver.
 *
 * TODO: In the future we may want to handle explicit and extension receivers for supporting top-level and extension
 *  properties.
 */
val FirQualifiedAccessExpression.pathReceiver: FirExpression?
    get() =
        when(this) {
            is FirPropertyAccessExpression -> {
                val symbol = this.calleeReference.symbol as? FirPropertySymbol ?: return null

                if (symbol.backingFieldSymbol != null) {
                    dispatchReceiver
                } else {
                    null
                }
            }
            else -> null
        }
