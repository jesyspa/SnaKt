// FULL_JDK
// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class B {
    var y: @Unique Exception = Exception()
}

// Throwing locals

fun `throw shared`(a: Throwable) {
    throw a
}

fun `throw borrowed`(a: @Borrowed Throwable) {
    throw <!LOCALITY_MISMATCH!>a<!>
}

fun `throw unique`(a: @Unique Throwable) {
    throw a
}

fun `throw unique-borrowed`(a: @Unique @Borrowed Throwable) {
    throw <!LOCALITY_MISMATCH!>a<!>
}

// Throwing subproperties

fun `throw shared subproperty`(a: B) {
    throw a.y
}

fun `throw borrowed subproperty`(a: @Borrowed B) {
    <!EXIT_UNIQUENESS_INCONSISTENCY!>throw a.y<!>
}

fun `throw unique subproperty`(a: @Unique B) {
    throw a.y
}

fun `throw unique-borrowed subproperty`(a: @Unique @Borrowed B) {
    <!EXIT_UNIQUENESS_INCONSISTENCY!>throw a.y<!>
}
