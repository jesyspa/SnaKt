/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*


/**
 * We want to deal with Viper's binary operators, functions and domain functions in a similar manner, hence introducing this common interface.
 */
interface Applicable {
    fun toFuncApp(
        args: List<Exp>,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
    ): Exp

    operator fun invoke(
        vararg args: Exp,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
    ) =
        toFuncApp(args.toList(), pos, info)
}

interface Function : IntoSilver<viper.silver.ast.Function>, Applicable {
    val name: SymbolicName
    val pos: Position
        get() = Position.NoPosition
    val info: Info
        get() = Info.NoInfo
    val includeInDumpPolicy: IncludeInDumpPolicy
    val formalArgs: List<Declaration.LocalVarDecl>
    val retType: Type
    val pres: List<Exp>
        get() = listOf()
    val posts: List<Exp>
        get() = listOf()
    val body: Exp?
        get() = null

    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.Function = viper.silver.ast.Function(
        name.mangled,
        formalArgs.map { it.toSilver() }.toScalaSeq(),
        retType.toSilver(),
        pres.toSilver().toScalaSeq(),
        postsWithTerminationMeasure.toSilver().toScalaSeq(),
        body.toScalaOption().toSilver(),
        pos.toSilver(),
        info.toSilver(),
        silverNoTrafos
    )

    override fun toFuncApp(
        args: List<Exp>,
        pos: Position,
        info: Info,
    ): Exp.FuncApp = Exp.FuncApp(name, args, retType, pos, info)
}

data class UserFunction(
    override val name: SymbolicName,
    override val formalArgs: List<Declaration.LocalVarDecl>,
    override val retType: Type,
    override val pres: List<Exp>,
    override val posts: List<Exp>,
    override val body: Exp?,
    override val pos: Position = Position.NoPosition,
    override val info: Info = Info.NoInfo,
) : Function {
    override val includeInDumpPolicy: IncludeInDumpPolicy = IncludeInDumpPolicy.ALWAYS
}

abstract class BuiltinFunction(
    override val name: SymbolicName,
    override val pos: Position = Position.NoPosition,
    override val info: Info = Info.NoInfo,
) : Function {
    override val includeInDumpPolicy: IncludeInDumpPolicy = IncludeInDumpPolicy.ONLY_IN_FULL_DUMP
}

/**
 * The postconditions to emit, with a termination measure supplied where there is none and none can be needed.
 *
 * A bodyless function is uninterpreted, so it cannot recurse and claiming it terminates claims
 * nothing. It still has to make the claim: a function that carries a measure of its own fails to
 * verify at every call to a function that carries none. Functions with a body are left alone, since
 * for them the measure is a real proof obligation and its absence is a deliberate gap.
 */
private val Function.postsWithTerminationMeasure: List<Exp>
    get() = when {
        body != null -> posts
        posts.any { it is DecreasesTuple || it is DecreasesWildcard } -> posts
        else -> posts + DecreasesWildcard()
    }

/**
 * These are function-like classes which are not translated to Viper as function calls but as arithmetic and/or boolean operations.
 */
interface Operator : Applicable
