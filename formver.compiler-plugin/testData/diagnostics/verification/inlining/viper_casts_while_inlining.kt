import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

fun <T> <!VIPER_TEXT!>idFun<!>(arg: T): T = arg

@NeverConvert
public inline fun <T, R> T.runWithId(block: T.() -> R): R = idFun(this).block()

@NeverConvert
public inline fun <T, R> T.copiedRun(block: T.() -> R): R = block()

@NeverConvert
public inline fun <T> copiedRun(block: () -> T): T = block()

class ClassWithMember(val member: Int)

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>checkMemberAccess<!>(): Boolean {
    contract {
        returns(true)
    }

    val obj = ClassWithMember(42)
    obj.runWithId {
        member
    }
    obj.copiedRun {
        return obj.member == 42
    }
}

class Box<T>(val wrapped: T)

@OptIn(ExperimentalContracts::class)
fun <T> <!VIPER_TEXT!>checkGenericMemberAccess<!>(box: Box<T>): Boolean {
    contract {
        returns(true)
    }

    box.runWithId {
        wrapped
    }

    Box(box.wrapped).copiedRun {
        return wrapped == box.wrapped
    }
}

class ClassWithVar(var a: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>checkArgumentIsCopied<!>(x: ClassWithVar) {
    x.a.copiedRun {
        x.a = 42
        this
    }
}

class ManyMembers(val i: Int, var b: Boolean, val c: ClassWithVar)

@AlwaysVerify
fun <!VIPER_TEXT!>accessManyMembers<!>(m: ManyMembers) {
    m.copiedRun {
        idFun(i)
        idFun(b)
        c
    }
    m.runWithId {
        idFun(i)
        idFun(b)
        c
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>checkEvaluatedOnce<!>(i: Int, mm: ManyMembers) {
    (i + (if (mm.b) 1 else -1)).copiedRun {
        verify(this == this)
    }
}

