// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

// A prefix/suffix query is a pure observation: it may not make the verification
// state inconsistent, so `false` has to stay unprovable after one.
@AlwaysVerify
fun <!VIPER_TEXT!>startsWithKeepsStateConsistent<!>(a: String, b: String) {
    a.startsWith(b)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>endsWithKeepsStateConsistent<!>(a: String, b: String) {
    a.endsWith(b)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}
