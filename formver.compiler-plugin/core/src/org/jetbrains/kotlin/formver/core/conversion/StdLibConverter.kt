/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GeIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GtIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.Implies
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.LeIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.Not
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.SubIntInt
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.names.SpecialPackages

private fun VariableEmbedding.sameSize(): ExpEmbedding =
    EqCmp(FieldAccess(this, CollectionSizeFieldEmbedding), Old(FieldAccess(this, CollectionSizeFieldEmbedding)))

private fun VariableEmbedding.increasedSize(amount: Int): ExpEmbedding = EqCmp(
    FieldAccess(this, CollectionSizeFieldEmbedding),
    OperatorExpEmbeddings.AddIntInt(Old(FieldAccess(this, CollectionSizeFieldEmbedding)), IntLit(amount)),
)

/**
 * Matches any parameter whose pretype is a subtype of `[pkg].[paramTypeInherits]`.
 * If the parameter type is nullable, the generated embeddings are wrapped as
 * `param != null implies condition`.
 */
data class StdLibParamSpec(
    val pkg: List<String>,
    val paramTypeInherits: String,
) {
    fun matches(paramPretype: PretypeEmbedding, typeResolver: TypeResolver): Boolean =
        typeResolver.isInheritorOf(paramPretype, pkg, paramTypeInherits)
}

sealed interface StdLibCondition {
    val conditions: List<FunctionCondition>
    fun matches(function: NamedFunctionSignature, typeResolver: TypeResolver): Boolean =
        with(typeResolver) { conditions.all { it.matches(function) } }
}

sealed interface StdLibPrecondition : StdLibCondition {
    companion object {
        val all = listOf(GetPrecondition, SubListPrecondition)
    }

    fun getEmbeddings(function: NamedFunctionSignature): List<ExpEmbedding>
}

sealed interface StdLibPostcondition : StdLibCondition {
    companion object {
        val all = listOf(
            EmptyListPostcondition,
            IsEmptyPostcondition,
            GetPostcondition,
            SubListPostcondition,
            AddPostcondition,
        )
    }

    fun getEmbeddings(returnVariable: VariableEmbedding, function: NamedFunctionSignature): List<ExpEmbedding>
}

sealed interface StdLibParamPrecondition {
    val spec: StdLibParamSpec
    fun matches(paramPretype: PretypeEmbedding, typeResolver: TypeResolver): Boolean =
        spec.matches(paramPretype, typeResolver)

    fun getEmbeddings(param: VariableEmbedding): List<ExpEmbedding>

    companion object {
        val all: List<StdLibParamPrecondition> = listOf()
    }
}

sealed interface StdLibParamPostcondition {
    val spec: StdLibParamSpec
    fun matches(paramPretype: PretypeEmbedding, typeResolver: TypeResolver): Boolean =
        spec.matches(paramPretype, typeResolver)

    fun getEmbeddings(returnVariable: VariableEmbedding, param: VariableEmbedding): List<ExpEmbedding>

    companion object {
        val all: List<StdLibParamPostcondition> = listOf()
    }
}

data object GetPrecondition : StdLibPrecondition {
    override val conditions = listOf(
        ReceiverSatisfies(listOf(IsSubtype(SpecialPackages.collections, "List"))),
        HasFunctionName("get"),
    )

    override fun getEmbeddings(function: NamedFunctionSignature): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        val indexArg = function.params[0]
        return listOf(
            GeIntInt(
                indexArg,
                IntLit(0),
                SourceRole.ListElementAccessCheck(SourceRole.ListElementAccessCheck.AccessCheckType.LESS_THAN_ZERO)
            ),
            GtIntInt(
                FieldAccess(receiver, CollectionSizeFieldEmbedding),
                indexArg,
                SourceRole.ListElementAccessCheck(SourceRole.ListElementAccessCheck.AccessCheckType.GREATER_THAN_LIST_SIZE)
            ),
        )
    }
}

data object SubListPrecondition : StdLibPrecondition {
    override val conditions = listOf(
        ReceiverSatisfies(listOf(IsSubtype(SpecialPackages.collections, "List"))),
        HasFunctionName("subList"),
    )

    override fun getEmbeddings(function: NamedFunctionSignature): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        val fromIndexArg = function.params[0]
        val toIndexArg = function.params[1]
        return listOf(
            LeIntInt(fromIndexArg, toIndexArg, SourceRole.SubListCreation.CheckInSize),
            GeIntInt(fromIndexArg, IntLit(0), SourceRole.SubListCreation.CheckNegativeIndices),
            LeIntInt(
                toIndexArg,
                FieldAccess(receiver, CollectionSizeFieldEmbedding),
                SourceRole.SubListCreation.CheckInSize
            )
        )
    }
}

data object EmptyListPostcondition : StdLibPostcondition {
    override val conditions = listOf(
        InPackage(SpecialPackages.collections),
        HasNoReceiver,
        HasFunctionName("emptyList"),
    )

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> = listOf(EqCmp(FieldAccess(returnVariable, CollectionSizeFieldEmbedding), IntLit(0)))
}

data object IsEmptyPostcondition : StdLibPostcondition {
    override val conditions = listOf(
        ReceiverSatisfies(listOf(IsSubtype(SpecialPackages.collections, "Collection"))),
        HasFunctionName("isEmpty"),
    )

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        return listOf(
            receiver.sameSize(),
            Implies(returnVariable, EqCmp(FieldAccess(receiver, CollectionSizeFieldEmbedding), IntLit(0))),
            Implies(Not(returnVariable), GtIntInt(FieldAccess(receiver, CollectionSizeFieldEmbedding), IntLit(0)))
        )
    }
}

data object GetPostcondition : StdLibPostcondition {
    override val conditions = listOf(
        ReceiverSatisfies(listOf(IsSubtype(SpecialPackages.collections, "List"))),
        HasFunctionName("get"),
    )

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> = listOf(function.dispatchReceiver!!.sameSize())
}

data object SubListPostcondition : StdLibPostcondition {
    override val conditions = listOf(
        ReceiverSatisfies(listOf(IsSubtype(SpecialPackages.collections, "List"))),
        HasFunctionName("subList"),
    )

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> {
        val fromIndexArg = function.params[0]
        val toIndexArg = function.params[1]
        return listOf(
            function.dispatchReceiver!!.sameSize(),
            EqCmp(FieldAccess(returnVariable, CollectionSizeFieldEmbedding), SubIntInt(toIndexArg, fromIndexArg))
        )
    }
}

data object AddPostcondition : StdLibPostcondition {
    override val conditions = listOf(
        ReceiverSatisfies(listOf(IsSubtype(SpecialPackages.collections, "MutableList"))),
        HasFunctionName("add"),
    )

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> = listOf(function.dispatchReceiver!!.increasedSize(1))
}

fun NamedFunctionSignature.stdLibPreconditions(ctx: TypeResolver): List<ExpEmbedding> {
    val fromFunction = StdLibPrecondition.all.filter { it.matches(this, ctx) }.flatMap { it.getEmbeddings(this) }
    val fromParams = params.flatMap { param ->
        StdLibParamPrecondition.all.filter { it.matches(param.type.pretype, ctx) }.flatMap { it.getEmbeddings(param) }
            .map { if (param.type.isNullable) Implies(param.notNullCmp(), it) else it }
    }
    return fromFunction + fromParams
}

fun NamedFunctionSignature.stdLibPostconditions(
    returnVariable: VariableEmbedding,
    ctx: TypeResolver,
): List<ExpEmbedding> {
    val fromFunction =
        StdLibPostcondition.all.filter { it.matches(this, ctx) }.flatMap { it.getEmbeddings(returnVariable, this) }
    val fromParams = params.flatMap { param ->
        StdLibParamPostcondition.all.filter { it.matches(param.type.pretype, ctx) }
            .flatMap { it.getEmbeddings(returnVariable, param) }
            .map { if (param.type.isNullable) Implies(param.notNullCmp(), it) else it }
    }
    return fromFunction + fromParams
}
