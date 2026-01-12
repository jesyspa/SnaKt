// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Manual

class ManualPermissionFields(val a: Int, @property:Manual var b: Int)

fun <!VIPER_TEXT!>testManualPermissionFieldGetter<!>(mpf: ManualPermissionFields) {
    val a = mpf.a
    val b = mpf.b
}

fun <!VIPER_TEXT!>testManualPermissionFieldSetter<!>(mpf: ManualPermissionFields) {
    mpf.b = 123
}