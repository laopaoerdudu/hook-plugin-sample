package com.dev.util

import android.annotation.TargetApi
import android.os.Build
import java.util.*

@TargetApi(Build.VERSION_CODES.KITKAT)
fun Array<String>?.contains(value: String?): Boolean {
    this ?: return false
    for (i in this.indices) {
        if (Objects.equals(this[i], value)) {
            return true
        }
    }
    return false
}

fun Array<String>?.append(newPath: String): Array<String>? {
    if (this.contains(newPath)) {
        return this
    }
    val newPathsCount = 1 + (this?.let { it.size } ?: run { 0 })
    val newPaths = Array(newPathsCount) { "" }
    this?.let {
        System.arraycopy(it, 0, newPaths, 0, it.size)
    }
    newPaths[newPathsCount - 1] = newPath
    return newPaths
}
