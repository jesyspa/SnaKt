package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

class ContextTrie(
    val parent: ContextTrie?,
    val symbol: FirBasedSymbol<*>?,
    val children: MutableMap<FirBasedSymbol<*>, ContextTrie>,
    override var level: UniqueLevel
) : UniquePathContext {
    context(context: UniqueCheckerContext) override fun getOrPutPath(path: List<FirBasedSymbol<*>>): ContextTrie {
        if (path.isEmpty()) return this

        val (head, tail) = path.first() to path.drop(1)
        val node = children.getOrPut(head) {
            ContextTrie(this, head, mutableMapOf(), context.resolveUniqueAnnotation(head))
        }
        return node.getOrPutPath(tail)
    }

    private val localVariable: FirBasedSymbol<*>?
        get() {
            val local = parent?.localVariable ?: this.symbol
            return local as? FirValueParameterSymbol
        }

    context(context: UniqueCheckerContext) override val borrowingLevel: BorrowingLevel
        get() = localVariable?.let { context.resolveBorrowingAnnotation(it) } ?: BorrowingLevel.Plain

    override val subtreeLUB: UniqueLevel
        get() = listOfNotNull(
            level,
            children.values.maxOfOrNull { it.subtreeLUB }).max()

    override val pathToRootLUB: UniqueLevel
        get() = listOfNotNull(parent?.pathToRootLUB, level).max()

    context(context: UniqueCheckerContext) override val hasChanges: Boolean
        get() {
            val changed = symbol?.let { level != context.resolveUniqueAnnotation(it) } ?: false
            return changed || children.values.any { it.hasChanges }
        }
}
