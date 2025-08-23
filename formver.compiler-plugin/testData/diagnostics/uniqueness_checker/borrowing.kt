// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

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

fun sharedBorrowingA(@Borrowed y: A) {}

fun borrowingB(@Borrowed @Unique z: B) {
    sharedBorrowingA(z.y)
}

fun valid_borrow(@Unique z: B) {
    borrowingB(z)
    consumeB(z)
}

fun borrowedToNonBorrowed(@Borrowed @Unique z: B) {
    consumeB(<!UNIQUENESS_VIOLATION!>z<!>)
}

fun borrowedToNonBorrowedShared(@Borrowed @Unique y: A) {
    makeIntoShared(<!UNIQUENESS_VIOLATION!>y<!>)
}
