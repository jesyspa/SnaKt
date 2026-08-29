// CHECKERS_ONLY
// DUMP_UNIQUENESS_CFG

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A(
    var first: @Unique B,
    var second: @Unique B,
)


class B()

fun <!UNIQUENESS_CFG!>nonDet<!>() : Boolean {
    return true
}

fun <!UNIQUENESS_CFG!>test<!>(a: @Unique A) : A{

    if (nonDet()) {
        var x = a.first
    } else {
        var y: @Unique B = a.second
    }

    return <!ESCAPE_UNIQUENESS_INCONSISTENCY, ESCAPE_UNIQUENESS_INCONSISTENCY!>a<!>

}
