package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

class ContextTrie(
    val parent: ContextTrie?, val children: MutableMap<FirBasedSymbol<*>, ContextTrie>, override var level: UniqueLevel
) : UniquePathContext {
    context(context: UniqueCheckerContext) override fun getOrPutPath(path: List<FirBasedSymbol<*>>): ContextTrie {
        if (path.isEmpty()) return this

        val (head, tail) = path.first() to path.drop(1)
        val node = children.getOrPut(head) {
            ContextTrie(this, mutableMapOf(), context.resolveUniqueAnnotation(head))
        }
        return node.getOrPutPath(tail)
    }

    override val subtreeLUB: UniqueLevel
        get() = listOfNotNull(
            level,
            children.values.maxOfOrNull { it.subtreeLUB }).max()

    override val pathLUB: UniqueLevel
        get() = listOfNotNull(parent?.pathLUB, level).max()
}
