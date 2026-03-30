package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toDisjunction
import org.jetbrains.kotlin.formver.viper.ast.Type

class SsaConverter(
    val source: KtSourceElement? = null,
) {
    private var head: SsaBlockNode = SsaBlockNode(SsaStartNode(), Exp.BoolLit(true))
    private val ssaAssignments: MutableList<Pair<SsaVariableName, Exp>> = mutableListOf()
    private val returnExpressions: MutableList<Pair<Exp, Exp>> = mutableListOf()
    private val accessInvariants: MutableMap<SsaVariableName, List<Exp.PredicateAccess>> = mutableMapOf()

    // Produce new ssa names for a source variable name
    private val ssaNameProducers: MutableMap<SymbolicName, FreshEntityProducer<SsaVariableName, SymbolicName>> =
        mutableMapOf()

    fun branch(
        condition: Exp,
        thenBlock: () -> Unit,
        elseBlock: () -> Unit
    ) {
        val splitPoint = head
        head = splitPoint.generateBranchingBlockNodeFromThisNode(condition)
        thenBlock()
        val thenResultHead = head
        head = splitPoint.generateBranchingBlockNodeFromThisNode(Exp.Not(condition))
        elseBlock()
        val joinNode = SsaJoinNode(
            thenResultHead,
            head,
            condition,
            this
        )
        val branchCondition = when {
            thenResultHead.returns && head.returns -> Exp.BoolLit(false)
            thenResultHead.returns -> head.fullBranchingCondition
            head.returns -> thenResultHead.fullBranchingCondition
            else -> listOf(thenResultHead.fullBranchingCondition, head.fullBranchingCondition).toDisjunction()
        }
        head = SsaBlockNode(joinNode, branchCondition)
    }

    fun constructExpression(): Exp {
        if (returnExpressions.isEmpty()) throw SnaktInternalException(
            source,
            "No return expression was found for translation"
        )
        val defaultBody = returnExpressions.last().second
        val bodyExp = returnExpressions.dropLast(1).foldRight(defaultBody) { expPair, elseBranch ->
            Exp.TernaryExp(
                expPair.first,
                expPair.second,
                elseBranch
            )
        }
        return ssaAssignments.foldRight(bodyExp) { assignment, innerScope ->
            Exp.LetBinding(Declaration.LocalVarDecl(assignment.first, Type.Ref), assignment.second, innerScope)
        }
    }

    fun generateFreshSsaName(name: SymbolicName): SsaVariableName {
        val producer = ssaNameProducers.getOrPut(name) { FreshEntityProducer(::SsaVariableName) }
        return producer.getFresh(name)
    }

    fun addAssignment(
        name: SymbolicName,
        varExp: Exp,
        newVarAccessInvariants: List<Exp.PredicateAccess> = emptyList()
    ) {
        val ssaName = head.updateLatestName(name)
        accessInvariants[ssaName] = newVarAccessInvariants
        varExp.propagateAccessInvariants(ssaName)
        addGuardedAssignment(ssaName, varExp.withAccessInvariants(ssaName))
    }

    fun addPhiAssignment(condition: Exp, left: SsaVariableName, right: SsaVariableName, name: SsaVariableName) {
        if (left.baseName != right.baseName) {
            throw SnaktInternalException(
                source,
                "Phi Assignments may only be created for SSA variables referring to the same source variable."
            )
        }
        val phiExpression = Exp.TernaryExp(
            condition,
            Exp.LocalVar(left, Type.Ref),
            Exp.LocalVar(right, Type.Ref)
        )
        phiExpression.propagateAccessInvariants(name)
        addGuardedAssignment(name, phiExpression.withAccessInvariants(name))
    }

    fun addReturn(returnExp: Exp) {
        head.returns = true
        returnExpressions.add(head.fullBranchingCondition to returnExp)
    }

    fun resolveVariableName(name: SymbolicName): SymbolicName {
        return head.resolveVariableName(name)
    }

    private fun addGuardedAssignment(name: SsaVariableName, varExp: Exp) {
        val defaultExpression = varExp.type.defaultExpression() ?: throw SnaktInternalException(
            source,
            "Tried to assign a variable without a default expression"
        )
        if (head.fullBranchingCondition == Exp.BoolLit(true)) {
            ssaAssignments.add(name to varExp)
        } else {
            ssaAssignments.add(name to Exp.TernaryExp(head.fullBranchingCondition, varExp, defaultExpression))
        }
    }

    private fun Exp.withAccessInvariants(name: SsaVariableName): Exp =
        when (this) {
            is Exp.FieldAccess, is Exp.FuncApp, is Exp.DomainFuncApp -> accessInvariants[name]?.foldRight(this) { invariant, acc ->
                Exp.Unfolding(invariant, acc)
            } ?: this

            else -> this
        }

    private fun mergeAccessInvariants(from: List<SymbolicName>, newName: SsaVariableName) {
        val mergedInvariants = from.mapNotNull { accessInvariants[it] }.flatten() + accessInvariants[newName].orEmpty()
        accessInvariants[newName] = mergedInvariants.distinct()
    }

    private fun Exp.propagateAccessInvariants(to: SsaVariableName) {
        var from: Exp? = null
        when (this) {
            is Exp.LocalVar -> from = this
            is Exp.FieldAccess -> from = this.rcv
            is Exp.FuncApp -> this.args.forEach { it.propagateAccessInvariants(to) }
            is Exp.DomainFuncApp -> {
                this.args.forEach { it.propagateAccessInvariants(to) }
            }
            // TODO: Determine how to handle the access invariants of a Ternary
            else -> {}
        }
        if (from == null) return
        if (from !is Exp.LocalVar) throw SnaktInternalException(
            source,
            "Access sources must be local variables $from"
        )
        mergeAccessInvariants(listOf(from.name), to)
    }
}
