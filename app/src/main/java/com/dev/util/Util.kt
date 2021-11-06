package com.dev.util

fun <T, R, P> safeLeft(t: T?, r: R?, block: (T, R) -> P?): P? {
    return if (t != null && r != null) block(t, r) else null
}

fun <A, B, C, D> safeLeft(a: A?, b: B?, c: C?, block: (A, B, C) -> D?): D? {
    return if (a != null && b != null && c != null) block(a, b, c) else null
}
