import org.jetbrains.kotlin.formver.plugin.Pure

fun <!VIPER_TEXT!>emptyFunction<!>() { val x = emptyAnnotatedFunction() }

@Pure
fun <!VIPER_TEXT!>emptyAnnotatedFunction<!>(): Int? { return null }

@Pure
fun <!VIPER_TEXT!>annotatedLiteralReturn<!>(): Boolean {
    return true
}

@Pure
fun <!VIPER_TEXT!>annotatedVariableReturn<!>(): Int {
    val x = 5
    return x
}