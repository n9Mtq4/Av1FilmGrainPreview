package com.n9mtq4.av1grain

/**
 * Created by will on 8/17/20 at 4:45 PM.
 * 
 * Some functions to easy the transition from C to Kotlin
 * 
 * @author Will "n9Mtq4" Bresnahan
 */

fun clamp(v: Int, min: Int, max: Int) = v.coerceIn(min, max)

fun memset(arr: IntArray, value: Int, size: Int) {
	for (i in 0 until size) arr[i] = value
}