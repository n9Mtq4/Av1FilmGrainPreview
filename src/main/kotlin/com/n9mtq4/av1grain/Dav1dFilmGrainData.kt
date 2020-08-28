package com.n9mtq4.av1grain

/**
 * Created by will on 8/28/20 at 5:36 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */
class Dav1dFilmGrainData {
	
	val seed: Int = 0
	val num_y_points: Int = 0
	val y_points = Array(14) { IntArray(2) { 0 } }
	val chroma_scaling_from_luma = 0
	val num_uv_points = IntArray(2) { 0 }
	val uv_points = Array(2) { Array(10) { IntArray(2) { 0 } } }
	val scaling_shift: Int = 0
	val ar_coeff_lag: Int = 0
	val ar_coeffs_y = IntArray(24) { 0 }
	val ar_coeffs_uv = Array(2) { IntArray(25 + 3) { 0 } }
	val ar_coeff_shift: Int = 0
	val grain_scale_shift: Int = 0
	val uv_mult = IntArray(2) { 0 }
	val uv_luma_mult = IntArray(2) { 0 }
	val uv_offset = IntArray(2) { 0 }
	val overlap_flat: Int = 0
	val clip_to_restricted_range: Int = 0
	
}
