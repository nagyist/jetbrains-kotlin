trait A<T>
class B() : A<String> {}

fun foo<T, U: A<T>>(a: U) {
    a is B
}