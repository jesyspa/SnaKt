// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A(
    var x: @Unique Any,
    var w: @Unique Throwable
)

fun `return unique field of a local shared`(x: @Borrowed A): Any {
    <!EXIT_UNIQUENESS_INCONSISTENCY!>return x.x<!>
}

fun `return unique field of a local unique`(x: @Borrowed @Unique A): @Unique Any {
    <!EXIT_UNIQUENESS_INCONSISTENCY!>return x.x<!>
}

fun `throw unique field of a local shared`(x: @Borrowed A): Any {
    <!EXIT_UNIQUENESS_INCONSISTENCY!>throw x.w<!>
}

fun `throw unique field of a local unique`(x: @Borrowed @Unique A): @Unique Any {
    <!EXIT_UNIQUENESS_INCONSISTENCY!>throw x.w<!>
}
