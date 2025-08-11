package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.uniqueness.UniqueLevel.*

/**
 * Stores unique context for each path in a trie structure.
 *
 * Each path represents a sequence of symbols like x.y.z, where each node has a unique level annotation ([Bottom]/[Unique]/[Shared]/[Top])
 *
 * Example:
 * ```
 *     x.y.w => local/x --> A/y -> B/w -> ..
 *                 ^     |
 *     x.z.w ======+     +-> A/z -> C/w -> ...
 * ```
 */
interface UniquePathContext {
    var level: UniqueLevel

    /**
     * Retrieves the child node corresponding to the given path within the trie structure,
     * or creates and inserts a new path if it does not already exist.
     * This method uses the provided context to resolve unique annotations for new nodes.
     *
     * @param path A list of [FirBasedSymbol] items representing the path to traverse or create in the trie.
     *             Each symbol corresponds to a hierarchical level in the path.
     * @return The [ContextTrie] node at the end of the given path, creating intermediate nodes as necessary.
     */
    context(context: UniqueCheckerContext) fun getOrPutPath(path: List<FirBasedSymbol<*>>): UniquePathContext

    val subtreeLUB: UniqueLevel
    val pathLUB: UniqueLevel
}