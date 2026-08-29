// CHECKERS_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

val sharedValue: Any = Any()

fun share(x: Any) {}
fun consume(x: @Unique Any) {}

class Node {
    var child: @Unique Any = Any()
}

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

fun `share constructor default for shared parameter twice`(
    y: Any = Any(),
) {
    share(y)
    share(y)
}

fun `share shared parameter with constructor default after assignment`(
    y: Any = Any(),
) {
    val z = y
    share(y)
}

fun `shared parameter with constructor default cannot initialize unique local`(
    y: Any = Any(),
) {
    val z: @Unique Any = <!UNIQUENESS_MISMATCH!>y<!>
}

fun `reuse shared constructor default across multiple defaults`(
    y: Any = Any(),
    z: Any = y,
    w: Any = y,
) {}

fun `escape inconsistent parameter in function default argument`(
    b: @Unique Node,
    moved: @Unique Any = b.child,
    escaped: Unit = consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>),
) {}

class EscapeFromConstructorDefaultArgument(
    b: @Unique Node,
    moved: @Unique Any = b.child,
    escaped: Unit = consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>),
)

fun `assign shared default argument for borrowed parameter`(
    x: @Borrowed Any = sharedValue,
) {}
