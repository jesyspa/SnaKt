import org.jetbrains.kotlin.formver.core.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.core.names.TypedKotlinNameWithType
import org.jetbrains.kotlin.formver.viper.NameExpr
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SEPARATOR
import org.jetbrains.kotlin.formver.viper.MangledName

class DebugNameResolver : NameResolver {
    override fun resolve(name: MangledName): String {
        val components = mutableListOf<String>()

        name.mangledType?.let { components.add(it) }

        name.fullScope?.toParts()?.forEach { part ->
            when (part) {
                is NameExpr.Part.Lit -> part.value?.let { components.add(it) }
                is NameExpr.Part.SymbolVal -> components.add(resolve(part.MangledName))
            }
        }

        name.mangledBaseName.toParts().forEach { part ->
            when (part) {
                is NameExpr.Part.Lit -> part.value?.let { components.add(it) }
                is NameExpr.Part.SymbolVal -> components.add(resolve(part.MangledName))
            }
        }
        if (name is ScopedKotlinName && name.name is TypedKotlinNameWithType) {
            name.name.additionalType.toParts().forEach { part ->
                when(part) {
                    is NameExpr.Part.Lit -> part.value?.let { components.add(it) }
                    is NameExpr.Part.SymbolVal -> components.add(resolve(part.MangledName))
                }
            }
        }
        return components.filter { it.isNotEmpty() }.joinToString(SEPARATOR)
    }

    override fun register(name: MangledName) {}
}

val MangledName.debugMangled: String
    get() {
        val debugResolver = DebugNameResolver()
        return debugResolver.resolve(this)
    }