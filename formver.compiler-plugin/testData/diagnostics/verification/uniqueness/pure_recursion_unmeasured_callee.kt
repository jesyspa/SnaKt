// FULL_JDK
// RENDER_PREDICATES
import org.jetbrains.kotlin.formver.plugin.*

class Link(var data: Int, val next: @Unique Link?)

// `double` has a body and no @Unique parameter, so it gets no measure of its own.
@Pure
fun <!VIPER_TEXT!>double<!>(n: Int): Int = 2 * n

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>doubleLength<!>(l: @Unique @Borrowed Link?): Int {
    return if (l == null) 0 else double(1) + doubleLength(l.next)
}
