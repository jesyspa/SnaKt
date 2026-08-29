/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.type.plugin.TypeFactIntersector
import org.jetbrains.kotlin.formver.type.plugin.TypeFactUnifier

/**
 * Persistent prefix trie of symbol components.
 *
 * A child entry `children[symbol]` represents the subtrie for all paths whose next component (relative to the current
 * node prefix) is `symbol`. Sibling children therefore represent alternative next FIR symbols under the same prefix.
 *
 * For example, for a FIR access path like `a.b.c`, the trie contains:
 * `children[a] -> children[b] -> children[c]`.
 *
 * A node may instead stand for a whole region of the path space: when [summarizesDescendants] is set, [data] is the
 * join of the values of every path under the node, the node carries no children, and a lookup that would descend past
 * it lands on the node itself. Summary nodes are what keeps the trie finite over recursive types; see
 * [UniquenessState.summarizeRecursivePaths].
 *
 * @property data value attached to the current path prefix, and to everything below it when [summarizesDescendants].
 * @property children map keyed by the next symbol component; empty when [summarizesDescendants].
 * @property summarizesDescendants whether this node stands for every path below it as well as for its own prefix.
 */
data class PathTrie<Type>(
    val data: Type,
    val children: PersistentMap<FirBasedSymbol<*>, PathTrie<Type>> = persistentMapOf(),
    val summarizesDescendants: Boolean = false,
)

fun <Type> PathTrie<Type>.putChild(symbol: FirBasedSymbol<*>, child: PathTrie<Type>): PathTrie<Type> =
    copy(children = children.put(symbol, child))

val PathTrie<*>.symbols: Sequence<FirBasedSymbol<*>>
    get() = children.keys.asSequence() + children.values.flatMap { it.symbols }

/**
 * Replaces this subtrie by a single summary node covering it: a leaf whose value is the join of every value in the
 * subtrie, standing for the node's own prefix and for all paths below it.
 */
fun <Type> PathTrie<Type>.summarizeDescendants(typeUnifier: TypeFactUnifier<Type>): PathTrie<Type> =
    if (summarizesDescendants) this else PathTrie(joinChildren(typeUnifier), summarizesDescendants = true)

fun <Type> PathTrie<Type>.join(other: PathTrie<Type>, typeUnifier: TypeFactUnifier<Type>): PathTrie<Type> {
    // A summary on either side absorbs the other side's children: the result has to stand for every path that either
    // input stands for, and only a summary can do that for the paths the summary itself no longer spells out.
    if (summarizesDescendants || other.summarizesDescendants) {
        return PathTrie(
            typeUnifier.join(joinChildren(typeUnifier), other.joinChildren(typeUnifier)),
            summarizesDescendants = true,
        )
    }

    var joinedChildren = children

    for ((symbol, otherChild) in other.children) {
        val child = joinedChildren[symbol]

        joinedChildren = joinedChildren.put(
            symbol,
            child?.join(otherChild, typeUnifier) ?: otherChild,
        )
    }

    return copy(
        data = typeUnifier.join(data, other.data),
        children = joinedChildren
    )
}

fun <Type> PathTrie<Type>.meet(other: PathTrie<Type>, typeIntersector: TypeFactIntersector<Type>): PathTrie<Type> {
    var metChildren = persistentMapOf<FirBasedSymbol<*>, PathTrie<Type>>()

    for ((symbol, child) in children) {
        val otherChild = other.children[symbol] ?: continue
        metChildren = metChildren.put(symbol, child.meet(otherChild, typeIntersector))
    }

    return copy(
        data = typeIntersector.meet(data, other.data),
        children = metChildren
    )
}

fun <Type> PathTrie<Type>.joinChildren(typeUnifier: TypeFactUnifier<Type>): Type {
    var joinedData = data

    for (child in children.values) {
        joinedData = typeUnifier.join(joinedData,child.joinChildren(typeUnifier))
    }

    return joinedData
}

fun <Type> PathTrie<Type>.find(path: List<FirBasedSymbol<*>>): PathTrie<Type>? {
    val head = path.firstOrNull() ?: return this
    val child = children[head] ?: return this.takeIf { summarizesDescendants }

    return child.find(path.drop(1))
}

fun <Type> PathTrie<Type>.enumerate(isTerminal: PathTrie<Type>.() -> Boolean): Sequence<List<FirBasedSymbol<*>>> =
    enumerate(emptyList(), isTerminal)

fun <Type> PathTrie<Type>.enumerate(
    prefix: List<FirBasedSymbol<*>>,
    isTerminal: PathTrie<Type>.() -> Boolean
): Sequence<Path> =
    if (children.isEmpty()) {
        sequenceOf()
    } else {
        sequence {
            for ((key, child) in children) {
                val newPrefix = prefix + key

                if (child.isTerminal()) {
                    yield(newPrefix)
                }

                yieldAll(child.enumerate(newPrefix, isTerminal))
            }
        }
    }
