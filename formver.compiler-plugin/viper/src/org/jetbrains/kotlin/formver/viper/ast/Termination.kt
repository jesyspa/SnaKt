/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.formver.viper.toScalaOption
import org.jetbrains.kotlin.formver.viper.toScalaSeq
import org.jetbrains.kotlin.formver.viper.toSilver

/**
 * The type Silver's predicate-instance plugin gives to every `PredicateInstance` expression.
 *
 * The plugin declares the domain itself and hands the type out ready-made, so there is no
 * [SymbolicName] to build a [Type.Domain] from: a name put through the [NameResolver] is renamed
 * as needed to stay collision-free, and would then no longer denote the domain the plugin declares.
 */
data object PredicateInstanceType : Type {
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.Type =
        viper.silver.plugin.standard.predicateinstance.`PredicateInstance$`.`MODULE$`.getType()

    override fun substitute(typeVarMap: kotlin.collections.Map<Type.TypeVar, Type>): PredicateInstanceType =
        PredicateInstanceType
}

/**
 * A predicate instance `P(args)`: the predicate itself as a value, rather than the permission to it.
 *
 * Termination measures compare these to express that a recursive call unfolds a predicate, which is
 * the one ordering on the heap the termination plugin knows about.
 */
data class PredicateInstance(
    val predicateName: SymbolicName,
    val args: List<Exp>,
    override val pos: Position = Position.NoPosition,
    override val info: Info = Info.NoInfo,
) : Exp {
    override val type: Type = PredicateInstanceType

    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.plugin.standard.predicateinstance.PredicateInstance =
        viper.silver.plugin.standard.predicateinstance.PredicateInstance(
            predicateName.mangled,
            args.toSilver().toScalaSeq(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

    context(nameResolver: NameResolver)
    override fun registerNames() {
        nameResolver.register(predicateName)
        args.forEach { it.registerNames() }
    }
}

/**
 * A `decreases` tuple: the lexicographically ordered measure that must decrease at every recursive
 * call, guarded by [condition] when it only applies on part of the input.
 *
 * Silver models decreases clauses as expressions so that they can sit among a callable's
 * preconditions; the Bool type is what that placement demands rather than anything meaningful.
 */
data class DecreasesTuple(
    val tupleExprs: List<Exp>,
    val condition: Exp? = null,
    override val pos: Position = Position.NoPosition,
    override val info: Info = Info.NoInfo,
) : Exp {
    override val type: Type = Type.Bool

    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.plugin.standard.termination.DecreasesTuple =
        viper.silver.plugin.standard.termination.DecreasesTuple(
            tupleExprs.toSilver().toScalaSeq(),
            condition?.toSilver().toScalaOption(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

    context(nameResolver: NameResolver)
    override fun registerNames() {
        tupleExprs.forEach { it.registerNames() }
        condition?.registerNames()
    }
}

/**
 * A `decreases _` clause: termination is claimed but its measure is left to the verifier to find.
 *
 * See [DecreasesTuple] on why a decreases clause is typed Bool.
 */
data class DecreasesWildcard(
    val condition: Exp? = null,
    override val pos: Position = Position.NoPosition,
    override val info: Info = Info.NoInfo,
) : Exp {
    override val type: Type = Type.Bool

    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.plugin.standard.termination.DecreasesWildcard =
        viper.silver.plugin.standard.termination.DecreasesWildcard(
            condition?.toSilver().toScalaOption(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

    context(nameResolver: NameResolver)
    override fun registerNames() {
        condition?.registerNames()
    }
}
