// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

fun borrow(x: @Borrowed A) {}

fun share(x: A) {}

fun borrowBoth(x: @Borrowed A, y: @Borrowed A) {}

fun shareAndBorrow(x: A, y: @Borrowed A) {}

fun borrowWithDefault(x: @Borrowed A = A()) {}

fun borrowDefaultAndShare(x: @Borrowed A = A(), y: A) {}

fun @Borrowed A.borrowTarget() {}

fun A.shareTarget() {}

fun @Borrowed A.borrowTargetAndArg(y: @Borrowed A) {}

fun `pass local as shared argument`(x: @Borrowed A) {
    share(<!LOCALITY_MISMATCH!>x<!>)
}

fun `pass global as borrowed argument`(x: A) {
    borrow(x)
}

fun `pass global as named borrowed argument`(x: A) {
    borrow(x=x)
}

fun `pass local as borrowed argument`(x: @Borrowed A) {
    borrow(x)
}

fun `pass local as named borrowed argument`(x: @Borrowed A) {
    borrow(x=x)
}

fun `assign local to global after passing it as borrowed argument`(x: @Borrowed A) {
    borrow(x)
    var y: A = <!LOCALITY_MISMATCH!>x<!>
}

fun `share local after initializing it with a global value`(x: A) {
    var y: @Borrowed A = x
    share(<!LOCALITY_MISMATCH!>y<!>)
}

fun `pass local twice as borrowed arguments`(x: @Borrowed A) {
    borrowBoth(x, x)
}

fun `pass local twice as named borrowed arguments`(x: @Borrowed A) {
    borrowBoth(y=x, x=x)
}

fun `pass local as explicit shared target`(x: @Borrowed A) {
    <!LOCALITY_MISMATCH!>x<!>.shareTarget()
}

fun `pass global as explicit borrowed target`(x: A) {
    x.borrowTarget()
}

fun `pass local as explicit borrowed target`(x: @Borrowed A) {
    x.borrowTarget()
}

fun `pass local as explicit borrowed target and argument`(x: @Borrowed A) {
    x.borrowTargetAndArg(x)
}

fun `pass local as borrowed target`(x: @Borrowed A) {
    x.borrowTarget()
}

fun `pass global as borrowed target`(x: A) {
    x.borrowTarget()
}

fun `pass local as borrowed target and argument`(x: @Borrowed A) {
    x.borrowTargetAndArg(x)
}

fun `pass global as borrowed target and argument`(x: A) {
    x.borrowTargetAndArg(x)
}

fun `pass local as named local and global arguments`(x: @Borrowed A) {
    shareAndBorrow(y = x, x = <!LOCALITY_MISMATCH!>x<!>)
}

fun `pass local as named local along local default`(x: @Borrowed A) {
    borrowDefaultAndShare(y = <!LOCALITY_MISMATCH!>x<!>)
}

fun @Borrowed A.`pass local as implicit shared target`() {
    <!LOCALITY_MISMATCH!>shareTarget()<!>
}

fun @Borrowed A.`pass local as implicit local target`() {
    borrowTarget()
}

fun @Borrowed A.`pass local this as explicit shared target`() {
    <!LOCALITY_MISMATCH!>this<!>.shareTarget()
}

fun @Borrowed A.`pass local this as explicit local target`() {
    this.borrowTarget()
}

fun `pass local as local argument in local property initializer`(x: @Borrowed A) {
    val y = borrow(x)
}
