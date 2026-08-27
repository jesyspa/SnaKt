// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@Manual
class Cell(
    @Unique var value: Int
)

// Writing a field of a @Manual class without unfolding its predicate must fail:
// the permission to the field is still held inside the folded predicate.
fun <!VIPER_TEXT!>writeWithoutUnfold<!>(@Unique c: Cell) {
    <!VIPER_VERIFICATION_ERROR!>c.value = 5<!>
}

// Unfolding the predicate and returning without folding it back must fail:
// the borrowed parameter's predicate is not re-established for the caller.
<!VIPER_VERIFICATION_ERROR!>fun <!VIPER_TEXT!>unfoldWithoutRefold<!>(@Unique @Borrowed c: Cell) {
    unfold(UniquePred(c))
}<!>
