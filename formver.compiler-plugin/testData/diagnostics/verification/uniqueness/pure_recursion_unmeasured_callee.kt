// FULL_JDK
// RENDER_PREDICATES
import org.jetbrains.kotlin.formver.plugin.*

class Link(var data: Int, val next: @Unique Link?)

// A bodied @Pure function with no @Unique parameter gets no termination measure, because
// there is no unique predicate to build one from. Viper requires every function called by
// a function that carries a measure to carry one itself, so such a function cannot be
// called from one that does. This records that limitation; the golden holds what Viper
// says about it.
@Pure
fun <!VIPER_TEXT!>twice<!>(n: Int): Int = n * 2

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>lengthTwice<!>(l: @Unique @Borrowed Link?): Int {
    return if (l == null) 0 else twice(1)
}
