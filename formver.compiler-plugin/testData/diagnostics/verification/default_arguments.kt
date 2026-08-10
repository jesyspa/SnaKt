// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.verify

fun <!VIPER_TEXT!>trailingDefault<!>(a: Int, b: Int = 0): Int = a + b

@Pure
fun <!VIPER_TEXT!>pureTrailingDefault<!>(a: Int, b: Int = 0): Int = a + b

@Suppress("NOTHING_TO_INLINE")
inline fun <!VIPER_TEXT!>inlineTrailingDefault<!>(a: Int, b: Int = 0): Int = a + b

fun <!VIPER_TEXT!>defaultInTheMiddle<!>(a: Int, skipped: Int = 0, c: Int): Int = a + skipped + c

class Boxed(val a: Int, val b: Int = 0)

// A parameter left at its default tells the caller nothing, but the call still may not
// make the verification state inconsistent.
@AlwaysVerify
fun <!VIPER_TEXT!>omittedTrailingDefault<!>() {
    trailingDefault(1)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedDefaultInTheMiddle<!>() {
    defaultInTheMiddle(1, c = 2)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedInlineDefault<!>() {
    inlineTrailingDefault(1)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedConstructorDefault<!>() {
    Boxed(1)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

// Nothing is assumed about the omitted argument, so the default value itself does not
// reach the caller.
@AlwaysVerify
fun <!VIPER_TEXT!>defaultValueIsNotAssumed<!>() {
    val defaulted = pureTrailingDefault(1)
    val given = pureTrailingDefault(1, 0)
    verify(given == 1)
    verify(<!VIPER_VERIFICATION_ERROR!>defaulted == 1<!>)
}
