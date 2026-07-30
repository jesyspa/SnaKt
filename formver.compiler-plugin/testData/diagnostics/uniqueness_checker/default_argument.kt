// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

val sharedValue: Any = Any()

fun share(x: Any) {}

fun `assign unique default argument from constructor call`(
    x: @Unique Any = Any()
) {}

fun `assign shared default argument for unique parameter`(
    x: @Unique Any = <!UNIQUENESS_MISMATCH!>sharedValue<!>
) {}

fun `assign shared parameter as unique default argument`(
    x: Any,
    y: @Unique Any = <!UNIQUENESS_MISMATCH!>x<!>
) {
    val z = x
}

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

fun `share default argument resolved from shared parameter twice`(
    x: Any,
    y: Any = x,
) {
    share(y)
    share(y)
}

fun `assign shared default argument for borrowed parameter`(
    x: @Borrowed Any = sharedValue,
) {}
