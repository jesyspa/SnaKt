import org.jetbrains.kotlin.formver.plugin.Pure

fun <!VIPER_TEXT!>emptyFunction<!>() { val x = emptyAnnotatedFunction() }

@Pure
fun <!VIPER_TEXT!>emptyAnnotatedFunction<!>(): Int? { return null }

@Pure
fun <!VIPER_TEXT!>annotatedIntLitReturn<!>(): Int { return 42 }

@Pure
fun <!VIPER_TEXT!>annotatedBoolLitReturn<!>(): Boolean { return true }

@Pure
fun <!VIPER_TEXT!>annotatedCharLitReturn<!>(): Char { return 'A' }

@Pure
fun <!VIPER_TEXT!>annotatedStringLitReturn<!>(): String { return "Hello SnaKt" }