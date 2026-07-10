// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `return shared value from expression body`(x: Any): Any = x

fun `return local value explicitly`(x: @Borrowed Any): Any {
    return <!LOCALITY_MISMATCH!>x<!>
}

fun `return local value from expression body`(x: @Borrowed Any): Any =
    <!LOCALITY_MISMATCH!>x<!>

class A(
    val x: Any
)

fun @Borrowed A.`return local receiver`(): A {
    return <!LOCALITY_MISMATCH!>this<!>
}

fun @Borrowed A.`return explicit local receiver property`(): Any {
    return this.x
}

fun @Borrowed A.`return implicit local receiver property`(): Any {
    return x
}
