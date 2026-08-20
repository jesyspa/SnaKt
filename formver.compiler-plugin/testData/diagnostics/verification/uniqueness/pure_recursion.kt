// FULL_JDK
// RENDER_PREDICATES
import org.jetbrains.kotlin.formver.plugin.*

class Link(var data: Int, val next: @Unique Link?)

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>length<!>(l: @Unique @Borrowed Link?): Int {
    postconditions<Int> { res -> res >= 0 }
    return if (l == null) 0 else 1 + length(l.next)
}

@AlwaysVerify
fun <!VIPER_TEXT!>callLength<!>(l: @Unique @Borrowed Link?) {
    verify(length(l) == length(l))
}

@AlwaysVerify
fun <!VIPER_TEXT!>pushFront<!>(tail: @Unique Link?, d: Int): @Unique Link {
    postconditions<Link> { new -> length(new) == old(length(tail)) + 1 }
    return Link(d, tail)
}
