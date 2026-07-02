/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*
import viper.silver.ast.*

/**
 * Shape shared by every binary Viper expression: two sub-expressions plus the position/info
 * trailer, encoded into Silver as `make(left, right, pos, info, silverNoTrafos)`.
 */
sealed interface BinaryExp : Exp {
    val left: Exp
    val right: Exp

    context(nameResolver: NameResolver)
    override fun registerNames() {
        left.registerNames()
        right.registerNames()
    }

    context(nameResolver: NameResolver)
    fun <S : viper.silver.ast.Exp> toSilverVia(
        make: (viper.silver.ast.Exp, viper.silver.ast.Exp, viper.silver.ast.Position, viper.silver.ast.Info, ErrorTrafo) -> S,
    ): S = make(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), silverNoTrafos)
}

/**
 * Shape shared by every unary Viper expression: one sub-expression plus the position/info
 * trailer, encoded into Silver as `make(arg, pos, info, silverNoTrafos)`.
 */
sealed interface UnaryExp : Exp {
    val arg: Exp

    context(nameResolver: NameResolver)
    override fun registerNames() {
        arg.registerNames()
    }

    context(nameResolver: NameResolver)
    fun <S : viper.silver.ast.Exp> toSilverVia(
        make: (viper.silver.ast.Exp, viper.silver.ast.Position, viper.silver.ast.Info, ErrorTrafo) -> S,
    ): S = make(arg.toSilver(), pos.toSilver(), info.toSilver(), silverNoTrafos)
}

sealed interface Exp : WithSilverMetadata, IntoSilver<viper.silver.ast.Exp> {

    val type: Type

    /**
     * Registers the [org.jetbrains.kotlin.formver.viper.SymbolicName]s this node and its
     * descendants introduce or reference, so they receive collision-free Viper identifiers.
     * Must recurse into exactly the sub-nodes whose names end up in the emitted Viper program.
     */
    context(nameResolver: NameResolver)
    fun registerNames()

    data class Minus(
        override val arg: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : UnaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Minus = toSilverVia(::Minus)
    }

    //region Arithmetic Expressions
    data class Add(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Add = toSilverVia(::Add)
    }

    data class Sub(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Sub = toSilverVia(::Sub)
    }

    data class Mul(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Mul = toSilverVia(::Mul)
    }

    data class Div(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Div = toSilverVia(::Div)
    }

    data class Mod(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Mod = toSilverVia(::Mod)
    }
    //endregion

    //region Integer Comparison Expressions
    data class LtCmp(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.LtCmp = toSilverVia(::LtCmp)
    }

    data class LeCmp(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.LeCmp = toSilverVia(::LeCmp)
    }

    data class GtCmp(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.GtCmp = toSilverVia(::GtCmp)
    }

    data class GeCmp(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.GeCmp = toSilverVia(::GeCmp)
    }
    //endregion

    //region Boolean Comparison Expressions
    data class EqCmp(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.EqCmp = toSilverVia(::EqCmp)
    }

    data class NeCmp(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.NeCmp = toSilverVia(::NeCmp)
    }
    //endregion

    //region Boolean Expressions
    data class And(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.And = toSilverVia(::And)
    }

    data class Or(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Or = toSilverVia(::Or)
    }

    data class Implies(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Implies = toSilverVia(::Implies)
    }

    data class Not(
        override val arg: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : UnaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Not = toSilverVia(::Not)
    }
    //endregion

    //region Quantifier Expressions
    class Trigger(
        val exps: List<Exp>,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
    ) : IntoSilver<viper.silver.ast.Trigger> {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Trigger =
            Trigger(exps.toSilver().toScalaSeq(), pos.toSilver(), info.toSilver(), silverNoTrafos)
    }

    data class Forall(
        val variables: List<Declaration.LocalVarDecl>,
        val triggers: List<Trigger>,
        val exp: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Forall =
            Forall(
                variables.map { it.toSilver() }.toScalaSeq(),
                triggers.toSilver().toScalaSeq(),
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos
            )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            variables.forEach { nameResolver.register(it.name) }
            exp.registerNames()
        }
    }

    data class Exists(
        val variables: List<Declaration.LocalVarDecl>,
        val triggers: List<Trigger>,
        val exp: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Exists =
            Exists(
                variables.map { it.toSilver() }.toScalaSeq(),
                triggers.toSilver().toScalaSeq(),
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos
            )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            variables.forEach { nameResolver.register(it.name) }
            exp.registerNames()
        }
    }

    //region Literals
    data class IntLit(
        val value: Int,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.IntLit =
            IntLit(value.toScalaBigInt(), pos.toSilver(), info.toSilver(), silverNoTrafos)

        context(nameResolver: NameResolver)
        override fun registerNames() {}
    }

    data class NullLit(
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        override val type = Type.Ref
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.NullLit = NullLit(pos.toSilver(), info.toSilver(), silverNoTrafos)

        context(nameResolver: NameResolver)
        override fun registerNames() {}
    }

    data class BoolLit(
        val value: Boolean,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.BoolLit =
            viper.silver.ast.BoolLit.apply(value, pos.toSilver(), info.toSilver(), silverNoTrafos)

        context(nameResolver: NameResolver)
        override fun registerNames() {}
    }
    //endregion

    data class LocalVar(
        val name: SymbolicName,
        override val type: Type,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.LocalVar =
            LocalVar(name.mangled, type.toSilver(), pos.toSilver(), info.toSilver(), silverNoTrafos)

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(name)
        }
    }

    data class FieldAccess(
        val rcv: Exp,
        val field: Field,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        override val type = field.type
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.FieldAccess =
            FieldAccess(rcv.toSilver(), field.toSilver(), pos.toSilver(), info.toSilver(), silverNoTrafos)

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(field.name)
            rcv.registerNames()
        }
    }

    data class Result(
        override val type: Type,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Result =
            Result(type.toSilver(), pos.toSilver(), info.toSilver(), silverNoTrafos)

        context(nameResolver: NameResolver)
        override fun registerNames() {}
    }

    data class FuncApp(
        val functionName: SymbolicName,
        val args: List<Exp>,
        override val type: Type,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.FuncApp = FuncApp(
            functionName.mangled,
            args.toSilver().toScalaSeq(),
            pos.toSilver(),
            info.toSilver(),
            type.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(functionName)
            args.forEach { it.registerNames() }
        }
    }

    /**
     * IMPORTANT: typeVarMap needs to be set even when the type variables are
     * not instantiated. In that case map the generic type variables to themselves.
     * Example: x is of type T, f(x: T) -> Int is a domain function, and you want to
     * make the generic domain function call f(x) then a Map from T -> T needs to be
     * supplied.
     */
    data class DomainFuncApp(
        val function: DomainFunc,
        val args: List<Exp>,
        val typeVarMap: Map<Type.TypeVar, Type>,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        context(nameResolver: NameResolver)
        private val scalaTypeVarMap: scala.collection.immutable.Map<TypeVar, viper.silver.ast.Type>
            get() = typeVarMap.mapKeys { it.key.toSilver() }.mapValues { it.value.toSilver() }.toScalaMap()
        override val type = function.returnType.substitute(typeVarMap)
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Exp =
            viper.silver.ast.DomainFuncApp.apply(
                function.toSilver(),
                args.toSilver().toScalaSeq(),
                scalaTypeVarMap,
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos
            )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(function.name)
            function.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
            args.forEach { it.registerNames() }
        }
    }

    /** Represents the application of an ADT constructor. Triggered by an ADT reference in an `AdtConstructorRef`. */
    data class AdtConstructorApp(
        val constructor: AdtConstructorDecl,
        val args: List<Exp>,
        val typeVarMap: Map<Type.TypeVar, Type> = emptyMap(),
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        override val type: Type = Type.Adt(constructor.adtName)

        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Exp =
            viper.silver.plugin.standard.adt.AdtConstructorApp.apply(
                constructor.toSilver(),
                args.toSilver().toScalaSeq(),
                typeVarMap.mapKeys { it.key.toSilver() }.mapValues { it.value.toSilver() }.toScalaMap(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos,
            )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(constructor.name)
            args.forEach { it.registerNames() }
        }
    }

    data class ExplicitSeq(
        val args: List<Exp>,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.ExplicitSeq =
            ExplicitSeq(
                args.toSilver().toScalaSeq(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos,
            )

        override val type = Type.Seq(args.first().type)

        context(nameResolver: NameResolver)
        override fun registerNames() {
            args.forEach { it.registerNames() }
        }
    }

    data class EmptySeq(
        val elementType: Type,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.EmptySeq =
            viper.silver.ast.EmptySeq.apply(
                elementType.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos,
            )

        override val type = Type.Seq(elementType)

        context(nameResolver: NameResolver)
        override fun registerNames() {}
    }

    data class SeqLength(
        val seq: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.SeqLength =
            viper.silver.ast.SeqLength.apply(
                seq.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos,
            )

        override val type = Type.Int

        context(nameResolver: NameResolver)
        override fun registerNames() {
            seq.registerNames()
        }
    }

    data class SeqTake(
        val seq: Exp,
        val idx: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.SeqTake =
            viper.silver.ast.SeqTake.apply(
                seq.toSilver(),
                idx.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos,
            )

        override val type = seq.type

        context(nameResolver: NameResolver)
        override fun registerNames() {
            seq.registerNames()
            idx.registerNames()
        }
    }

    data class SeqIndex(
        val seq: Exp,
        val idx: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.SeqIndex =
            viper.silver.ast.SeqIndex.apply(
                seq.toSilver(),
                idx.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos,
            )

        override val type = Type.Int

        context(nameResolver: NameResolver)
        override fun registerNames() {
            seq.registerNames()
            idx.registerNames()
        }
    }

    data class SeqAppend(
        override val left: Exp,
        override val right: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : BinaryExp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.SeqAppend = toSilverVia(viper.silver.ast.SeqAppend::apply)

        override val type = left.type
    }

    data class Old(
        val exp: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        override val type = exp.type
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Old =
            Old(exp.toSilver(), pos.toSilver(), info.toSilver(), silverNoTrafos)

        context(nameResolver: NameResolver)
        override fun registerNames() {
            exp.registerNames()
        }
    }

    data class PredicateAccess(
        val predicateName: SymbolicName,
        val formalArgs: List<Exp>,
        val perm: PermExp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        // The type is set to Bool just to be consistent with Silver type. Probably it will never be used
        override val type: Type = Type.Bool

        // Note: since the simple syntax P(...) has the same meaning as acc(P(...)), which in turn has the same meaning as acc(P(...), write)
        // It is always better to deal with PredicateAccessPredicate because PredicateAccess seems not working well with Silver
        context(nameResolver: NameResolver)
        override fun toSilver(): PredicateAccessPredicate {
            val predicateAccess = PredicateAccess(
                formalArgs.toSilver().toScalaSeq(),
                predicateName.mangled,
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos
            )
            return PredicateAccessPredicate(
                predicateAccess,
                perm.toSilver().toScalaOption(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos
            )
        }

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(predicateName)
            formalArgs.forEach { it.registerNames() }
        }
    }

    data class Unfolding(
        val predicateAccess: PredicateAccess,
        val body: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        override val type = body.type
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Unfolding =
            Unfolding(predicateAccess.toSilver(), body.toSilver(), pos.toSilver(), info.toSilver(), silverNoTrafos)

        context(nameResolver: NameResolver)
        override fun registerNames() {
            predicateAccess.registerNames()
            body.registerNames()
        }
    }

    data class Acc(
        val field: FieldAccess,
        val perm: PermExp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Exp {
        override val type = Type.Bool

        context(nameResolver: NameResolver)
        override fun toSilver() = FieldAccessPredicate(
                field.toSilver(),
                perm.toSilver().toScalaOption(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos,
            )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            field.registerNames()
        }
    }

    data class LetBinding(
        val variable: Declaration.LocalVarDecl,
        val varExp: Exp,
        val body: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ): Exp {
        override val type: Type = variable.type

        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Let =
            Let(variable.toSilver(), varExp.toSilver(), body.toSilver(), pos.toSilver(), info.toSilver(), silverNoTrafos)

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(variable.name)
            body.registerNames()
        }
    }

    data class TernaryExp(
        val condExp: Exp,
        val thenExp: Exp,
        val elseExp: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ): Exp {
        override val type: Type = thenExp.type.also { assert(it == elseExp.type) }

        context(nameResolver: NameResolver)
        override fun toSilver(): CondExp =
            CondExp(condExp.toSilver(), thenExp.toSilver(), elseExp.toSilver(), pos.toSilver(), info.toSilver(), silverNoTrafos)

        context(nameResolver: NameResolver)
        override fun registerNames() {
            thenExp.registerNames()
            elseExp.registerNames()
        }
    }

    // We can't pass all the available position and info here.
    // Living with that seems fine for the moment.
    fun fieldAccess(
        field: Field,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
    ): FieldAccess =
        FieldAccess(this, field, pos, info)

    // We can't pass all the available position and info here.
    // Living with that seems fine for the moment.
    fun fieldAccessPredicate(
        field: Field,
        permission: PermExp,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
    ): AccessPredicate.FieldAccessPredicate =
        AccessPredicate.FieldAccessPredicate(fieldAccess(field, pos), permission, pos, info)

    companion object {
        private fun forallImpl(vars: List<Declaration.LocalVarDecl>, action: ForallBuilder.() -> Exp): Exp {
            val builder = ForallBuilder(vars)
            val body = builder.action()
            return builder.toForallExp(body)
        }

        /**
         * Create an Exp.Forall node, passing the local variables as local variable access expressions into the action.
         */
        fun forall(v1: Var, action: ForallBuilder.(LocalVar) -> Exp): Exp =
            forallImpl(listOf(v1.decl())) { action(v1.use()) }

        fun forall(v1: Var, v2: Var, action: ForallBuilder.(LocalVar, LocalVar) -> Exp): Exp =
            forallImpl(listOf(v1.decl(), v2.decl())) { action(v1.use(), v2.use()) }

        fun forall(v1: Var, v2: Var, v3: Var, action: ForallBuilder.(LocalVar, LocalVar, LocalVar) -> Exp): Exp =
            forallImpl(listOf(v1.decl(), v2.decl(), v3.decl())) { action(v1.use(), v2.use(), v3.use()) }

        /**
         * Take the conjunction of the given expressions.
         */
        fun List<Exp>.toConjunction(): Exp =
            if (isEmpty()) BoolLit(true)
            else reduce { l, r -> And(l, r) }
    }

    /**
     * Builder for statements of the form
     * ```
     * forall vars :: { triggers } assumptions ==> conclusion
     * ```
     *
     * The assumptions and conclusion together are the body of the forall.
     *
     * This class is intended to be used via the `Exp.forall` functions, not directly.
     */
    class ForallBuilder(private val vars: List<Declaration.LocalVarDecl>) {
        private val triggers = mutableListOf<Trigger>()
        private val assumptions = mutableListOf<Exp>()

        fun toForallExp(conclusion: Exp): Exp {
            val body =
                if (assumptions.isNotEmpty()) {
                    Implies(assumptions.toConjunction(), conclusion)
                } else {
                    conclusion
                }
            return Forall(vars, triggers, body)
        }

        /**
         * Add an assumption to this forall statement.
         */
        fun assumption(action: () -> Exp): Exp {
            val exp = action()
            assumptions.add(exp)
            return exp
        }

        /**
         * Create a trigger consisting of a single expression.
         */
        fun simpleTrigger(action: () -> Exp): Exp {
            val exp = action()
            triggers.add(Trigger(listOf(exp)))
            return exp
        }

        /**
         * Create a trigger consisting of multiple expressions, and return them as a conjunction.
         *
         * Note that a compound trigger must contain at least one expression; an empty list does
         * not make sense here.
         */
        fun compoundTrigger(action: TriggerBuilder.() -> Unit): Exp {
            val builder = TriggerBuilder()
            builder.action()
            triggers.add(builder.toTrigger())
            return builder.toConjunction()
        }

        class TriggerBuilder {
            private val exps = mutableListOf<Exp>()

            /**
             * Add an expression to the trigger.
             */
            fun subTrigger(action: () -> Exp): Exp {
                val exp = action()
                exps.add(exp)
                return exp
            }

            fun toTrigger(): Trigger {
                assert(exps.isNotEmpty()) { "There is no point to having an empty trigger expression. " }
                return Trigger(exps)
            }

            fun toConjunction(): Exp = exps.toConjunction()
        }
    }
}

operator fun Exp.not() = Exp.Not(this)
infix fun Exp.or(other: Exp) = Exp.Or(this, other)
infix fun Exp.and(other: Exp) = Exp.And(this, other)
infix fun Exp.eq(other: Exp) = Exp.EqCmp(this, other)
infix fun Exp.ne(other: Exp) = Exp.NeCmp(this, other)
infix fun Exp.ge(other: Exp) = Exp.GeCmp(this, other)
infix fun Exp.le(other: Exp) = Exp.LeCmp(this, other)
infix fun Exp.gt(other: Exp) = Exp.GtCmp(this, other)
infix fun Exp.lt(other: Exp) = Exp.LtCmp(this, other)
infix operator fun Exp.plus(other: Exp) = Exp.Add(this, other)
infix operator fun Exp.minus(other: Exp) = Exp.Sub(this, other)
infix operator fun Exp.times(other: Exp) = Exp.Mul(this, other)
infix operator fun Exp.div(other: Exp) = Exp.Div(this, other)
infix operator fun Exp.rem(other: Exp) = Exp.Mod(this, other)
infix fun Exp.implies(other: Exp) = Exp.Implies(this, other)
fun Int.toExp() = Exp.IntLit(this)
fun Boolean.toExp() = Exp.BoolLit(this)

fun Any?.viperLiteral(
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
): Exp = when (this) {
    null -> Exp.NullLit(pos, info)
    is Int -> Exp.IntLit(this, pos, info)
    is Boolean -> Exp.BoolLit(this, pos, info)
    is Char -> Exp.IntLit(this.code, pos, info)
    is String ->
        if (isEmpty()) Exp.EmptySeq(Type.Int, pos, info)
        else Exp.ExplicitSeq(map { it.viperLiteral(pos, info) }, pos, info)

    else -> error("Literal type not known.")
}
