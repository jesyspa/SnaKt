package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.common.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * A restrictive version of the StmtConversionVisitor, which is used to embed functions annotated as pure
 * into an ExpEmbedding representation that reflects their pure aspects better and makes linearization easier
 */
object StmtPureConversionVisitor : FirVisitor<ExpEmbedding, StmtConversionContext>() {
    override fun visitElement(element: FirElement, data: StmtConversionContext): ExpEmbedding =
        handleUnimplementedElement(
            element.source,
            "Pure conversion not yet implemented for $element (${element.source.text}) - Consider removing the @Pure annotation of this function",
            data
        )

    override fun visitReturnExpression(
        returnExpression: FirReturnExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val expr = when (returnExpression.result) {
            is FirUnitExpression -> NullLit
            else -> data.convertPure(returnExpression.result)
        }
        return expr
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext): ExpEmbedding =
        block.statements.map(data::convertPure).toPureBlock()

    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression,
        data: StmtConversionContext,
    ): ExpEmbedding = when (literalExpression.kind) {
        ConstantValueKind.Int -> IntLit((literalExpression.value as Long).toInt())
        ConstantValueKind.Boolean -> BooleanLit(literalExpression.value as Boolean)
        ConstantValueKind.Char -> CharLit(literalExpression.value as Char)
        ConstantValueKind.Null -> NullLit
        // TODO: Think about StringPooling and string literals in a pure context here
        else -> handleUnimplementedElement(
            literalExpression.source,
            "Constant Expression of type ${literalExpression.kind} is not yet implemented.",
            data
        )
    }

    override fun visitProperty(property: FirProperty, data: StmtConversionContext): ExpEmbedding {
        val symbol = property.symbol
        if (!symbol.isLocal) {
            throw SnaktInternalException(
                property.source,
                "StmtPureConversionVisitor should not encounter non-local properties."
            )
        }

        val type = data.embedType(symbol.resolvedReturnType)
        return data.declareLocalProperty(symbol, property.initializer?.let { data.convertPure(it).withType(type) })
    }

    private fun handleUnimplementedElement(
        source: KtSourceElement?, msg: String, data: StmtConversionContext
    ): ExpEmbedding = when (data.config.behaviour) {
        UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
            throw SnaktInternalException(source, msg)

        UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
            data.errorCollector.addMinorError(msg)
            ErrorExp
        }
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val propertyAccess = data.embedPurePropertyAccess(propertyAccessExpression)
        return propertyAccess.getValue(data)
    }
}