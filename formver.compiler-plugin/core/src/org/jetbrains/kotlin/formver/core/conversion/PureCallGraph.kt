/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.callables.CompleteFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FunctionCall
import org.jetbrains.kotlin.formver.viper.SymbolicName

/**
 * Which pure functions of a program participate in a cycle of calls, and so may recurse.
 *
 * Termination is a proof obligation only for those; the rest terminate by induction over the
 * acyclic remainder, provided the cyclic ones do. The distinction decides which measure a function
 * is given, and giving a cyclic function a wildcard would leave the induction without a base.
 *
 * Specification expressions are edges like any other: a postcondition that mentions its own
 * function is a cycle Viper rejects as inconsistent, and treating it as acyclic would hide that.
 */
class PureCallGraph private constructor(private val cyclic: Set<SymbolicName>) {
    fun mayRecurse(name: SymbolicName): Boolean = name in cyclic

    companion object {
        fun of(
            signatures: Map<SymbolicName, CompleteFunctionSignature>,
            bodies: ConvertedBodyResolver,
        ): PureCallGraph {
            val callees = signatures.mapValues { (name, signature) ->
                val roots = signature.preconditions + signature.postconditions + listOfNotNull(bodies.lookupPure(name))
                roots.flatMap { it.calls() }.filterTo(mutableSetOf()) { it in signatures }
            }
            return PureCallGraph(callees.keys.filterTo(mutableSetOf()) { it.reaches(it, callees) })
        }

        /** Whether [target] is reachable from this name over one or more edges. */
        private fun SymbolicName.reaches(
            target: SymbolicName,
            callees: Map<SymbolicName, Set<SymbolicName>>,
        ): Boolean {
            val seen = mutableSetOf<SymbolicName>()
            val pending = ArrayDeque(callees[this].orEmpty())
            while (true) {
                val next = pending.removeFirstOrNull() ?: return false
                if (next == target) return true
                if (seen.add(next)) pending.addAll(callees[next].orEmpty())
            }
        }

        private fun ExpEmbedding.calls(): Sequence<SymbolicName> = sequence {
            if (this@calls is FunctionCall) yield(function.name)
            yieldAll(children().flatMap { it.calls() })
        }
    }
}
