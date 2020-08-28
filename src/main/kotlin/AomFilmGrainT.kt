/**
 * Created by will on 8/17/20 at 3:30 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */

typealias IArray2D = Array<IntArray>

class AomFilmGrainT {
	
	val apply_grain: Int = 0
	
	val update_parameters: Int = 0
	
	val scaling_points_y: IArray2D = Array(14) { IntArray(2) }
	val num_y_points: Int = 0
	
	val scaling_points_cb: IArray2D = Array(10) { IntArray(2) }
	val num_cb_points: Int = 0
	
	val scaling_points_cr: IArray2D = Array(10) { IntArray(2) }
	val num_cr_points: Int = 0
	
	val scaling_shift: Int = 0
	
	val ar_coeff_lag: Int = 0
	
	val ar_coeffs_y: IntArray = IntArray(24)
	val ar_coeffs_cb: IntArray = IntArray(25)
	val ar_coeffs_cr: IntArray = IntArray(25)
	
	val ar_coeff_shift: Int = 0
	
	val cb_mult: Int = 0
	val cb_luma_mult: Int = 0
	val cb_offset: Int = 0
	
	val cr_mult: Int = 0
	val cr_luma_mult: Int = 0
	val cr_offset: Int = 0
	
	val overlap_flag: Int = 0
	
	val clip_to_restricted_range: Int = 0
	
	val bit_depth: Int = 0
	
	val chroma_scaling_from_luma: Int = 0
	
	val grain_scale_shift: Int = 0
	
	val random_seed: Int = 0
	
}
