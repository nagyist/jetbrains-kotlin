// ISSUE: KT-47989

// IGNORE_BACKEND: ANY
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// REASON: red code (see corresponding diagnostic test)
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun box(): String {
    build {
        object: TypeSourceInterface {
            override fun produceTargetTypeBuildee() = this@build
        }
    }
    return "OK"
}




class TargetType

interface TypeSourceInterface {
    fun produceTargetTypeBuildee(): Buildee<TargetType>
}

class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
