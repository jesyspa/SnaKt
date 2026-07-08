// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class Box {
    var item: @Unique Any = Any()
    var other: @Unique Any = Any()
}

class Nested {
    var box: @Unique Box = Box()
}

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

fun share(a: Any) {}

// Local aliases

fun `consume through unique alias moves source`(x: @Unique Any) {
    val alias: @Unique Any = x

    consume(alias)
    consume(<!INVALID_MOVED_ACCESS!>x<!>)
}

fun `borrow through unique alias does not restore source`(x: @Unique Any) {
    val alias: @Unique Any = x

    borrow(alias)
    consume(<!INVALID_MOVED_ACCESS!>x<!>)
}

fun `share through unique alias moves source`(x: @Unique Any) {
    val alias = x

    share(alias)
    consume(<!INVALID_MOVED_ACCESS!>x<!>)
}

fun `reassign alias after consuming original`(x: @Unique Any, y: @Unique Any) {
    var alias = x
    consume(alias)

    alias = y
    consume(alias)
}

fun `consume original after alias reassigned`(x: @Unique Any, y: @Unique Any) {
    var alias = x
    alias = y

    consume(<!INVALID_MOVED_ACCESS!>x<!>)
    consume(alias)
}

// Property aliases

fun `consume property alias leaks parent`(nested: @Unique Nested) {
    val box: @Unique Box = nested.box

    consume(box)
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>nested<!>)
}

fun `borrow property alias does not restore parent`(nested: @Unique Nested) {
    val box: @Unique Box = nested.box

    borrow(box)
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>nested<!>)
}

fun `consume nested property alias leaks grandparent`(nested: @Unique Nested) {
    val item: @Unique Any = nested.box.item

    consume(item)
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>nested<!>)
}

fun `restore property after alias move clears leak`(nested: @Unique Nested, fresh: @Unique Box) {
    val box: @Unique Box = nested.box
    consume(box)

    nested.box = fresh
    consume(nested)
}

fun `move one property alias and consume sibling`(box: @Unique Box) {
    val item: @Unique Any = box.item

    consume(item)
    consume(box.other)
}

fun `move one property alias then consume parent leaks`(box: @Unique Box) {
    val item: @Unique Any = box.item

    consume(item)
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>box<!>)
}
