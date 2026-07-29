// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique

val sharedValue: Any = Any()

fun `assign unique default argument from constructor call`(
    x: @Unique Any = Any()
) {}

fun `assign shared default argument for unique parameter`(
    x: @Unique Any = <!UNIQUENESS_MISMATCH!>sharedValue<!>
) {}

class `assign unique default argument in constructor`(
    val x: @Unique Any = Any(),
)

class `assign shared default argument in constructor`(
    val x: @Unique Any = <!UNIQUENESS_MISMATCH!>sharedValue<!>,
)

fun `assign unique argument as unique default argument`(
    x: @Unique Any,
    y: @Unique Any = x
) {
    val z = <!INVALID_MOVED_ACCESS!>x<!>
}

fun `chain assign unique argument as unique default argument`(
    x: @Unique Any,
    y: @Unique Any = x,
    z: @Unique Any = <!INVALID_MOVED_ACCESS!>x<!>
) {
    val z = <!INVALID_MOVED_ACCESS!>x<!>
}
