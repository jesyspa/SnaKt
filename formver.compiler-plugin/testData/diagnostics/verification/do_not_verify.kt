// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify
import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

@NeverVerify
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>bad_returns<!>(): Boolean {
    contract {
        returns(true)
    }
    return false
}

@NeverConvert
fun noop() {}

@NeverConvert
fun excludedOuter() {
    @AlwaysVerify
    fun <!VIPER_TEXT!>selectedInner<!>(): Unit {
        verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
    }
}
