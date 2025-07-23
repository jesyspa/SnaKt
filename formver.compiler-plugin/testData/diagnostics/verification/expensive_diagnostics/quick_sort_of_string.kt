// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.*

@NeverConvert
fun String.chooseIndex(): Int {
    preconditions {
        length >= 1
    }
    postconditions<Int> { res ->
        0 <= res && res < length
    }
    return indices.random()
}

@AlwaysVerify
fun String.<!VIPER_TEXT!>minOrMaxChar<!>(calcMin: Boolean): Char {
    preconditions {
        length >= 1
    }
    postconditions<Char> { res ->
        calcMin implies forAll<Int> {
            (0 <= it && it < length) implies (res <= get(it))
        }
        !calcMin implies forAll<Int> {
            (0 <= it && it < length) implies (res >= get(it))
        }
    }
    var res = get(0)
    var i = 1
    while (i < length) {
        loopInvariants {
            0 <= i && i <= length
            calcMin implies forAll<Int> {
                (0 <= it && it < i) implies (res <= get(it))
            }
            !calcMin implies forAll<Int> {
                (0 <= it && it < i) implies (res >= get(it))
            }
        }
        if ((calcMin && get(i) < res) || (!calcMin && get(i) > res)) res = get(i)
        ++i
    }
    return res
}

@AlwaysVerify
fun String.<!VIPER_TEXT!>quickSort<!>() : String {
    postconditions<String> { res ->
        res.length == length
        forAll<Int> {
            (1 <= it && it < length) implies (res[it - 1] <= res[it])
        }
    }
    if (length <= 1) return this

    val minVal = minOrMaxChar(true)
    val maxVal = minOrMaxChar(false)

    return quickSortRec(minVal, maxVal)
}

@AlwaysVerify
fun String.<!VIPER_TEXT!>quickSortRec<!>(minVal: Char, maxVal: Char): String {
    preconditions {
        forAll<Int> {
            (0 <= it && it < length) implies (minVal <= get(it) && get(it) <= maxVal)
        }
    }
    postconditions<String> { res ->
        res.length == length
        forAll<Int> { it ->
            (0 <= it && it < length) implies (minVal <= res[it] && res[it] <= maxVal)
        }
        forAll<Int> { it ->
            (1 <= it && it < length) implies (res[it - 1] <= res[it])
        }
    }
    if (length <= 1) return this
    val medVal = get(chooseIndex())
    var i = 0
    var lessString = ""
    var greaterString = ""
    var eqString = ""

    while (i < length) {
        loopInvariants {
            0 <= i && i <= length
            lessString.length + greaterString.length + eqString.length == i
            forAll<Int> {
                (0 <= it && it < lessString.length) implies
                        (minVal <= lessString[it] && lessString[it] <= medVal)
            }
            forAll<Int> {
                (0 <= it && it < greaterString.length) implies
                        (medVal <= greaterString[it] && greaterString[it] <= maxVal)
            }
            forAll<Int> {
                (0 <= it && it < eqString.length) implies
                        (eqString[it] == medVal)
            }
        }
        var curChar = get(i++)
        when {
            curChar < medVal -> lessString += curChar
            curChar > medVal -> greaterString += curChar
            else -> eqString += curChar
        }
    }
    return lessString.quickSortRec(minVal, medVal) + eqString + greaterString.quickSortRec(medVal, maxVal)
}
