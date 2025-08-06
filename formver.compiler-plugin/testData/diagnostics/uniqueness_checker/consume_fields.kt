// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    @Unique val x = 1
}

class B {
    @Unique val y = A()
}

fun consumeInt(@Unique a: Int) {}
fun consumeA(@Unique a: A) {}
fun consumeB(@Unique a: B) {}

fun makeIntoShared(a: A) {}

fun valid_consume_all(@Unique z: B) {
    consumeInt(z.y.x)
    consumeA(z.y)
    consumeB(z)
}

<!UNIQUENESS_VIOLATION!>fun consumeParent(@Unique z: B) {
    consumeA(z.y)
    consumeInt(z.y.x)
}<!>

<!UNIQUENESS_VIOLATION!>fun uniqueBecomeShared(@Unique z: B) {
    makeIntoShared(z.y)
    consumeA(z.y)
}<!>