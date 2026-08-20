// FULL_JDK
// RENDER_PREDICATES
import org.jetbrains.kotlin.formver.plugin.*

class Link(var data: Int, val next: @Unique Link?)

// Recurses on `l` itself rather than on `l.next`, so the inferred measure compares a
// predicate instance against itself and never decreases. This is what stops the measure
// from being vacuous: were it accepted, every recursive @Pure function would verify.
@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>badLength<!>(l: @Unique @Borrowed Link?): Int {
    return if (l == null) 0 else 1 + <!VIPER_VERIFICATION_ERROR!>badLength(l)<!>
}
