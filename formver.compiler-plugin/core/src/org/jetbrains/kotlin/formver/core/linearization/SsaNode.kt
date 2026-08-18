package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * A node in a SSA-Graph
 */
sealed interface SsaNode {
    /**
     * Function resolves source names to their SSAVariableName
     * Fallsback to provided name if no such name is found
     */
    fun resolveVariableName(name: SymbolicName): SymbolicName
}

class SsaStartNode : SsaNode {
    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        name
}

class SsaBlockNode(
    private val predecessor: SsaNode,
    val fullBranchingCondition: Exp,
) : SsaNode {
    val latestName: MutableMap<SymbolicName, SsaVariableName> = mutableMapOf()
    var returns: Boolean = false
        private set

    fun markAsReturning() {
        returns = true
    }

    fun generateBranchingBlockNodeFromThisNode(condition: Exp): SsaBlockNode =
        SsaBlockNode(
            this,
            if (fullBranchingCondition == Exp.BoolLit(true)) condition else Exp.And(fullBranchingCondition, condition),
        )

    context(ssaConverter: SsaConverter)
    fun updateLatestName(name: SymbolicName): SsaVariableName =
        ssaConverter.generateFreshSsaName(name).also { latestName[name] = it }

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        latestName[name] ?: predecessor.resolveVariableName(name)
}

class SsaJoinNode(
    private val leftPredecessor: SsaBlockNode,
    private val rightPredecessor: SsaBlockNode,
    private val mostRecentBranchingCondition: Exp,
    private val ssaConverter: SsaConverter
) : SsaNode {
    private val lookupCache: MutableMap<SymbolicName, SymbolicName> = mutableMapOf()

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        lookupCache[name] ?: resolveNameFromPredecessors(name)

    private fun resolveNameFromPredecessors(name: SymbolicName): SymbolicName {
        val leftIncoming = leftPredecessor.resolveVariableName(name)
        val rightIncoming = rightPredecessor.resolveVariableName(name)
        return if (rightIncoming == leftIncoming) {
            leftIncoming
        } else if (leftIncoming is SsaVariableName && rightIncoming is SsaVariableName) {
            val ssaName = ssaConverter.generateFreshSsaName(leftIncoming.baseName)
            ssaConverter.addPhiAssignment( // Resolve to phi assignment
                mostRecentBranchingCondition,
                leftIncoming,
                rightIncoming,
                ssaName
            )
            ssaName
        } else {
            throw SnaktInternalException(
                ssaConverter.source,
                "Phi Assignments may only be created for SSA variables"
            )
        }
    }
}
