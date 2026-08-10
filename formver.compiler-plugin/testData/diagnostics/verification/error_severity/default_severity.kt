// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// The postcondition does not hold. By default a failed proof is a warning, so
// that turning the plugin on cannot break a build.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>unprovablePostcondition<!>(x: Int): Int {
    postconditions<Int> { result -> result > x }
    return x
}<!>
