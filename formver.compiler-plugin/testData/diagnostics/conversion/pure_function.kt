import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.DumpExpEmbeddings

@DumpExpEmbeddings
fun <!EXP_EMBEDDING, VIPER_TEXT!>emptyFunction<!>() { <!INTERNAL_ERROR!>emptyFunctionAsWell()<!> }

@Pure
fun <!VIPER_TEXT!>emptyFunctionAsWell<!>(): Int { return 10 }

@Pure
fun <!VIPER_TEXT!>emptyAnnotatedFunction<!>(): Int { return 42 }
