import org.jetbrains.kotlin.formver.plugin.Pure

fun <!VIPER_TEXT!>emptyFunction<!>() { val x = emptyAnnotatedFunction() }

@Pure
fun <!VIPER_TEXT!>emptyAnnotatedFunction<!>(): Int? { return null }
