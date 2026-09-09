// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

/*
 * Codeforces 734A: Anton and Danik
 * Source: https://codeforces.com/problemset/problem/734/A
 *
 * Paraphrased context: `games` records one winner per game (`A` for Anton and
 * `D` for Danik).  Return 1 when Anton has more wins, 0 for a draw, and -1
 * when Danik has more wins.
 */

/** Mathematical score of games[0 until end], used by the executable contract. */
@AlwaysVerify
@Pure
fun antonDanikPrefixBalance(games: String, end: Int): Int {
    preconditions {
        0 <= end && end <= games.length
        forAll<Int> {
            (0 <= it && it < end) implies (games[it] == 'A' || games[it] == 'D')
        }
    }
    return if (end == 0) {
        0
    } else {
        antonDanikPrefixBalance(games, end - 1) +
            if (games[end - 1] == 'A') 1 else -1
    }
}

@AlwaysVerify
fun antonAndDanik(games: String): Int {
    preconditions {
        forAll<Int> {
            (0 <= it && it < games.length) implies
                (games[it] == 'A' || games[it] == 'D')
        }
    }
    postconditions<Int> { res ->
        res == -1 || res == 0 || res == 1
        (res == 1) == (antonDanikPrefixBalance(games, games.length) > 0)
        (res == 0) == (antonDanikPrefixBalance(games, games.length) == 0)
        (res == -1) == (antonDanikPrefixBalance(games, games.length) < 0)
    }

    var i = 0
    var balance = 0
    while (i < games.length) {
        loopInvariants {
            0 <= i && i <= games.length
            balance == antonDanikPrefixBalance(games, i)
            forAll<Int> {
                (0 <= it && it < i) implies
                    (games[it] == 'A' || games[it] == 'D')
            }
        }
        if (games[i] == 'A') {
            balance += 1
        } else {
            balance -= 1
        }
        i += 1
    }

    return if (balance > 0) 1 else if (balance < 0) -1 else 0
}
