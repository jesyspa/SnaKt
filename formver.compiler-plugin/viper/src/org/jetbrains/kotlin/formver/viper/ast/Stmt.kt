/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*

sealed interface Stmt : WithSilverMetadata, IntoSilver<viper.silver.ast.Stmt> {

    /**
     * Registers the [org.jetbrains.kotlin.formver.viper.SymbolicName]s this node and its
     * descendants introduce or reference, so they receive collision-free Viper identifiers.
     * Must recurse into exactly the sub-nodes whose names end up in the emitted Viper program.
     */
    context(nameResolver: NameResolver)
    fun registerNames()

    data class LocalVarAssign(
        val lhs: Exp.LocalVar,
        val rhs: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.LocalVarAssign =
            viper.silver.ast.LocalVarAssign(
                lhs.toSilver(),
                rhs.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos
            )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(lhs.name)
            rhs.registerNames()
        }
    }

    data class FieldAssign(
        val lhs: Exp.FieldAccess,
        val rhs: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.FieldAssign =
            viper.silver.ast.FieldAssign(
                lhs.toSilver(),
                rhs.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos
            )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            lhs.registerNames()
            rhs.registerNames()
        }
    }

    companion object {
        fun assign(
            lhs: Exp,
            rhs: Exp,
            pos: Position = Position.NoPosition,
            info: Info = Info.NoInfo,
        ): Stmt = when (lhs) {
            is Exp.LocalVar ->
                LocalVarAssign(lhs, rhs, pos, info)

            is Exp.FieldAccess ->
                FieldAssign(lhs, rhs, pos, info)

            else -> {
                throw IllegalArgumentException("Expected an lvalue on the left-hand side of an assignment.")
            }
        }
    }

    data class MethodCall(
        val methodName: SymbolicName,
        val args: List<Exp>,
        val targets: List<Exp.LocalVar>,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.MethodCall = viper.silver.ast.MethodCall(
            methodName.mangled,
            args.map { it.toSilver() }.toScalaSeq(),
            targets.map { it.toSilver() }.toScalaSeq(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            args.forEach { it.registerNames() }
            targets.forEach { it.registerNames() }
            nameResolver.register(methodName)
        }
    }

    data class Exhale(
        val exp: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Exhale = viper.silver.ast.Exhale(
            exp.toSilver(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            exp.registerNames()
        }
    }

    data class Inhale(
        val exp: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Inhale = viper.silver.ast.Inhale(
            exp.toSilver(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            exp.registerNames()
        }
    }

    data class Assert(
        val exp: Exp,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Assert = viper.silver.ast.Assert(
            exp.toSilver(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            exp.registerNames()
        }
    }

    data class Seqn(
        val stmts: List<Stmt> = listOf(),
        val scopedSeqnDeclarations: List<Declaration> = listOf(),
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Seqn = viper.silver.ast.Seqn(
            stmts.toSilver().toScalaSeq(),
            scopedSeqnDeclarations.toSilver().toScalaSeq(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            scopedSeqnDeclarations.forEach { nameResolver.register(it.name) }
            stmts.forEach { it.registerNames() }
        }
    }

    data class If(
        val cond: Exp,
        val then: Seqn,
        val els: Seqn?,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.If = viper.silver.ast.If(
            cond.toSilver(),
            then.toSilver(),
            els?.toSilver() ?: Seqn().toSilver(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            cond.registerNames()
            then.registerNames()
            els?.registerNames()
        }
    }

    data class While(
        val cond: Exp,
        val invariants: List<Exp>,
        val body: Seqn,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.While = viper.silver.ast.While(
            cond.toSilver(),
            invariants.map { it.toSilver() }.toScalaSeq(),
            body.toSilver(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            cond.registerNames()
            body.registerNames()
            invariants.forEach { it.registerNames() }
        }
    }

    data class Label(
        val name: SymbolicName,
        val invariants: List<Exp>,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Label = viper.silver.ast.Label(
            name.mangled,
            invariants.map { it.toSilver() }.toScalaSeq(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(name)
            invariants.forEach { it.registerNames() }
        }
    }

    data class Goto(
        val name: SymbolicName,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Goto = viper.silver.ast.Goto(
            name.mangled,
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(name)
        }
    }

    data class Fold(
        val acc: Exp.PredicateAccess,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Fold = viper.silver.ast.Fold(
            acc.toSilver(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(acc.predicateName)
        }
    }

    data class Unfold(
        val acc: Exp.PredicateAccess,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Unfold = viper.silver.ast.Unfold(
            acc.toSilver(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

        context(nameResolver: NameResolver)
        override fun registerNames() {
            nameResolver.register(acc.predicateName)
        }
    }
}
