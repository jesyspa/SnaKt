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
 *
 * Writing into a summary node cannot single out one path out of the region the summary stands for, so the write is
 * weak there: the summary absorbs [child] instead of replacing anything.
 */
fun UniquenessState.insert(path: Path, child: UniquenessState): UniquenessState =
    when {
        path.isEmpty() -> child
        summarizesDescendants -> copy(data = data.join(child.joinChildren(UniquenessUnifier)))
        else -> {
            val head = path.first()
            copy(
                children = children.put(head, (children[head] ?: EmptyUniquenessState).insert(path.drop(1), child))
            )
        }
    }

/**
 * Collapses every path that revisits a symbol into a summary of the region below the repeat.
 *
 * Assignment projects the substate of the right-hand side under the path of the left-hand side, so a loop over a
 * recursive type can deepen the trie once per iteration - `prev.next`, then `prev.next.next`, and so on - and the
 * chain of states never stabilizes. Cutting the trie wherever a symbol comes round a second time bounds its depth by
 * the number of distinct symbols the function mentions, which makes the state space finite and the fixed point
 * reachable. Any growth that is unbounded has to repeat a symbol, because a loop body can only ever append the finitely
 * many components its own syntax spells out; paths over non-recursive data are therefore left exactly as they are.
 */
fun UniquenessState.summarizeRecursivePaths(): UniquenessState =
    summarizeRecursivePaths(emptySet())

private fun UniquenessState.summarizeRecursivePaths(seen: Set<FirBasedSymbol<*>>): UniquenessState {
    if (summarizesDescendants || children.isEmpty()) return this

    var newChildren = children

    for ((symbol, child) in children) {
        // States are normalized at every node, so most of the time there is nothing to collapse: keep the subtries
        // that come back unchanged rather than rebuilding the trie on each visit.
        val newChild = if (symbol in seen) {
            child.summarizeDescendants(UniquenessUnifier)
        } else {
            child.summarizeRecursivePaths(seen + symbol)
        }

        if (newChild !== child) {
            newChildren = newChildren.put(symbol, newChild)
        }
    }

    return if (newChildren === children) this else copy(children = newChildren)
}
