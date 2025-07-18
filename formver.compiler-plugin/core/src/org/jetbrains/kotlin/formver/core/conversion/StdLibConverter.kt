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
import org.jetbrains.kotlin.formver.core.embeddings.properties.ListSizeFieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.isInheritorOfCollectionTypeNamed
import org.jetbrains.kotlin.formver.core.names.NameMatcher

private fun VariableEmbedding.sameSize(): ExpEmbedding =
    EqCmp(FieldAccess(this, ListSizeFieldEmbedding), Old(FieldAccess(this, ListSizeFieldEmbedding)))

private fun VariableEmbedding.increasedSize(amount: Int): ExpEmbedding =
    EqCmp(
        FieldAccess(this, ListSizeFieldEmbedding),
        OperatorExpEmbeddings.AddIntInt(Old(FieldAccess(this, ListSizeFieldEmbedding)), IntLit(amount)),
    )

sealed interface StdLibReceiverInterface {
    fun match(function: NamedFunctionSignature): Boolean
}

sealed interface PresentInterface : StdLibReceiverInterface {
    val interfaceName: String
    override fun match(function: NamedFunctionSignature): Boolean =
        function.callableType.dispatchReceiverType?.pretype?.isInheritorOfCollectionTypeNamed(interfaceName) ?: false
}

data object CollectionInterface : PresentInterface {
    override val interfaceName = "Collection"
}

data object ListInterface : PresentInterface {
    override val interfaceName = "List"
}

data object MutableListInterface : PresentInterface {
    override val interfaceName = "MutableList"
}

data object NoInterface : StdLibReceiverInterface {
    override fun match(function: NamedFunctionSignature): Boolean =
        NameMatcher.matchClassScope(function.name) {
            ifInCollectionsPkg {
                ifNoReceiver {
                    return true
                }
            }
            return false
        }
}

sealed interface StdLibCondition {
    val stdLibInterface: StdLibReceiverInterface
    val functionName: String

    fun match(function: NamedFunctionSignature): Boolean {
        NameMatcher.matchClassScope(function.name) {
            ifFunctionName(functionName) {
                return true
            }
            return false
        }
    }
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
            AddPostcondition
        )
    }

    fun getEmbeddings(returnVariable: VariableEmbedding, function: NamedFunctionSignature): List<ExpEmbedding>
}

data object GetPrecondition : StdLibPrecondition {
    override fun getEmbeddings(function: NamedFunctionSignature): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        val indexArg = function.formalArgs[1]
        return listOf(
            GeIntInt(
                indexArg,
                IntLit(0),
                SourceRole.ListElementAccessCheck(SourceRole.ListElementAccessCheck.AccessCheckType.LESS_THAN_ZERO)
            ),
            GtIntInt(
                FieldAccess(receiver, ListSizeFieldEmbedding),
                indexArg,
                SourceRole.ListElementAccessCheck(SourceRole.ListElementAccessCheck.AccessCheckType.GREATER_THAN_LIST_SIZE)
            ),
        )
    }

    override val stdLibInterface = ListInterface
    override val functionName = "get"
}

data object SubListPrecondition : StdLibPrecondition {
    override fun getEmbeddings(function: NamedFunctionSignature): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        val fromIndexArg = function.formalArgs[1]
        val toIndexArg = function.formalArgs[2]
        return listOf(
            LeIntInt(fromIndexArg, toIndexArg, SourceRole.SubListCreation.CheckInSize),
            GeIntInt(fromIndexArg, IntLit(0), SourceRole.SubListCreation.CheckNegativeIndices),
            LeIntInt(toIndexArg, FieldAccess(receiver, ListSizeFieldEmbedding), SourceRole.SubListCreation.CheckInSize)
        )
    }

    override val stdLibInterface = ListInterface
    override val functionName = "subList"
}

data object EmptyListPostcondition : StdLibPostcondition {
    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> {
        return listOf(
            EqCmp(FieldAccess(returnVariable, ListSizeFieldEmbedding), IntLit(0))
        )
    }

    override val stdLibInterface = NoInterface
    override val functionName = "emptyList"
}

data object IsEmptyPostcondition : StdLibPostcondition {
    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        return listOf(
            receiver.sameSize(),
            Implies(returnVariable, EqCmp(FieldAccess(receiver, ListSizeFieldEmbedding), IntLit(0))),
            Implies(Not(returnVariable), GtIntInt(FieldAccess(receiver, ListSizeFieldEmbedding), IntLit(0)))
        )
    }

    override val stdLibInterface = CollectionInterface
    override val functionName = "isEmpty"
}

data object GetPostcondition : StdLibPostcondition {
    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> {
        return listOf(function.dispatchReceiver!!.sameSize())
    }

    override val stdLibInterface = ListInterface
    override val functionName = "get"
}

data object SubListPostcondition : StdLibPostcondition {
    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> {
        val fromIndexArg = function.formalArgs[1]
        val toIndexArg = function.formalArgs[2]
        return listOf(
            function.dispatchReceiver!!.sameSize(),
            EqCmp(FieldAccess(returnVariable, ListSizeFieldEmbedding), SubIntInt(toIndexArg, fromIndexArg))
        )
    }

    override val stdLibInterface = ListInterface
    override val functionName = "subList"
}

data object AddPostcondition : StdLibPostcondition {
    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> {
        return listOf(function.dispatchReceiver!!.increasedSize(1))
    }

    override val stdLibInterface = MutableListInterface
    override val functionName = "add"
}

fun NamedFunctionSignature.stdLibPreconditions(): List<ExpEmbedding> {
    StdLibPrecondition.all.groupBy {
        it.stdLibInterface
    }.forEach { (stdLibInterface, preconditions) ->
        if (stdLibInterface.match(this)) {
            preconditions.forEach {
                if (it.match(this)) {
                    return it.getEmbeddings(this)
                }
            }
        }
    }
    return listOf()
}


fun NamedFunctionSignature.stdLibPostconditions(returnVariable: VariableEmbedding): List<ExpEmbedding> {
    StdLibPostcondition.all.groupBy {
        it.stdLibInterface
    }.forEach { (stdLibInterface, postconditions) ->
        if (stdLibInterface.match(this)) {
            postconditions.forEach {
                if (it.match(this)) {
                    return it.getEmbeddings(returnVariable, this)
                }
            }
        }
    }
    return listOf()
}
