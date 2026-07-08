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
 * @property data value attached to the current path prefix.
 * @property children map keyed by the next symbol component.
 */
data class PathTrie<Type>(
    val data: Type,
    val children: PersistentMap<FirBasedSymbol<*>, PathTrie<Type>> = persistentMapOf(),
)

fun <Type> PathTrie<Type>.putChild(symbol: FirBasedSymbol<*>, child: PathTrie<Type>): PathTrie<Type> =
    copy(children = children.put(symbol, child))

val PathTrie<*>.symbols: Sequence<FirBasedSymbol<*>>
    get() = children.keys.asSequence() + children.values.flatMap { it.symbols }

fun <Type> PathTrie<Type>.join(other: PathTrie<Type>, typeUnifier: TypeFactUnifier<Type>): PathTrie<Type> {
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
    val head = path.firstOrNull()

    return if (head != null) {
        children[head]?.find(path.drop(1))
    } else {
        this
    }
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
