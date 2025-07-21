// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    @Unique val x = 1
}

class B {
    val y = A()
}

@Unique val Int.f get() = 3

fun f(@Unique z: B) {
    z.y.x
}

fun g() {
    3.f
}

fun g(@Unique x: Int) {

}

<!UNIQUENESS_VIOLATION!>fun use_g(@Unique z: B) {
    g(z.y.x)
}<!>