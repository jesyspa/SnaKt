// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A(
    var x: Any
)

fun `assign local to global property`(x: A, y: @Borrowed Any) {
    x.x = <!LOCALITY_MISMATCH!>y<!>
}

fun `assign local to local property`(x: @Borrowed A, y: @Borrowed Any) {
    x.x = <!LOCALITY_MISMATCH!>y<!>
}

fun `assign global property access to local`(x: A) {
    var z: @Borrowed Any = x.x
}

fun `assign local property access to local`(x: @Borrowed A) {
    var z: @Borrowed Any = x.x
}

fun `assign global safe-call expression to local`(x: A?) {
    var z: @Borrowed Any? = x?.x
}

fun `assign local safe-call expression to local`(x: @Borrowed A?) {
    var z: @Borrowed Any? = x?.x
}

fun `assign local safe-call expression to global`(x: @Borrowed A?) {
    var z: Any? = x?.x
}

fun `safe-call local receiver property stays global`(a: @Borrowed A?) {
    val y: Any? = a?.x
}

class B(
    var x: Any
)

fun `assign global property to local property`(x: @Borrowed B, y: B) {
    x.x = y.x
}

fun `assign local property to global property`(x: B, y: @Borrowed B) {
    x.x = y.x
}

fun `assign local property to local property`(x: @Borrowed B, y: @Borrowed B) {
    x.x = y.x
}
