/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.asSourceRole
import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.domains.Injection
import org.jetbrains.kotlin.formver.core.domains.viperType
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.asInfo
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.NamedBranchingNode
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.core.embeddings.properties.PropertyAccessEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.fillHoles
import org.jetbrains.kotlin.formver.core.embeddings.types.injectionOrNull
import org.jetbrains.kotlin.formver.core.names.AnonymousBuiltinName
import org.jetbrains.kotlin.formver.core.names.AnonymousName
import org.jetbrains.kotlin.formver.core.names.ResultVariableName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.mangled

sealed interface VariableEmbedding : PureExpEmbedding, PropertyAccessEmbedding {
    val name: SymbolicName
    override val type: TypeEmbedding
    val isUnique: Boolean
        get() = false
    val isBorrowed: Boolean
        get() = false

    fun toLocalVarDecl(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Declaration.LocalVarDecl = Declaration.LocalVarDecl(name, Type.Ref, pos, info, trafos)

    fun toLocalVarUse(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.LocalVar = Exp.LocalVar(name, Type.Ref, pos, info, trafos)

    override fun toViper(source: KtSourceElement?): Exp = when (name) {
        is ResultVariableName -> Exp.Result(Type.Ref, source.asPosition, sourceRole.asInfo)
        else -> Exp.LocalVar(name, Type.Ref, source.asPosition, sourceRole.asInfo)
    }

    val isOriginallyRef: Boolean
        get() = true

    override fun getValue(ctx: StmtConversionContext): ExpEmbedding = this
    override fun setValue(value: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding = Assign(this, value)

    fun pureInvariants(): List<ExpEmbedding> = type.pureInvariants().fillHoles(this)
    fun provenInvariants(): List<ExpEmbedding> = listOf(type.subTypeInvariant().fillHole(this))
    fun accessInvariants(): List<ExpEmbedding> = type.accessInvariants().fillHoles(this)
    fun sharedPredicateAccessInvariant() = type.sharedPredicateAccessInvariant()?.fillHole(this)
    fun uniquePredicateAccessInvariant() = type.uniquePredicateAccessInvariant()?.fillHole(this)

    fun allAccessInvariants() = accessInvariants() + listOfNotNull(sharedPredicateAccessInvariant())

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode("Var", PlaintextLeaf(name.mangled))
}

/**
 * Embedding of a variable that is only used as a local placeholder, e.g. the return value or parameters
 * in a type signature.
 */
class PlaceholderVariableEmbedding(
    override val name: SymbolicName,
    override val type: TypeEmbedding,
    override val isUnique: Boolean = false,
    override val isBorrowed: Boolean = false,
) : VariableEmbedding

/**
 * Embedding of an anonymous variable.
 */
class AnonymousVariableEmbedding(n: Int, override val type: TypeEmbedding) : VariableEmbedding {
    override val name: SymbolicName = AnonymousName(n)
}

class AnonymousBuiltinVariableEmbedding(n: Int, override val type: TypeEmbedding) : VariableEmbedding {
    override val name: SymbolicName = AnonymousBuiltinName(n)
    private val injection: Injection? = type.injectionOrNull
    override fun toViper(source: KtSourceElement?): Exp {
        val inner = Exp.LocalVar(name, injection.viperType, source.asPosition, sourceRole.asInfo)
        return injection?.let { it.toRef(inner) } ?: inner
    }

    override fun toLocalVarDecl(pos: Position, info: Info, trafos: Trafos) =
        Declaration.LocalVarDecl(name, injection.viperType, pos, info, trafos)

    override fun toLocalVarUse(pos: Position, info: Info, trafos: Trafos): Exp.LocalVar =
        Exp.LocalVar(name, injection.viperType, pos, info, trafos)

    override val isOriginallyRef: Boolean
        get() = injection == null
}

/**
 * Embedding of a variable that comes from some FIR element.
 */
class FirVariableEmbedding(
    override val name: SymbolicName,
    override val type: TypeEmbedding,
    val symbol: FirBasedSymbol<*>,
    override val isUnique: Boolean = false,
    override val isBorrowed: Boolean = false,
) : VariableEmbedding {
    override val sourceRole: SourceRole
        get() = symbol.asSourceRole
}

/**
 * Variable embedding generated at linearization phase.
 *
 * This can still correspond to an earlier variable, but it no longer carries any interesting information.
 */
class LinearizationVariableEmbedding(override val name: SymbolicName, override val type: TypeEmbedding) :
    VariableEmbedding

val ExpEmbedding.underlyingVariable
    get() = this.ignoringCastsAndMetaNodes() as? VariableEmbedding
