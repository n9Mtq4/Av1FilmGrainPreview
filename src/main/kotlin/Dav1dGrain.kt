import com.n9mtq4.av1grain.*

/**
 * Created by will on 8/17/20 at 5:07 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */

// stuff from src/film_grain.h
const val GRAIN_WIDTH = 82
const val GRAIN_HEIGHT = 73
const val BLOCK_SIZE = 32
const val SCALING_SIZE = 255

// hacks for preprocessor stuff
const val bitdepth_max = 8
fun bitdepth_from_max(x: Int) = 8

// from include/common/bitdepth.h
// for bd = 8
fun PXSTRIDE(x: Int) = x

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
			buf[y][x] = round2(gaussian_sequence[value], shift)
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

fun sample_lut(grain_lut: IArray2D, offsets: IArray2D, subx: Int, suby: Int, bx: Int, by: Int, x: Int, y: Int): Int {
	
	val randval: Int = offsets[bx][by]
	val offx = 3 + (2 shr subx) * (3 + (randval shr 4))
	val offy = 3 + (2 shr suby) * (3 + (randval and 0xF))
	return grain_lut[offy + y + (BLOCK_SIZE shr suby) * by][offx + x + (BLOCK_SIZE shr subx) * bx]
	
}

fun fgy_32x32xn_c(
	dst_row: IntArray,
	src_row: IntArray,
	stride: Int,
	data: Dav1dFilmGrainData,
	pw: Int,
	scaling: IntArray,
	grain_lut: IArray2D,
	bh: Int,
	row_num: Int
) {
	
	var dst_row_ptr = 0
	var src_row_ptr = 0
	
	val rows: Int = 1 + (if (((data.overlap_flag != 0) && row_num > 0)) 1 else 0)
	val bitdepth_min_8 = bitdepth_from_max(bitdepth_max) - 8
	val grain_ctr = 128 shl bitdepth_min_8
	val grain_min = -grain_ctr
	val grain_max = grain_ctr - 1
	
	val min_value: Int
	val max_value: Int
	if (data.clip_to_restricted_range != 0) {
		min_value = 16 shl bitdepth_min_8
		max_value = 235 shl bitdepth_min_8
	} else {
		min_value = 0
		max_value = 0xff
	}
	
	
	val seed = intArrayOf(0, 0)
	for (i in 0 until rows) {
		seed[i] = data.seed
		seed[i] = seed[i] xor ((row_num - i) * 37 + 178 and 0xFF shl 8)
		seed[i] = seed[i] xor ((row_num - i) * 173 + 105 and 0xFF)
	}
	
	// assert(stride % (BLOCK_SIZE * sizeof(pixel)) == 0);
	
	val offsets = arrayOf(intArrayOf(0, 0), intArrayOf(0, 0))
	
	for (bx in 0 until pw step BLOCK_SIZE) {
		
		val bw: Int = imin(BLOCK_SIZE, pw - bx)
		
		if ((data.overlap_flag != 0) && bx != 0) {
			// shift previous offsets left
			for (i in 0 until rows) offsets[1][i] = offsets[0][i]
		}
		
		// update current offsets
		for (i in 0 until rows) {
			random_number_state = seed[i]
			offsets[0][i] = get_random_number(8)
			// offsets[0][i] = get_random_number(8, &seed[i]);
		}
		
		// x/y block offsets to compensate for overlapped regions
		// x/y block offsets to compensate for overlapped regions
		val ystart = if ((data.overlap_flag != 0) && (row_num != 0)) imin(2, bh) else 0
		val xstart = if ((data.overlap_flag != 0) && (bx != 0)) imin(2, bw) else 0
		
		val w = arrayOf(intArrayOf(27, 17), intArrayOf(17, 27))
		
		for (y in ystart until bh) {
			// Non-overlapped image region (straightforward)
			for (x in xstart until bw) {
				val grain = sample_lut(grain_lut, offsets, 0, 0, 0, 0, x, y)
				
				// add_noise_y(x, y, grain)
				val src = src_row_ptr + (y) * PXSTRIDE(stride) + (x) + bx
				val dst = dst_row_ptr + (y) * PXSTRIDE(stride) + (x) + bx
				val noise = round2(scaling[src_row[src]] * (grain), data.scaling_shift)
				dst_row[dst] = iclip(src_row[src_row_ptr] + noise, min_value, max_value)
				
			}
			
			// Special case for overlapped column
			for (x in 0 until xstart) {
				var grain = sample_lut(grain_lut, offsets, 0, 0, 0, 0, x, y)
				val old = sample_lut(grain_lut, offsets, 0, 0, 1, 0, x, y)
				grain = round2(old * w[x][0] + grain * w[x][1], 5)
				grain = iclip(grain, grain_min, grain_max)
				
				// add_noise_y(x, y, grain)
				val src = src_row_ptr + (y) * PXSTRIDE(stride) + (x) + bx
				val dst = dst_row_ptr + (y) * PXSTRIDE(stride) + (x) + bx
				val noise = round2(scaling[src_row[src]] * (grain), data.scaling_shift)
				dst_row[dst] = iclip(src_row[src_row_ptr] + noise, min_value, max_value)
				
			}
		}
		
		
		for (y in 0 until ystart) {
			// Special case for overlapped row (sans corner)
			for (x in xstart until bw) {
				var grain = sample_lut(grain_lut, offsets, 0, 0, 0, 0, x, y)
				val old = sample_lut(grain_lut, offsets, 0, 0, 0, 1, x, y)
				grain = round2(old * w[y][0] + grain * w[y][1], 5)
				grain = iclip(grain, grain_min, grain_max)
				
				// add_noise_y(x, y, grain)
				val src = src_row_ptr + (y) * PXSTRIDE(stride) + (x) + bx
				val dst = dst_row_ptr + (y) * PXSTRIDE(stride) + (x) + bx
				val noise = round2(scaling[src_row[src]] * (grain), data.scaling_shift)
				dst_row[dst] = iclip(src_row[src_row_ptr] + noise, min_value, max_value)
				
			}
			
			// Special case for doubly-overlapped corner
			for (x in 0 until xstart) {
				// Blend the top pixel with the top left block
				var top = sample_lut(grain_lut, offsets, 0, 0, 0, 1, x, y)
				var old = sample_lut(grain_lut, offsets, 0, 0, 1, 1, x, y)
				top = round2(old * w[x][0] + top * w[x][1], 5)
				top = iclip(top, grain_min, grain_max)
				
				// Blend the current pixel with the left block
				var grain = sample_lut(grain_lut, offsets, 0, 0, 0, 0, x, y)
				old = sample_lut(grain_lut, offsets, 0, 0, 1, 0, x, y)
				grain = round2(old * w[x][0] + grain * w[x][1], 5)
				grain = iclip(grain, grain_min, grain_max)
				
				// Mix the row rows together and apply grain
				grain = round2(top * w[y][0] + grain * w[y][1], 5)
				grain = iclip(grain, grain_min, grain_max)
				
				// add_noise_y(x, y, grain)
				val src = src_row_ptr + (y) * PXSTRIDE(stride) + (x) + bx
				val dst = dst_row_ptr + (y) * PXSTRIDE(stride) + (x) + bx
				val noise = round2(scaling[src_row[src]] * (grain), data.scaling_shift)
				dst_row[dst] = iclip(src_row[src_row_ptr] + noise, min_value, max_value)
				
			}
		}
		
	}
	
}

fun fguv_32x32xn_c(
	dst_row: IntArray,
	src_row: IntArray,
	stride: Int,
	data: Dav1dFilmGrainData,
	pw: Int,
	scaling: IntArray,
	grain_lut: IArray2D,
	bh: Int,
	row_num: Int,
	luma_row: IntArray,
	luma_stride: Int,
	uv: Int,
	is_id: Int,
	sx: Int,
	sy: Int
) {
	
	val rows: Int = 1 + (if (((data.overlap_flag != 0) && row_num > 0)) 1 else 0)
	val bitdepth_min_8 = bitdepth_from_max(bitdepth_max) - 8
	val grain_ctr = 128 shl bitdepth_min_8
	val grain_min = -grain_ctr
	val grain_max = grain_ctr - 1
	
	val min_value: Int
	val max_value: Int
	if (data.clip_to_restricted_range != 0) {
		min_value = 16 shl bitdepth_min_8
		max_value = (if (is_id != 0) 235 else 240) shl bitdepth_min_8
	} else {
		min_value = 0
		max_value = 0xff
	}
	
	val seed = intArrayOf(0, 0)
	for (i in 0 until rows) {
		seed[i] = data.seed
		seed[i] = seed[i] xor ((row_num - i) * 37 + 178 and 0xFF shl 8)
		seed[i] = seed[i] xor ((row_num - i) * 173 + 105 and 0xFF)
	}
	
	val offsets = arrayOf(intArrayOf(0, 0), intArrayOf(0, 0))
	
	for (bx in 0 until pw step (BLOCK_SIZE shr sx)) {
		val bw = imin(BLOCK_SIZE shr sx, pw - bx)
		if ((data.overlap_flag != 0) && (bx != 0)) {
			// shift previous offsets left
			for (i in 0 until rows) offsets[1][i] = offsets[0][i]
		}
		
		// update current offsets
		// update current offsets
		for (i in 0 until rows) {
			random_number_state = seed[i]
			offsets[0][i] = get_random_number(8)
			// offsets[0][i] = get_random_number(8, &seed[i]);
		}
		
		// x/y block offsets to compensate for overlapped regions
		val ystart = if ((data.overlap_flag != 0) && (row_num != 0)) imin(2 shr sy, bh) else 0
		val xstart = if ((data.overlap_flag != 0) && (bx != 0)) imin(2 shr sx, bw) else 0
		
		val w = arrayOf(
			arrayOf(arrayOf(27, 17), arrayOf(17, 27)),
			arrayOf(arrayOf(23, 22))
		)
		
		for (y in ystart until bh) {
			// Non-overlapped image region (straightforward)
			for (x in xstart until bw) {
				val grain = sample_lut(grain_lut, offsets, sx, sy, 0, 0, x, y)
				
				// add_noise_uv(x, y, grain)
				val lx = (bx + x) shl sx
				val ly = y shl sy
				val luma_ptr = ly * PXSTRIDE(luma_stride) + lx
				var avg: Int = luma_row[luma_ptr + 0]
				if (sx != 0) avg = avg + luma_row[luma_ptr + 1] + 1 shr 1
				val src_ptr = y * PXSTRIDE(stride) + (bx + x)
				val dst_ptr = y * PXSTRIDE(stride) + (bx + x)
				var pval = avg
				if (data.chroma_scaling_from_luma == 0) {
					val combined = avg * data.uv_luma_mult[uv] + src_row[src_ptr] * data.uv_mult[uv]
					pval = iclip_pixel((combined shr 6) + data.uv_offset[uv] * (1 shl bitdepth_min_8))
				}
				val noise = round2(scaling[pval] * grain, data.scaling_shift)
				dst_row[dst_ptr] = iclip(src_row[src_ptr] + noise, min_value, max_value)
				
			}
			
			// Special case for overlapped column
			for (x in 0 until xstart) {
				var grain = sample_lut(grain_lut, offsets, sx, sy, 0, 0, x, y)
				val old = sample_lut(grain_lut, offsets, sx, sy, 1, 0, x, y)
				grain = old * w[sx][x][0] + grain * w[sx][x][1] + 16 shr 5
				grain = iclip(grain, grain_min, grain_max)
				
				// add_noise_uv(x, y, grain)
				val lx = (bx + x) shl sx
				val ly = y shl sy
				val luma_ptr = ly * PXSTRIDE(luma_stride) + lx
				var avg: Int = luma_row[luma_ptr + 0]
				if (sx != 0) avg = avg + luma_row[luma_ptr + 1] + 1 shr 1
				val src_ptr = y * PXSTRIDE(stride) + (bx + x)
				val dst_ptr = y * PXSTRIDE(stride) + (bx + x)
				var pval = avg
				if (data.chroma_scaling_from_luma == 0) {
					val combined = avg * data.uv_luma_mult[uv] + src_row[src_ptr] * data.uv_mult[uv]
					pval = iclip_pixel((combined shr 6) + data.uv_offset[uv] * (1 shl bitdepth_min_8))
				}
				val noise = round2(scaling[pval] * grain, data.scaling_shift)
				dst_row[dst_ptr] = iclip(src_row[src_ptr] + noise, min_value, max_value)
				
			}
		}
		
		for (y in 0 until ystart) {
			// Special case for overlapped row (sans corner)
			for (x in xstart until bw) {
				var grain = sample_lut(grain_lut, offsets, sx, sy, 0, 0, x, y)
				val old = sample_lut(grain_lut, offsets, sx, sy, 0, 1, x, y)
				grain = old * w[sy][y][0] + grain * w[sy][y][1] + 16 shr 5
				grain = iclip(grain, grain_min, grain_max)
				
				// add_noise_uv(x, y, grain)
				val lx = (bx + x) shl sx
				val ly = y shl sy
				val luma_ptr = ly * PXSTRIDE(luma_stride) + lx
				var avg: Int = luma_row[luma_ptr + 0]
				if (sx != 0) avg = avg + luma_row[luma_ptr + 1] + 1 shr 1
				val src_ptr = y * PXSTRIDE(stride) + (bx + x)
				val dst_ptr = y * PXSTRIDE(stride) + (bx + x)
				var pval = avg
				if (data.chroma_scaling_from_luma == 0) {
					val combined = avg * data.uv_luma_mult[uv] + src_row[src_ptr] * data.uv_mult[uv]
					pval = iclip_pixel((combined shr 6) + data.uv_offset[uv] * (1 shl bitdepth_min_8))
				}
				val noise = round2(scaling[pval] * grain, data.scaling_shift)
				dst_row[dst_ptr] = iclip(src_row[src_ptr] + noise, min_value, max_value)
				
			}
			
			// Special case for doubly-overlapped corner
			for (x in 0 until xstart) {
				// Blend the top pixel with the top left block
				var top = sample_lut(grain_lut, offsets, sx, sy, 0, 1, x, y)
				var old = sample_lut(grain_lut, offsets, sx, sy, 1, 1, x, y)
				top = old * w[sx][x][0] + top * w[sx][x][1] + 16 shr 5
				top = iclip(top, grain_min, grain_max)
				
				// Blend the current pixel with the left block
				var grain = sample_lut(grain_lut, offsets, sx, sy, 0, 0, x, y)
				old = sample_lut(grain_lut, offsets, sx, sy, 1, 0, x, y)
				grain = old * w[sx][x][0] + grain * w[sx][x][1] + 16 shr 5
				grain = iclip(grain, grain_min, grain_max)
				
				// Mix the row rows together and apply to image
				grain = top * w[sy][y][0] + grain * w[sy][y][1] + 16 shr 5
				grain = iclip(grain, grain_min, grain_max)
				
				// add_noise_uv(x, y, grain)
				val lx = (bx + x) shl sx
				val ly = y shl sy
				val luma_ptr = ly * PXSTRIDE(luma_stride) + lx
				var avg: Int = luma_row[luma_ptr + 0]
				if (sx != 0) avg = avg + luma_row[luma_ptr + 1] + 1 shr 1
				val src_ptr = y * PXSTRIDE(stride) + (bx + x)
				val dst_ptr = y * PXSTRIDE(stride) + (bx + x)
				var pval = avg
				if (data.chroma_scaling_from_luma == 0) {
					val combined = avg * data.uv_luma_mult[uv] + src_row[src_ptr] * data.uv_mult[uv]
					pval = iclip_pixel((combined shr 6) + data.uv_offset[uv] * (1 shl bitdepth_min_8))
				}
				val noise = round2(scaling[pval] * grain, data.scaling_shift)
				dst_row[dst_ptr] = iclip(src_row[src_ptr] + noise, min_value, max_value)
				
			}
		}
		
	}
	
}


fun generate_scaling(bitdepth: Int, points: IArray2D, num: Int, scaling: IntArray) {
	
	val shift_x = 0
	val scaling_size = 1 shl bitdepth
	
	// Fill up the preceding entries with the initial value
	for (i in 0 until (points[0][0] shl shift_x)) {
		scaling[i] = points[0][1]
	}
	
	// Linearly interpolate the values in the middle
	for (i in 0 until num - 1) {
		val bx = points[i][0]
		val by = points[i][1]
		val ex = points[i + 1][0]
		val ey = points[i + 1][1]
		val dx = ex - bx
		val dy = ey - by
		val delta = dy * ((0x10000 + (dx shr 1)) / dx)
		for (x in 0 until dx) {
			val v = by + (x * delta + 0x8000 shr 16)
			scaling[bx + x shl shift_x] = v
		}
	}
	
	// Fill up the remaining entries with the final value
	for (i in (points[num - 1][0] shl shift_x) until scaling_size) {
		scaling[i] = points[num - 1][1]
	}
	
}
