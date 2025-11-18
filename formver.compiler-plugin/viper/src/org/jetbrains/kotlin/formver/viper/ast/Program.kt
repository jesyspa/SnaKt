/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*

data class Program(
    val domains: List<Domain>,
    val fields: List<Field>,
    val functions: List<Function>,
    val predicates: List<Predicate>,
    val methods: List<Method>,
    /* no extensions */
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Program> {
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.Program = viper.silver.ast.Program(
        domains.sortedBy { it.name.mangled }.toSilver().toScalaSeq(),
        fields.sortedBy { it.name.mangled }.toSilver().toScalaSeq(),
        functions.sortedBy { it.name.mangled }.toSilver().toScalaSeq(),
        predicates.sortedBy { it.name.mangled }.toSilver().toScalaSeq(),
        methods.sortedBy { it.name.mangled }.toSilver().toScalaSeq(),
        emptySeq(), /* extensions */
        pos.toSilver(),
        info.toSilver(),
        trafos.toSilver(),
    )

    fun toShort(): Program = Program(
        domains.filter { it.includeInShortDump },
        fields.filter { it.includeInShortDump },
        functions.filter { it.includeInDumpPolicy != IncludeInDumpPolicy.ONLY_IN_FULL_DUMP },
        predicates.filter { it.includeInDumpPolicy != IncludeInDumpPolicy.ONLY_IN_FULL_DUMP },
        methods.filter { it.includeInShortDump },
        pos,
        info,
        trafos,
    )

    fun withoutPredicates(): Program = copy(
        predicates = predicates.filter { it.includeInDumpPolicy == IncludeInDumpPolicy.ALWAYS },
        functions = functions.filter { it.includeInDumpPolicy == IncludeInDumpPolicy.ALWAYS }
    )

    context(nameResolver: NameResolver)
    fun toDebugOutput(): String = toSilver().toString()
}
context(nameResolver: NameResolver)
private fun registerExpNames(exp: Exp) {
    when (exp) {
        is Exp.LocalVar -> nameResolver.register(exp.name)
        is Exp.FieldAccess -> {
            nameResolver.register(exp.field.name)
            registerExpNames(exp.rcv)
        }
        is Exp.PredicateAccess -> {
            nameResolver.register(exp.predicateName)
            exp.formalArgs.forEach { registerExpNames(it) }
        }
        is BinaryExp -> {
            registerExpNames(exp.left)
            registerExpNames(exp.right)
        }
        is Exp.FuncApp -> {
            nameResolver.register(exp.functionName)
            exp.args.forEach { registerExpNames(it) }
        }
        is Exp.DomainFuncApp -> {
            nameResolver.register(exp.function.name)
            exp.function.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
            exp.args.forEach { registerExpNames(it) }
        }
        else -> { }
    }
}

context(nameResolver: NameResolver)
private fun Program.registerSeqnNames(seqn: Stmt.Seqn) {
    seqn.scopedSeqnDeclarations.forEach { decl ->
        when (decl) {
            is Declaration.LocalVarDecl -> nameResolver.register(decl.name)
            is Declaration.LabelDecl -> nameResolver.register(decl.name)
        }
    }
    seqn.stmts.forEach { stmt ->
        when (stmt) {
            is Stmt.Label -> nameResolver.register(stmt.name)
            is Stmt.Seqn -> registerSeqnNames(stmt)
            is Stmt.Goto -> nameResolver.register(stmt.name)
            is Stmt.MethodCall -> nameResolver.register(stmt.methodName)
            is Stmt.If -> {
                registerSeqnNames(stmt.then)
                stmt.els?.let { registerSeqnNames(it) }
            }
            is Stmt.While -> registerSeqnNames(stmt.body)
            is Stmt.LocalVarAssign -> nameResolver.register(stmt.lhs.name)
            is Stmt.Fold -> nameResolver.register(stmt.acc.predicateName)
            is Stmt.Unfold -> nameResolver.register(stmt.acc.predicateName)
            else -> { }
        }
    }
}

context(nameResolver: NameResolver)
fun Program.registerAllNames() {
    domains.forEach { domain ->
        // Call specialized registration for domains that implement it (e.g., RuntimeTypeDomain)
        (domain as? org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain)?.registerDomain()

        nameResolver.register(domain.name)
        domain.functions.forEach { function ->
            nameResolver.register(function.name)
            function.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
        }
    }
    fields.forEach { nameResolver.register(it.name) }

    functions.forEach { function ->
        nameResolver.register(function.name)
        function.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
        function.body?.let { exp -> registerExpNames(exp) }
    }

    predicates.forEach { predicate ->
        nameResolver.register(predicate.name)
        predicate.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
        registerExpNames(predicate.body)
    }

    methods.forEach { method ->
        nameResolver.register(method.name)
        method.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
        method.formalReturns.forEach { ret -> nameResolver.register(ret.name) }
        method.body?.let { seqn -> registerSeqnNames(seqn) }
    }
}
