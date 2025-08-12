// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    @Unique val x = 1
    @Unique val w = 2
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
    consumeInt(z.y.w)
}

fun doubleConsume(@Unique a: A) {
    consumeA(a)
    consumeA(<!UNIQUENESS_VIOLATION!>a<!>)
}

fun consumeParent(@Unique z: B) {
    consumeA(z.y)
    consumeInt(<!UNIQUENESS_VIOLATION!>z.y.x<!>)
}

fun uniqueBecomeShared(@Unique z: B) {
    makeIntoShared(z.y)
    consumeA(<!UNIQUENESS_VIOLATION!>z.y<!>)
}

fun uniqueBecomeSharedValid(@Unique z: B) {
    makeIntoShared(z.y)
    makeIntoShared(z.y)
}