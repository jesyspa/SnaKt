package org.jetbrains.kotlin.formver.core.names

import debugMangled
import org.jetbrains.kotlin.formver.viper.Join
import org.jetbrains.kotlin.formver.viper.Lit
import org.jetbrains.kotlin.formver.viper.SymbolName
import org.jetbrains.kotlin.formver.viper.NameExpr
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SEPARATOR

data class Candidate(val expr: NameExpr) {
    context(nameResolver: GraphBasedNameResolver)
    fun getStringRepresentation() : String {
        val sb = StringBuilder()
        expr.toParts().forEach { part ->
            when(part) {
                is NameExpr.Part.Lit -> sb.append(part.value)
                is NameExpr.Part.SymbolVal -> sb.append(nameResolver.resolve(part.symbolName))
            }
        }
        return sb.toString()
    }
}
data class NameBuilder(val name: SymbolName, val candidates: MutableList<Candidate>) {
    var currentIndex: Int = 0
    context(nameResolver: GraphBasedNameResolver)
    fun currentCandidate() : String {
        if (currentIndex >= candidates.size) return candidates.last().getStringRepresentation() + (currentIndex - candidates.size).toString()
        with(nameResolver) {
            return candidates[currentIndex].getStringRepresentation()
        }
    }
    fun addCandidate(candidate: Candidate) { candidates.add(candidate) }
    fun deleteCurrentCandidate() {currentIndex++}
}
val reservedViper = listOf(
    "field", "method", "function", "result", "predicate",
    "domain", "axiom", "define", "requires", "ensures", "acc",
    "package", "unique", "derives"
)
class GraphBasedNameResolver: NameResolver {
    private val builderMap: MutableMap<SymbolName, NameBuilder> = mutableMapOf()
    override fun resolve(name: SymbolName): String {
        if (name !in builderMap) register(name)
        return builderMap[name]?.currentCandidate() ?: name.debugMangled
    }
    override fun register(name: SymbolName) {
        val base: NameExpr = name.mangledBaseName
        val requiredScope: NameExpr? = name.requiredScope
        val fullScope: NameExpr? = name.fullScope
        val typeName: NameExpr = Lit(name.mangledType)

        base.toParts().forEach { it ->
            it.let { it ->
                val symbolName = it as? SymbolName
                symbolName?.let {register(it)}
            }
        }
        requiredScope?.toParts()?.forEach { it ->
            it.let { it ->
                val symbolName = it as? SymbolName
                symbolName?.let {register(it)}
            }
        }
        fun joinExprs(vararg segs: NameExpr?): NameExpr {
            val items = segs
                .filterNotNull()
            return when (items.size) {
                0 -> Lit("")
                1 -> items[0]
                else -> Join(items, SEPARATOR)
            }
        }
        val candidates = mutableListOf<Candidate>()
        val c1 = Candidate(joinExprs(requiredScope, base))
        val c2 = Candidate(joinExprs(typeName, requiredScope, base))
        val c3 = Candidate(joinExprs(typeName, fullScope, base))
        if (!name.requiresType) candidates+=c1
        candidates+=c2
        candidates+=c3
        if (name is ScopedKotlinName && name.name is TypedKotlinNameWithType) {
            val cand = Candidate(joinExprs(typeName, fullScope, base, name.name.additionalType))
            candidates+=cand
        }
        builderMap[name] = NameBuilder(name, candidates)
    }
    fun chooseNamesForFix(): Set<SymbolName> {
        val buckets = mutableMapOf<String, MutableList<SymbolName>>()
        for ((mn, nb) in builderMap) {
            val s = nb.currentCandidate()
            buckets.getOrPut(s) { mutableListOf() }.add(mn)
        }

        val collisions: List<SymbolName> =
            buckets.values.filter { it.size > 1 }.flatten()

        val reservedHits: List<SymbolName> =
            buckets.filterKeys { it in reservedViper }
                .values.flatten()
        reservedHits.forEach { builderMap[it]?.currentCandidate()}
        return (collisions + reservedHits).toSet()
    }
    fun numberOfConflicts(): Int = chooseNamesForFix().size
    fun assignViperNames() {
        while (true) {
            val assignedNames = builderMap.values.toList().map { it.currentCandidate() }.toMutableSet()
            reservedViper.forEach {assignedNames.add(it)}
            if (numberOfConflicts() == 0) {
                break
            }
            val fixSet = chooseNamesForFix()
            fixSet.forEach { name ->
                val builder = builderMap[name]!!
                builder.deleteCurrentCandidate()
            }
        }
    }
}