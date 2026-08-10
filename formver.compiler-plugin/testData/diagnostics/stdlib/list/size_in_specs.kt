// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

@AlwaysVerify
fun <!VIPER_TEXT!>size_in_postcondition<!>(l: List<Int>): Int {
    postconditions<Int> { res ->
        res == l.size
    }
    return l.size
}

@AlwaysVerify
fun <!VIPER_TEXT!>size_in_precondition<!>(l: List<Int>): Int {
    preconditions {
        l.size > 0
    }
    return l[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>count_up_over_list<!>(l: List<Int>): Int {
    postconditions<Int> { res ->
        res == l.size
    }

    var i = 0
    var acc = 0
    while (i < l.size) {
        loopInvariants {
            0 <= i && i <= l.size
            acc == i
        }
        val element = l[i]
        acc = acc + 1
        i = i + 1
    }
    return acc
}
