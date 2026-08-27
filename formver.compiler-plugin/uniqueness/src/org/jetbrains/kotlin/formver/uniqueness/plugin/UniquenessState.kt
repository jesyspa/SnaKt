package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.type.plugin.TypeFactIntersector
import org.jetbrains.kotlin.formver.type.plugin.TypeFactUnifier

typealias UniquenessState = PathTrie<Uniqueness>

val EmptyUniquenessState = UniquenessState(Uniqueness.Unique)

/**
 * Performs the join of two [UniquenessState]s.
 */
fun UniquenessState.join(other: UniquenessState): UniquenessState =
    join(other, UniquenessUnifier)

/**
 * Joins the uniqueness values along [path], using [Uniqueness.Unique] for missing path components.
 */
fun UniquenessState.joinOverPath(path: List<FirBasedSymbol<*>>): Uniqueness =
    data.join((children[path.first()]?.joinOverPath(path.drop(1)) ?: Uniqueness.Unique))

/**
 * Enumerates the paths whose uniqueness state is [Uniqueness.Moved].
 */
fun UniquenessState.enumerateInconsistentPaths(): Sequence<Path> =
    enumerate(emptyList()) { data == Uniqueness.Moved }

/**
 * Replaces the substate at [path] with [child].
 */
fun UniquenessState.insert(path: Path, child: UniquenessState): UniquenessState =
    if (path.isEmpty()) {
        child
    } else {
        val head = path.first()
        copy(
            children = children.put(head, (children[head] ?: EmptyUniquenessState).insert(path.drop(1), child))
        )
    }
