// CHECKERS_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class Box(
    val a: @Unique Any = Any()
)

val `unique top-level property`: @Unique Any = Any()

fun `unique parameter`(box: @Unique Box) {}

fun `borrowed parameter`(box: @Borrowed Box) {}

fun `unique borrowed parameter`(box: @Unique @Borrowed Box) {}

fun @Unique Box.`unique extension receiver`() {}

fun `unique local variable`() {
    val x: @Unique Any = Any()
}

fun `unique return type`(): @Unique Box = Box()

val `reject unique attribute on generic type parameter`: List<<!INVALID_UNIQUENESS_TYPE_TARGET!>@Unique Any<!>>
    = listOf()

typealias `Invalid Unique Type` = <!INVALID_UNIQUENESS_TYPE_TARGET!>@Unique Any<!>
