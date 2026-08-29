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

// Returning locals

fun `return shared`(a: Any): Any {
    return a
}

fun `return borrowed`(a: @Borrowed Any): Any {
    return <!LOCALITY_MISMATCH!>a<!>
}

fun `return unique`(a: @Unique Any): Any {
    return a
}

fun `return unique-borrowed`(a: @Unique @Borrowed Any): Any {
    return <!LOCALITY_MISMATCH!>a<!>
}

// Returning subproperties

fun `return shared subproperty`(a: B): Any {
    return a.y
}

fun `return borrowed subproperty`(a: @Borrowed B): Any {
    <!EXIT_UNIQUENESS_INCONSISTENCY!>return a.y<!>
}

fun `return unique subproperty`(a: @Unique B): Any {
    return a.y
}

fun `return unique-borrowed subproperty`(a: @Unique @Borrowed B): Any {
    <!EXIT_UNIQUENESS_INCONSISTENCY!>return a.y<!>
}

fun `return shared from unique function`(a: Any) : @Unique Any {
    return <!UNIQUENESS_MISMATCH!>a<!>
}

fun `return conforms to unique`() {
    val x: @Unique Nothing = return
}

fun makeUnique(): @Unique Any? = null

fun `return in elvis conforms to unique`() {
    val x: @Unique Any = makeUnique() ?: return
}

fun `return in elvis conforms to shared`() {
    val x: Any = makeUnique() ?: return
}
