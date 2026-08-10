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

fun <!VIPER_TEXT!>refDefault<!>(a: Int, r: Boxed = Boxed(0)): Int = a

@Suppress("NOTHING_TO_INLINE")
inline fun <!VIPER_TEXT!>inlineRefDefault<!>(a: Int, r: Boxed = Boxed(0)): Int = a

fun <!VIPER_TEXT!>nullableDefault<!>(a: Int, r: Boxed? = null): Int = a

fun <!VIPER_TEXT!>functionDefault<!>(a: Int, f: (Int) -> Int = { it }): Int = a

fun <!VIPER_TEXT!>varargAndDefault<!>(vararg xs: Int, b: Int = 0): Int = b

// The filled-in value carries no access permissions, so nothing can be read off it, but the
// call still may not make the verification state inconsistent.
@AlwaysVerify
fun <!VIPER_TEXT!>omittedReferenceDefault<!>() {
    refDefault(1)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedInlineReferenceDefault<!>() {
    inlineRefDefault(1)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedNullableDefault<!>() {
    nullableDefault(1)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedFunctionDefault<!>() {
    functionDefault(1)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

// A vararg argument of a user function is not supported; filling in the omitted default must
// keep reporting that, rather than a different failure.
@AlwaysVerify
fun varargWithOmittedDefault() {
    varargAndDefault(<!INTERNAL_ERROR!>1, 2<!>)
    verify(false)
}

// A vararg parameter given no arguments has no entry in the argument mapping, so it is filled
// in like any omitted parameter: a value of the array type, not known to be empty.
@AlwaysVerify
fun <!VIPER_TEXT!>emptyVarargWithOmittedDefault<!>() {
    varargAndDefault()
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@Pure
fun <!VIPER_TEXT!>pureDifference<!>(a: Int, b: Int): Int = a - b

// The arguments reach the parameters they name, not the ones in their own order.
@AlwaysVerify
fun <!VIPER_TEXT!>namedArgumentsOutOfOrder<!>() {
    verify(pureDifference(b = 1, a = 3) == 2)
}
