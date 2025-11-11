import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.DumpExpEmbeddings

@DumpExpEmbeddings
fun <!VIPER_TEXT!>emptyFunction<!>() { emptyFunctionAsWell() }

@Pure
fun <!VIPER_TEXT!>emptyFunctionAsWell<!>(): Int { return 10 }

@Pure
fun <!VIPER_TEXT!>emptyAnnotatedFunction<!>(): Int { return 42 }
