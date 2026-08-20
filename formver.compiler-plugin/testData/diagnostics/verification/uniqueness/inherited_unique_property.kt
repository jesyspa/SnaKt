// FULL_JDK
// RENDER_PREDICATES
import org.jetbrains.kotlin.formver.plugin.*

open class Node(val child: @Unique Node?)
class Tagged(val tag: Int, child: @Unique Node?) : Node(child)

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>depth<!>(n: @Unique @Borrowed Node?): Int {
    postconditions<Int> { res -> res >= 0 }
    return if (n == null) 0 else 1 + depth(n.child)
}

@AlwaysVerify
fun <!VIPER_TEXT!>depthOfTagged<!>(t: @Unique @Borrowed Tagged) {
    verify(depth(t.child) >= 0)
}
