// CHECKERS_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
    var w: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

class Node(val next: @Unique Node?)

fun borrow(a: @Borrowed Any) {}

fun borrowUnique(a: @Borrowed @Unique Any) {}

fun consume(a: @Unique Any?) {}

fun share(a: Any) {}

fun borrowBoth(a: @Borrowed Any, b: @Borrowed Any) {}

fun consumeBoth(a: @Unique Any, b: @Unique Any) {}

fun shareBoth(a: Any, b: Any) {}

fun `pass unique subproperty and parent to consumeBoth`(a: @Unique B) {
    consumeBoth(<!INVALID_OVERLAPPING_UNIQUE_ARGUMENTS!>a.y<!>, <!INVALID_OVERLAPPING_UNIQUE_ARGUMENTS!>a<!>)
}

fun `pass unique-borrowed subproperty and parent to borrowBoth`(a: @Unique @Borrowed B) {
    borrowBoth(a.y, a)
}

fun `consume unique after safe cast`(a: @Unique Any) {
    val cast = a as? A ?: return
    consume(cast)
    consume(<!INVALID_MOVED_ACCESS!>a<!>)
    consume(<!INVALID_MOVED_ACCESS!>cast<!>)
}

fun `borrow after sharing shared`(a: A) {
    share(a)
    borrow(a)
}

// Sharing locals

fun `share shared`(y: A) {
    share(y)
}

fun `share borrowed`(y: @Borrowed A) {
    share(<!LOCALITY_MISMATCH!>y<!>)
}

fun `share unique`(y: @Unique A) {
    share(y)
}

fun `share unique-borrowed`(y: @Borrowed @Unique A) {
    share(<!LOCALITY_MISMATCH!>y<!>)
}

// Borrowing locals

fun `borrow shared`(z: A) {
    borrow(z)
}

fun `borrow borrowed`(z: @Borrowed A) {
    borrow(z)
}

fun `borrow unique`(z: @Unique A) {
    borrow(z)
}

fun `borrow unique-borrowed`(z: @Borrowed @Unique A) {
    borrow(z)
}

// Borrowing locals after consuming

fun `borrow after consuming unique`(a: @Unique A) {
    consume(a)
    borrow(<!INVALID_MOVED_ACCESS!>a<!>)
}

// Borrowing locals after sharing

fun `borrow after sharing unique`(a: @Unique A) {
    share(a)
    borrow(<!INVALID_MOVED_ACCESS!>a<!>)
}

// Consuming locals

fun `consume shared`(a: A) {
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
}

fun `consume borrowed`(a: @Borrowed A) {
    consume(<!LOCALITY_MISMATCH, UNIQUENESS_MISMATCH!>a<!>)
}

fun `consume unique`(a: @Unique A) {
    consume(a)
}

fun `consume unique-borrowed`(a: @Unique @Borrowed A) {
    consume(<!LOCALITY_MISMATCH!>a<!>)
}

fun `consume unique null`() {
    consume(null)
}

// Consuming locals after borrowing

fun `consume shared after borrowing it`(a: A) {
    borrow(a)
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
}

fun `consume borrowed after borrowing it`(a: @Borrowed A) {
    borrow(a)
    consume(<!LOCALITY_MISMATCH, UNIQUENESS_MISMATCH!>a<!>)
}

fun `consume after borrowing unique`(a: @Unique A) {
    borrow(a)
    consume(a)
}

fun `consume after borrowing unique as unique`(a: @Unique A) {
    borrowUnique(a)
    consume(a)
}

fun `consume after borrowing unique-borrowed`(a: @Unique @Borrowed A) {
    borrow(a)
    consume(<!LOCALITY_MISMATCH!>a<!>)
}

fun `consume after after borrowing unique-borrowed as unique`(a: @Unique @Borrowed A) {
    borrowUnique(a)
    consume(<!LOCALITY_MISMATCH!>a<!>)
}

// Consuming locals after type checks and casts

fun `consume unique after storing type check`(a: @Unique Any) {
    val ok = a is A
    consume(a)
}

// Passing the same local as multiple arguments

fun `pass shared twice to shareBoth`(a: Any) {
    shareBoth(a, a)
}

fun `pass borrowed twice to borrowBoth`(a: @Borrowed Any) {
    borrowBoth(a, a)
}

fun `pass unique twice to consumeBoth`(a: @Unique Any) {
    consumeBoth(<!INVALID_DUPLICATE_UNIQUE_ARGUMENT!>a<!>, <!INVALID_DUPLICATE_UNIQUE_ARGUMENT!>a<!>)
}

// Sharing subproperties

fun `share shared subproperty`(z: B) {
    share(z.y.x)
}

fun `share borrowed subproperty`(z: @Borrowed B) <!EXIT_UNIQUENESS_INCONSISTENCY!>{
    share(z.y.x)
}<!>

fun `share unique subproperty`(z: @Unique B) {
    share(z.y.x)
}

fun `share unique-borrowed subproperty`(z: @Unique @Borrowed B) <!EXIT_UNIQUENESS_INCONSISTENCY!>{
    share(z.y.x)
}<!>

fun `share multiple unique subproperties`(z: @Unique B) {
    share(z.y.x)
    share(z.y.w)
}

// Sharing partially-inconsistent subproperties

fun `share partially moved`(z: @Unique B) {
    consume(z.y)
    share(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>z<!>)
}

fun `share partially shared`(z: @Unique B) {
    share(z.y)
    share(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>z<!>)
}

// Sharing subproperties after assignment

fun `share subproperty after assigning it to shared`(x: @Unique B, v: A) {
    x.y = <!UNIQUENESS_MISMATCH!>v<!>
    share(x.y)
}

fun `share subproperty after assigning it to unique`(x: @Unique B, v: @Unique A) {
    x.y = v
    share(x.y)
}

// Borrowing subproperties

fun `share shared subproperty twice`(z: B) {
    share(z.y)
    share(z.y)
}

fun `borrow shared subproperty`(z: B) {
    borrow(z.y)
}

fun `borrow shared subproperty twice`(z: B) {
    borrow(z.y)
    borrow(z.y)
}

fun `borrow borrowed subproperty`(z: @Borrowed B) {
    borrow(z.y)
}

fun `borrow unique subproperty`(z: @Unique B) {
    borrow(z.y)
}

fun `borrow unique-borrowed subproperty`(z: @Borrowed @Unique B) {
    borrow(z.y)
}

fun `borrow multiple unique subproperties`(z: @Unique B) {
    borrow(z.y.x)
    borrow(z.y.w)
}

// Borrowing partially-inconsistent subproperties

fun `borrow partially moved`(z: @Unique B) {
    consume(z.y)
    borrow(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>z<!>)
}

fun `borrow partially shared`(z: @Unique B) {
    share(z.y)
    borrow(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>z<!>)
}

// Borrowing subproperties after assignment

fun `borrow unique parent after assigning subproperty to unique`(x: @Unique B, v: @Unique A) {
    x.y = v
    borrow(x)
}

fun `borrow unique parent twice after assigning subproperty to unique`(x: @Unique @Borrowed B, v: @Unique A) {
    x.y = v
    borrow(x)
    borrow(x)
}

// Consuming subproperties

fun `consume shared subproperty`(z: B) {
    consume(<!UNIQUENESS_MISMATCH!>z.y.x<!>)
}

fun `consume borrowed subproperty`(z: @Borrowed B) <!EXIT_UNIQUENESS_INCONSISTENCY!>{
    consume(<!UNIQUENESS_MISMATCH!>z.y.x<!>)
}<!>

fun `consume unique subproperty`(z: @Unique B) {
    consume(z.y.x)
}

fun `consume unique-borrowed subproperty`(z: @Borrowed @Unique B) <!EXIT_UNIQUENESS_INCONSISTENCY!>{
    consume(z.y.x)
}<!>

fun `consume multiple unique subproperties`(z: @Unique B) {
    consume(z.y.x)
    consume(z.y.w)
}

// Consuming partially-inconsistent subproperties

fun `consume partially moved`(z: @Unique B) {
    consume(z.y)
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>z<!>)
}

fun `consume partially shared`(z: @Unique B) {
    share(z.y)
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>z<!>)
}

// Consuming subproperty after assignment

fun `consume unique parent after assigning subproperty to unique`(x: @Unique B, y: @Unique A) {
    x.y = y
    consume(x.y)
}

// Consuming subproperty after smart-cast

fun `consume unique parent after cast`(node: @Unique Any) {
    val local: @Unique Node? = (node as Node).next
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>node<!>)
}

fun `consume unique parent after cast to not-null`(node: @Unique Node) {
    val local: @Unique Node = node.next as Node
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>node<!>)
}

fun `consume unique parent after smart-cast`(node: @Unique Node?) {
    if (node != null) {
        val local: @Unique Node? = node.next
        consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>node<!>)
    }
}

// Passing the same subproperty as multiple arguments

fun `pass shared subproperty and parent to shareBoth`(a: B) {
    shareBoth(a.y, a)
}

fun `pass borrowed subproperty and parent to borrowBoth`(a: @Borrowed B) {
    borrowBoth(a.y, a)
}
