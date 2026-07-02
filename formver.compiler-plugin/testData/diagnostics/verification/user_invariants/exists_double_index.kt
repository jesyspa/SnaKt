// FULL_JDK


import org.jetbrains.kotlin.formver.plugin.*

// Indexing the same String at two distinct variables under one `exists` reaches
// a Silver info kind the error-reporting path does not handle. The unprovable
// postcondition degrades to a normal Viper verification error.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>duplicateIndexExists<!>(s: String, res: Int): Int {
    postconditions<Int> {
        exists<Int> { i -> 0 <= i && i < s.length && s[i] == s[res] }
    }
    return 0
}<!>
