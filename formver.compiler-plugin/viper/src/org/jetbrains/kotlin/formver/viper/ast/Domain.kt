/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*
import viper.silver.ast.AnonymousDomainAxiom
import viper.silver.ast.NamedDomainAxiom


data class DomainFunc(
    val name: SymbolicName,
    val domainName: SymbolicName,
    val formalArgs: List<Declaration.LocalVarDecl>,
    /** The type variables of the domain this function belongs to; they are declared there, not here. */
    val typeArgs: List<Type.TypeVar>,
    val returnType: Type,
    val unique: Boolean,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
) : IntoSilver<viper.silver.ast.DomainFunc>, Applicable {
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.DomainFunc =
        viper.silver.ast.DomainFunc(
            name.mangled,
            formalArgs.map { it.toSilver() }.toScalaSeq(),
            returnType.toSilver(),
            unique,
            null.toScalaOption(),
            pos.toSilver(),
            info.toSilver(),
            domainName.mangled,
            silverNoTrafos
        )

    /**
     * Applies the function without instantiating the domain, mapping every type variable to itself.
     *
     * That is the right reading only inside the domain's own axioms, where the type variables are
     * still in scope. [Applicable] has no room for an instantiation, so a call site that does
     * instantiate the domain has to go through [Domain.funcApp] and pass the map.
     */
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info): Exp.DomainFuncApp =
        Exp.DomainFuncApp(this, args, typeArgs.associateWith { it }, pos, info)
}

class DomainAxiom(
    val name: SymbolicName?,
    val domainName: SymbolicName,
    val exp: Exp,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
) : IntoSilver<viper.silver.ast.DomainAxiom> {
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.DomainAxiom =
        when (name) {
            null -> AnonymousDomainAxiom(
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                domainName.mangled,
                silverNoTrafos
            )

            else -> NamedDomainAxiom(
                name.mangled,
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                domainName.mangled,
                silverNoTrafos
            )
        }
}

abstract class Domain(
    val name: SymbolicName,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
) : IntoSilver<viper.silver.ast.Domain> {


    open val includeInShortDump: Boolean = true
    abstract val typeVars: List<Type.TypeVar>
    abstract val functions: List<DomainFunc>
    abstract val axioms: List<DomainAxiom>
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.Domain =
        viper.silver.ast.Domain(
            name.mangled,
            functions.toSilver().toScalaSeq(),
            axioms.toSilver().toScalaSeq(),
            // Can't use List.toViper directly here as the type would end up being `List<Type>` instead of `List<TypeVar`.
            typeVars.map { it.toSilver() }.toScalaSeq(),
            null.toScalaOption(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )

    /**
     * Applies one of this domain's functions, instantiating the domain's type variables as [typeVarMap] says.
     *
     * The map is not defaulted: mapping the type variables to themselves is only meaningful inside
     * this domain's own axioms, and that case is [DomainFunc.toFuncApp]'s. Choosing the wrong
     * instantiation is not diagnosed anywhere downstream, so the choice is made here, explicitly.
     */
    fun funcApp(
        func: DomainFunc,
        args: List<Exp>,
        typeVarMap: Map<Type.TypeVar, Type>,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
    ): Exp.DomainFuncApp {
        require(func.domainName == name) { "Function ${func.name} does not belong to domain $name." }
        return Exp.DomainFuncApp(func, args, typeVarMap, pos, info)
    }
}

abstract class BuiltinDomain(
    baseName: SymbolicName,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
) : Domain(baseName, pos, info) {
    override val includeInShortDump: Boolean = false
}
