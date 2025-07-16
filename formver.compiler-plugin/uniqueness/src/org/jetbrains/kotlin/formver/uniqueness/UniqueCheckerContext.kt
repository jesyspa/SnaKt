/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.text
import kotlin.collections.set

typealias ContextFrames = MutableList<HashMap<FirVariableSymbol<*>, UniqueLevel>>

data class UniqueCheckerContext(
    val session: FirSession,
    val config: PluginConfiguration,
    val errorCollector: ErrorCollector
) {
    private fun getAnnotationId(name: String): ClassId =
        ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")), Name.identifier(name))

    private val uniqueId: ClassId
        get() = getAnnotationId("Unique")

    var resolvedUniquenessFrames: ContextFrames = mutableListOf()
    val ownedTo: HashMap<FirVariableSymbol<*>, FirVariableSymbol<*>> = hashMapOf()
    val ownedBy: HashMap<FirVariableSymbol<*>, FirVariableSymbol<*>> = hashMapOf()

    fun resolveUniquenessAnnotation(declaration: FirDeclaration): UniqueLevel =
        if (declaration.hasAnnotation(uniqueId, session)) {
            UniqueLevel.Unique
        } else {
            UniqueLevel.Shared
        }

    fun resolveUniqueness(declaration: FirVariableSymbol<*>): UniqueLevel =
        resolvedUniquenessFrames.asReversed().asSequence().firstNotNullOfOrNull {
            it[declaration]
        } ?: throw IllegalArgumentException("can't resolve uniqueness for ${declaration.source.text}")

    fun assignUniqueness(declaration: FirVariableSymbol<*>, level: UniqueLevel) {
        val declarationFrame =
            resolvedUniquenessFrames.find { it.containsKey(declaration) } ?: resolvedUniquenessFrames.last()
        declarationFrame[declaration] = level
    }

    fun assignOwnership(declaration: FirVariableSymbol<*>, owner: FirVariableSymbol<*>) {
        if (resolveUniqueness(declaration) != UniqueLevel.Unique) {
            throw IllegalArgumentException("can't assign ownership for ${declaration.source.text} as it is not unique")
        }
        ownedTo[owner] = declaration
        ownedBy[declaration] = owner
        assignUniqueness(declaration, UniqueLevel.Top)
    }

    fun resetOwnership(declaration: FirVariableSymbol<*>) {
        val owner = ownedTo[declaration] ?: return
        ownedTo.remove(owner)
        ownedBy.remove(declaration)
    }

    fun pushUniquenessFrame() {
        resolvedUniquenessFrames.push(hashMapOf())
    }

    fun popUniquenessFrame() {
        val lastFrame = resolvedUniquenessFrames.pop()
        for ((declaration, level) in lastFrame) {
            if (level == UniqueLevel.Unique) {
                ownedTo[declaration]?.let { assignUniqueness(it, UniqueLevel.Unique) }
                ownedTo.remove(declaration)
            }
        }
    }

    fun runBlock(block: FirBlock, visitor: FirVisitor<UniqueLevel, UniqueCheckerContext>): UniqueCheckerContext {
        val newContext = copy()
        resolvedUniquenessFrames.forEach {
            newContext.resolvedUniquenessFrames.push(HashMap(it))
        }

        newContext.ownedTo.putAll(ownedTo)
        newContext.ownedBy.putAll(ownedBy)

        block.accept(visitor, newContext)
        return newContext
    }

    fun assignVariable(
        visitor: FirVisitor<UniqueLevel, UniqueCheckerContext>,
        declaration: FirVariableSymbol<*>,
        rvalue: FirExpression?
    ) {
        val initializerLevel = rvalue?.accept(visitor, this) ?: UniqueLevel.Shared

        if (initializerLevel == UniqueLevel.Top) {
            throw IllegalArgumentException("attempting to assign an inaccessible property ${declaration.source.text}")
        }

        assignUniqueness(declaration, initializerLevel)

        val assignedSymbol = rvalue?.let { symbolFromExpression(it) }

        if (assignedSymbol != null && initializerLevel == UniqueLevel.Unique) {
            assignOwnership(assignedSymbol, declaration)
        } else {
            resetOwnership(declaration)
        }
    }

    companion object {
        fun lub(left: UniqueLevel, right: UniqueLevel): UniqueLevel {
            return if (left.ordinal > right.ordinal) left else right
        }

        fun mergeContextFrames(lhs: ContextFrames, rhs: ContextFrames): ContextFrames {
            val newFrames = mutableListOf<HashMap<FirVariableSymbol<*>, UniqueLevel>>()
            lhs.zip(rhs).forEach { (lhsFrame, rhsFrame) ->
                val keys = lhsFrame.keys + rhsFrame.keys
                val newFrame = hashMapOf<FirVariableSymbol<*>, UniqueLevel>()
                keys.forEach { key ->
                    newFrame[key] = lub(lhsFrame[key] ?: UniqueLevel.Top, rhsFrame[key] ?: UniqueLevel.Top)

                    newFrames.push(newFrame)
                }
            }
            return newFrames
        }

        fun symbolFromExpression(expression: FirExpression): FirVariableSymbol<*>? =
            (((expression as? FirPropertyAccessExpression)?.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirVariableSymbol<*>)
    }
}