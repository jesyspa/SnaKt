// FULL_JDK


import org.jetbrains.kotlin.formver.plugin.*

// Indexing the same String at two distinct variables under one `exists` reaches
// a Silver info kind the error-reporting path did not handle, which used to crash
// the compiler with `NotImplementedError: Unreachable`. The unprovable
// postcondition must now degrade to a normal Viper verification error instead.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>duplicateIndexExists<!>(s: String, res: Int): Int {
    postconditions<Int> {
        exists<Int> { i -> 0 <= i && i < s.length && s[i] == s[res] }
    }
    return 0
}<!>
