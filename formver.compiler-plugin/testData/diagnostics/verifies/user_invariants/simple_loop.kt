import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify
import org.jetbrains.kotlin.formver.plugin.loopInvariants

@AlwaysVerify
fun <!VIPER_TEXT!>test<!>(n: Int) {
    var it = 0
    var holds = true
    while (it < 10) {
        loopInvariants {
            it <= 10
            holds
        }
        it = it + 1
    }
    verify(it == 10)

    if (it <= n) {
        while (it < n) {
            loopInvariants {
                it <= n
                holds
            }
            it = it + 1
        }
        verify(it == n)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>loopInsideLoop<!>() {
    var i = 0
    while (i < 10) {
        loopInvariants {
            i <= 10
        }
        var j = i + 1
        while (j < 10) {
            loopInvariants {
                i < j
                j <= 10
            }
            j = j + 1
        }
        i = i + 1
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>withBreak<!>() {
    var i = 0
    while (true) {
        loopInvariants {
            i <= 10
        }
        if (i >= 10) break
    }
    verify(i == 10)
}

class WithVar(var e: Int) {
    @NeverConvert
    fun doSomething() = e > 10
}

@AlwaysVerify
fun <!VIPER_TEXT!>test_boolean_postcondition<!>() {
    val withVar = WithVar(42)
    var boolean = true
    while (boolean) {
        boolean = withVar.doSomething()
    }
    verify(!boolean)
}