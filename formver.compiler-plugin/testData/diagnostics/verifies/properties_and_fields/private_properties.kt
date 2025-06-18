import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

open class A {
    private var field: Boolean = false
        get() {
            return !field
        }

    fun <!VIPER_TEXT!>getBooleanField<!>() = field
}

open class B : A() {
    private val field: String = ""

    fun <!VIPER_TEXT!>getStringField<!>() = field
}

class C : B() {
    var field: Int = 0
}

class D: B() {
    val field: Int = 0
}

@AlwaysVerify
@Suppress("USELESS_IS_CHECK")
fun <!VIPER_TEXT!>extractPublic<!>() {
    val cond1 = C().field is Int
    val cond2 = D().field is Int
    verify(cond1, cond2)
}