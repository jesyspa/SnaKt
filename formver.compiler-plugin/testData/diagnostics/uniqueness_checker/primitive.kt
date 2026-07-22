// FULL_JDK
// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun `assign primitive int literal to unique local`() {
    val i: @Unique Int = <!UNIQUENESS_MISMATCH!>10<!>
}

fun `assign primitive int literal to shared local`() {
    val i: Int = 10
}

fun `assign primitive operation on literals to unique local`() {
    val i: @Unique Int = <!UNIQUENESS_MISMATCH!>10 + 10<!>
}

fun `assign primitive variable to unique local`() {
    val x: Int = 0
    val i: @Unique Int = <!UNIQUENESS_MISMATCH!>0<!>
}
