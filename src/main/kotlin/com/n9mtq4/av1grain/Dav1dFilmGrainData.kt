package com.n9mtq4.av1grain

/**
 * Created by will on 8/28/20 at 5:36 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */
class Dav1dFilmGrainData {
	
	var seed: Int = 0
	var num_y_points: Int = 0
	var y_points = Array(14) { IntArray(2) { 0 } }
	var chroma_scaling_from_luma = 0
	var num_uv_points = IntArray(2) { 0 }
	var uv_points = Array(2) { Array(10) { IntArray(2) { 0 } } }
	var scaling_shift: Int = 0
	var ar_coeff_lag: Int = 0
	var ar_coeffs_y = IntArray(24) { 0 }
	var ar_coeffs_uv = Array(2) { IntArray(25 + 3) { 0 } }
	var ar_coeff_shift: Int = 0
	var grain_scale_shift: Int = 0
	var uv_mult = IntArray(2) { 0 }
	var uv_luma_mult = IntArray(2) { 0 }
	var uv_offset = IntArray(2) { 0 }
	var overlap_flag: Int = 0
	var clip_to_restricted_range: Int = 0
	
}
