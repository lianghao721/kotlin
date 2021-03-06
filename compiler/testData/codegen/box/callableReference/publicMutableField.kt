// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: A.java

public class A {
    public int field = 239;
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    val a = A()
    val f = A::field
    if (f.get(a) != 239) return "Fail 1: ${f.get(a)}"
    f.set(a, 42)
    if (f.get(a) != 42) return "Fail 2: ${f.get(a)}"
    if (f.get(a) != 42) return "Fail 2: ${f.get(a)}"
    return "OK"
}
