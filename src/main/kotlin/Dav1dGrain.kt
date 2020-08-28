import com.n9mtq4.av1grain.Dav1dFilmGrainData
import com.n9mtq4.av1grain.gaussian_sequence
import com.n9mtq4.av1grain.iclip

/**
 * Created by will on 8/17/20 at 5:07 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */

// stuff from src/film_grain.h
const val GRAIN_WIDTH = 82
const val GRAIN_HEIGHT = 73
const val BLOCK_SIZE = 32

// hacks for preprocessor stuff
const val bitdepth_max = 8
fun bitdepth_from_max(x: Int) = 8

// stuff from src/film_grain_tmpl.c
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

fun generate_grain_y_c(buf: IArray2D, data: Dav1dFilmGrainData) {
	
	val bitdepth_min_8: Int = bitdepth_from_max(bitdepth_max) - 8
	random_number_state = data.seed
	val shift = 4 - bitdepth_min_8 + data.grain_scale_shift
	val grain_ctr = 128 shl bitdepth_min_8
	val grain_min = -grain_ctr
	val grain_max = grain_ctr - 1
	
	for (y in 0 until GRAIN_HEIGHT) {
		for (x in 0 until GRAIN_WIDTH) {
			val value = get_random_number(11 /*, &seed*/)
			buf[y][x] = round2(gaussian_sequence.get(value), shift)
		}
	}
	
	val ar_pad = 3
	val ar_lag = data.ar_coeff_lag
	
	for (y in ar_pad until GRAIN_HEIGHT) {
		for (x in ar_pad until GRAIN_WIDTH - ar_pad) {
			// const int8_t *coeff = data.ar_coeffs_y;
			var coeff = 0
			var sum = 0
			for (dy in -ar_lag..0) {
				for (dx in -ar_lag..ar_lag) {
					if (dx == 0 && dy == 0) break
					// sum += *(coeff++) * buf[y + dy][x + dx];
					sum += data.ar_coeffs_y[coeff++] * buf[y + dy][x + dx]
				}
			}
			val grain = buf[y][x] + round2(sum, data.ar_coeff_shift)
			buf[y][x] = iclip(grain, grain_min, grain_max)
		}
	}
	
}


fun generate_grain_uv_c(buf: IArray2D, buf_y: IArray2D, data: Dav1dFilmGrainData, uv: Int, subx: Int, suby: Int) {
	
	val bitdepth_min_8 = bitdepth_from_max(bitdepth_max) - 8
	// unsigned seed = data.seed ^ ((uv!=0) ? 0x49d8 : 0xb524);
	random_number_state = data.seed xor if (uv != 0) 0x49d8 else 0xb524
	val shift = 4 - bitdepth_min_8 + data.grain_scale_shift
	val grain_ctr = 128 shl bitdepth_min_8
	val grain_min = -grain_ctr
	val grain_max = grain_ctr - 1
	
	val chromaW = if (subx != 0) SUB_GRAIN_WIDTH else GRAIN_WIDTH
	val chromaH = if (suby != 0) SUB_GRAIN_HEIGHT else GRAIN_HEIGHT
	
	for (y in 0 until chromaH) {
		for (x in 0 until chromaW) {
			val value = get_random_number(11)
			buf[y][x] = round2(gaussian_sequence[value], shift)
		}
	}
	
	val ar_pad = 3
	val ar_lag = data.ar_coeff_lag
	
	for (y in ar_pad until chromaH) {
		for (x in ar_pad until chromaW - ar_pad) {
			// int8_t *coeff = data.ar_coeffs_uv[uv];
			var coeff = 0
			var sum = 0
			for (dy in -ar_lag..0) {
				for (dx in -ar_lag..ar_lag) {
					// For the final (current) pixel, we need to add in the
					// contribution from the luma grain texture
					if (dx == 0 && dy == 0) {
						if (!(data.num_y_points != 0)) break
						var luma = 0
						val lumaX = (x - ar_pad shl subx) + ar_pad
						val lumaY = (y - ar_pad shl suby) + ar_pad
						for (i in 0..suby) {
							for (j in 0..subx) {
								luma += buf_y[lumaY + i][lumaX + j]
							}
						}
						luma = round2(luma, subx + suby)
						// sum += luma * (*coeff);
						sum += luma * data.ar_coeffs_uv[uv][coeff]
						break
					}
					
					// sum += *(coeff++) * buf[y + dy][x + dx];
					sum += data.ar_coeffs_uv[uv][coeff++] * buf[y + dy][x + dx]
				}
			}
			val grain = buf[y][x] + round2(sum, data.ar_coeff_shift)
			buf[y][x] = iclip(grain, grain_min, grain_max)
		}
	}
	
}

