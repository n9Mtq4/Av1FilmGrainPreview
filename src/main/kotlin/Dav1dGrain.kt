/**
 * Created by will on 8/17/20 at 5:07 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */

const val SUB_GRAIN_WIDTH = 44
const val SUB_GRAIN_HEIGHT = 38

var random_number_state = 0
fun get_random_number(bits: Int): Int {
	
	val r = random_number_state
	val bit = r shr 0 xor (r shr 1) xor (r shr 3) xor (r shr 12) and 1
	random_number_state = r shr 1 or (bit shl 15)
	return random_number_state shr 16 - bits and (1 shl bits) - 1
	
}

fun round2(x: Int, shift: Int): Int {
	return (x + (1 shl shift shr 1)) shr shift
}

fun generate_grain_y_c(buf: IArray2D, data: AomFilmGrainT) {
	
	
	
}
