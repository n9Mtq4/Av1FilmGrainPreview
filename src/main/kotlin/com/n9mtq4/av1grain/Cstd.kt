package com.n9mtq4.av1grain

import kotlin.math.min

/**
 * Created by will on 8/17/20 at 4:45 PM.
 * 
 * Some functions to easy the transition from C to Kotlin
 * 
 * @author Will "n9Mtq4" Bresnahan
 */

fun clamp(v: Int, min: Int, max: Int) = v.coerceIn(min, max)
fun iclip(v: Int, min: Int, max: Int) = clamp(v, min, max)

fun imin(a: Int, b: Int) = min(a, b)

fun memset(arr: IntArray, value: Int, size: Int) {
	for (i in 0 until size) arr[i] = value
}

fun memcpy(dst: IntArray, src: IntArray, size: Int, dst_offset: Int = 0, src_offset: Int = 0) {
	for (i in 0 until size) dst[dst_offset + i] = src[src_offset + i]
}
