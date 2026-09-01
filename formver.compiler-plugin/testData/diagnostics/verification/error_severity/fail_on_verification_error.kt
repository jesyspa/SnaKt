// FULL_JDK
// FAIL_ON_VERIFICATION_ERROR

import org.jetbrains.kotlin.formver.plugin.*

// The same failed proof as in default_severity.kt, reported as an error rather
// than a warning.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>unprovablePostcondition<!>(x: Int): Int {
    postconditions<Int> { result -> result > x }
    return x
}<!>
