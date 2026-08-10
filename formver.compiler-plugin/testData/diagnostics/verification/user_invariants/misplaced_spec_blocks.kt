// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.loopInvariants

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>preconditionsAfterPostconditions<!>(arg: Boolean): Boolean {
    postconditions<Boolean> { ret -> ret == arg }
    <!IGNORED_SPEC_BLOCK!>preconditions {
        arg == true
    }<!>
    return arg
}

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>postconditionsNotImmediatelyAfterPreconditions<!>(arg: Boolean): Boolean {
    preconditions {
        arg == true
    }
    val unrelated = arg
    <!IGNORED_SPEC_BLOCK!>postconditions<Boolean> { ret -> ret == arg }<!>
    return unrelated
}

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>loopInvariantsNotFirstInLoop<!>() {
    var i = 0
    while (i < 10) {
        i = i + 1
        <!IGNORED_SPEC_BLOCK!>loopInvariants {
            i <= 10
        }<!>
    }
}
