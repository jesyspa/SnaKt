package org.jetbrains.kotlin.formver.core.names

import NameScope
import debugMangled
import org.jetbrains.kotlin.formver.viper.Lit
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.NameExpr
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.joinExprs

val reservedViperNames = setOf(
    "field", "method", "function", "result", "predicate",
    "domain", "axiom", "define", "requires", "ensures", "acc",
    "package", "unique", "derives"
)

data class Candidate(val expr: NameExpr?) {
    context(nameResolver: GraphBasedNameResolver)
    fun getStringRepresentation(): String {
        val stringRepresentation = StringBuilder()
        expr?.toParts()?.forEach { part ->
            when (part) {
                is NameExpr.Part.Lit -> stringRepresentation.append(part.value)
                is NameExpr.Part.SymbolVal -> {
                    stringRepresentation.append(nameResolver.resolve(part.symbolicName))
                }
            }
        }
        return stringRepresentation.toString()
    }
}

data class NameBuilder(val name: SymbolicName) {
    companion object {
        var currentSuffix = 0
    }
    var currentIndex: Int = 0
    val candidates: MutableList<Candidate> = mutableListOf()

    fun buildCandidates() {
        val base: NameExpr = name.mangledBaseName

        val requiredScope: NameExpr? = name.requiredScope
        val fullScope: NameExpr? = name.fullScope
        val typeName: NameExpr = Lit(name.mangledType)

        if (!name.requiresType) {
            addCandidate(
                Candidate(joinExprs(requiredScope, base))
            )
        }
        addCandidate(
            Candidate(joinExprs(typeName, requiredScope, base))
        )
        addCandidate(
            Candidate(joinExprs(typeName, fullScope, base))
        )

        // example:
        // name - fun f(val x: String) : Nothing
        // additional type - String_Nothing
        if (name is ScopedKotlinName && name.name is TypedKotlinNameWithType) {
            addCandidate(
                Candidate(joinExprs(typeName, fullScope, base, name.name.additionalType))
            )
        }
    }

    context(nameResolver: GraphBasedNameResolver)
    fun currentCandidate(): String {
        if (currentIndex >= candidates.size) {
            val lastName = candidates.last()
            repeat(currentIndex - candidates.size + 1) { i ->
                addCandidate(Candidate(joinExprs(lastName.expr, Lit(currentSuffix.toString()))))
            }
        }
        with(nameResolver) {
            return candidates[currentIndex].getStringRepresentation()
        }
    }

    fun addCandidate(candidate: Candidate) {
        candidates.add(candidate)
    }

    fun deleteCurrentCandidate() {
        currentIndex++
    }
}

class GraphBasedNameResolver : NameResolver {
    private val builderMap: MutableMap<SymbolicName, NameBuilder> = mutableMapOf()
    override fun resolve(name: SymbolicName): String {
        if (name !in builderMap) register(name)
        return builderMap[name]?.currentCandidate() ?: error("Incorrect name: $name.")
    }

    override fun register(name: SymbolicName) {
        if (name in builderMap) return
        builderMap[name] = NameBuilder(name)
        builderMap[name]!!.buildCandidates()
    }
    fun chooseNamesForFix(): Set<SymbolicName> {
        val stringRepresentations = mutableMapOf<String, MutableList<SymbolicName>>()
        for ((name, builder) in builderMap) {
            val s = builder.currentCandidate()
            stringRepresentations.getOrPut(s) { mutableListOf() }.add(name)
        }

        val collisions: List<SymbolicName> =
            stringRepresentations.values.filter { it.size > 1 }.flatten()

        val collisionsWithReservedNames: List<SymbolicName> =
            stringRepresentations.filterKeys { it in reservedViperNames }
                .values.flatten()

        return (collisions + collisionsWithReservedNames).toSet()
    }

    fun assignViperNames() {
        while (true) {
            val assignedNames = builderMap.values.toList().map { it.currentCandidate() }.toMutableSet()
            reservedViperNames.forEach { assignedNames.add(it) }
            val fixSet = chooseNamesForFix()
            if (fixSet.isEmpty()) break
            fixSet.forEach { name ->
                val builder = builderMap[name]!!
                builder.deleteCurrentCandidate()
            }
        }
    }
}