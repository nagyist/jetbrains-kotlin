// TARGET_BACKEND: JVM_IR
// FULL_JDK
import java.io.File;

class A : File("A")

fun foo(): A = TODO()

fun box() = "OK"
