import org.jetbrains.kotlin.formver.viper.NameExpr

interface NameScope {
    val parent: NameScope?

    val mangledScopeName: NameExpr?
}

data class SimpleScope(override val mangledScopeName: NameExpr?, override val parent: NameScope? = null) : NameScope