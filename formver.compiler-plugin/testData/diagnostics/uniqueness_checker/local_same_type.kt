// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    @Unique val x = 1
}

fun consumeInt(@Unique x: Int) {}

fun moveSameTypedLocals(@Unique x: Int, @Unique y: Int) {
    consumeInt(x)
    consumeInt(y)
}

fun moveSameTypedLocalMembers(@Unique x: A, @Unique y: A) {
    consumeInt(x.x)
    consumeInt(y.x)
}