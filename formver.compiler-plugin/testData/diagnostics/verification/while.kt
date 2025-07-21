// REPLACE_STDLIB_EXTENSIONS
// ALWAYS_VALIDATE

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify
import org.jetbrains.kotlin.formver.plugin.NeverConvert

class ClassWithField(val field: Int)

fun <!VIPER_TEXT!>test_while<!>(param: ClassWithField) {
    val c = ClassWithField(13)
    val initParamField = param.field

    // Inserting an additional scope here to check that
    // while invariants don't capture variables from there.
    var iteration =
        if (initParamField > 0) 0
        else {
            val intermediate = - initParamField + 1
            intermediate * intermediate
        }
    while (iteration < 10) {
        val field = c.field
        val paramField = param.field
        iteration = iteration + 1
    }
    val cond1 = c.field == 13
    val cond2 = initParamField == param.field
    verify(cond1)
    verify(cond2)
}

fun <!VIPER_TEXT!>test_while_with_inlining<!>(param: ClassWithField) {
    val local = ClassWithField(13)
    ClassWithField(42).run {
        var iteration = 0
        while (iteration < 10) {
            val paramField = param.field
            val localField = local.field
            val thisField = field
            iteration = iteration + 1
        }
        verify(field == 42)
        verify(local.field == 13)
    }
}

fun <!VIPER_TEXT!>test_while_with_smartcast<!>(param: Any, innerParam: Any) {
    if (param is ClassWithField) {
        var iteration = 0
        while (iteration < 10) {
            val paramField = param.field
            if (innerParam is ClassWithField) {
                val innerParamField = innerParam.field
            }
            iteration = iteration + 1
        }
    }
}



