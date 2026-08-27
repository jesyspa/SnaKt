// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

open class `base with global constructor parameter`(x: Any)

class `derive and call super` : `base with global constructor parameter` {
    constructor(x: Any, marker: Int) : super(x)

    constructor(x: @Borrowed Any) : super(<!LOCALITY_MISMATCH!>x<!>)
}

class `chain this constructors with global parameter` {
    constructor(x: Any)

    constructor(x: @Borrowed Any, marker: Int) : this(<!LOCALITY_MISMATCH!>x<!>)
}

open class `base with borrowed constructor parameter`(x: @Borrowed Any)

class `derive and call borrowed super` : `base with borrowed constructor parameter` {
    constructor(x: @Borrowed Any) : super(x)
}

class `chain this constructors with borrowed parameter` {
    constructor(x: @Borrowed Any)

    constructor(x: @Borrowed Any, marker: Int) : this(x)
}
