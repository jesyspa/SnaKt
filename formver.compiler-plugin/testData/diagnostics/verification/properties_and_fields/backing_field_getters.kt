import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

class Z
class Y {
    val z = Z()
}

class X {
    val y = Y()
}

@AlwaysVerify
fun <!VIPER_TEXT!>cascadeGet<!>(x: X): Z {
    return x.y.z
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>receiverNotNullProved<!>(x: X?): Boolean {
    contract {
        returns(true) implies (x != null)
    }
    return x?.y != null
}

class NullableY {
    val z: Z? = Z()
}

class NullableX {
    val y: NullableY? = NullableY()
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>cascadeNullableGet<!>(x: NullableX?): Z? {
    contract {
        returnsNotNull() implies (x != null)
    }
    return x?.y?.z
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>cascadeNullableSmartcastGet<!>(x: NullableX?): Z? {
    contract {
        returnsNotNull() implies (x != null)
    }
    return if (x == null) null else if (x.y == null) null else x.y.z
}

class Baz {
    val x: Int = 4
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullableReceiverNotNullSafeGet<!>() {
    val f: Baz? = Baz()
    val cond1 = f?.x != null
    verify(cond1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullableReceiverNullSafeGet<!>() {
    val f: Baz? = null
    val cond1 = f?.x == null
    verify(cond1)
}

@Suppress("UNNECESSARY_SAFE_CALL")
@AlwaysVerify
fun <!VIPER_TEXT!>nonNullableReceiverSafeGet<!>() {
    val f: Baz = Baz()
    val cond1 = f?.x != null
    verify(cond1)
}

open class ClassI(val x: Int, val y: Int) {
    open val z: Z = Z()
}

class ClassII(final override val z: Z) : ClassI(10, 10)

@AlwaysVerify
fun <!VIPER_TEXT!>checkPrimary<!>(x: Int, y: Int) {
    val classI = ClassI(x, y)
    val z = Z()
    val cond1 = x != y || classI.x == classI.y
    val cond2 = ClassII(z).z == z
    verify(
        cond1,
        cond2
    )
}