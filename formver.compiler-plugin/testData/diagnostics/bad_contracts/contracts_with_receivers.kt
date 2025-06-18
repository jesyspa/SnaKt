import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class Class {
    @OptIn(ExperimentalContracts::class)
    fun <!VIPER_TEXT!>is2<!>(): Boolean {
        contract {
            <!CONDITIONAL_EFFECT_ERROR!>returns(true) implies (this@Class is Impl1)<!>
        }
        return this is Impl2
    }

    @OptIn(ExperimentalContracts::class)
    fun Class.<!VIPER_TEXT!>is1butWithDispatch<!>(): Boolean {
        contract {
            <!CONDITIONAL_EFFECT_ERROR!>returns(true) implies (this@is1butWithDispatch is Impl2)<!>
        }
        return this@is1butWithDispatch is Impl1
    }
}

class Impl1: Class()
class Impl2: Class()

@OptIn(ExperimentalContracts::class)
fun Class.<!VIPER_TEXT!>is1<!>(): Boolean {
    contract {
        <!CONDITIONAL_EFFECT_ERROR!>returns(true) implies (this@is1 is Impl2)<!>
    }
    return this is Impl1
}
