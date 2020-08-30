import com.n9mtq4.av1grain.Dav1dFilmGrainData
import com.n9mtq4.av1grain.imin
import java.awt.Color
import java.awt.image.BufferedImage

fun main(args: Array<String>) {
	
	val pictureHeight = 300
	val pictureWidth = 300
	
	val img = BufferedImage(pictureWidth, pictureHeight, BufferedImage.TYPE_INT_RGB)
	
	img.setRGB(0, 0, pictureWidth, pictureHeight, IntArray(pictureWidth * pictureHeight) { Color(255 / 2, 255 / 2, 255 / 2).rgb }, 0, 1)
	
	// configure grain info
	val data = Dav1dFilmGrainData()
	data.ar_coeff_lag = 3
	data.ar_coeff_shift = 8
	data.grain_scale_shift = 0
	data.scaling_shift = 11
	data.chroma_scaling_from_luma = 0
	data.overlap_flag = 1
	data.uv_mult[0] = 128
	data.uv_luma_mult[0] = 192
	data.uv_offset[0] = 256
	data.uv_mult[1] = 128
	data.uv_luma_mult[1] = 192
	data.uv_offset[1] = 256
	
	data.num_y_points = 6
	data.y_points[0] = intArrayOf(0, 22)
	data.y_points[1] = intArrayOf(13, 22)
	data.y_points[2] = intArrayOf(40, 30)
	data.y_points[3] = intArrayOf(54, 32)
	data.y_points[4] = intArrayOf(67, 32)
	data.y_points[5] = intArrayOf(255, 32)
	
	data.num_uv_points[0] = 5
	data.num_uv_points[1] = 6
	
	data.uv_points[0][0] = intArrayOf(0, 14)
	data.uv_points[0][1] = intArrayOf(27, 15)
	data.uv_points[0][2] = intArrayOf(54, 27)
	data.uv_points[0][3] = intArrayOf(67, 28)
	data.uv_points[0][4] = intArrayOf(255, 29)
	
	data.uv_points[1][0] = intArrayOf(0, 6)
	data.uv_points[1][0] = intArrayOf(13, 6)
	data.uv_points[1][0] = intArrayOf(27, 8)
	data.uv_points[1][0] = intArrayOf(54, 17)
	data.uv_points[1][0] = intArrayOf(67, 20)
	data.uv_points[1][0] = intArrayOf(255, 21)
	
	data.ar_coeffs_y = intArrayOf(3, 2, 0, 2, 7, 5, 5, 1, 2, -4, -18, -2, 5, 5, 2, -5, -10, 46, 23, 3, 7, 5, -21, 62)
	data.ar_coeffs_uv[0] = intArrayOf(3, -1, 0, -2, 5, 3, 3, 0, -2, 4, -22, 0, 5, 4, -2, 1, -18, 54, 14, -1, 4, 2, -22, 56, -3)
	data.ar_coeffs_uv[1] = intArrayOf(5, -1, 3, -8, 6, 3, 4, 0, 1, 3, -17, 0, 6, 2, 3, 0, -17, 64, 13, 0, 7, 0, -12, 57, -5)
	
	// create image raster
	val srcYRaster = extractRasterY(img)
	val srcURaster = extractRasterU(img, ss = 1)
	val srcVRaster = extractRasterV(img, ss = 1)
	
	val dstYRaster = srcYRaster.copyOf()
	val dstURaster = srcURaster.copyOf()
	val dstVRaster = srcVRaster.copyOf()
	
	// compute lut
	val grainLut = Array(3) { Array(GRAIN_HEIGHT + 1) { IntArray(GRAIN_WIDTH) { 0 } } }
	generate_grain_y_c(grainLut[0], data)
	if (data.num_uv_points[0] > 0 || data.chroma_scaling_from_luma != 0) {
		generate_grain_uv_c(grainLut[1], grainLut[1], data, 0, 1, 0)
	}
	if (data.num_uv_points[1] > 0 || data.chroma_scaling_from_luma != 0) {
		generate_grain_uv_c(grainLut[2], grainLut[1], data, 1, 1, 0)
	}
	
	// generate scaling
	val scaling = Array(3) { IntArray(SCALING_SIZE) { 0 } }
	if (data.num_y_points > 0) generate_scaling(8, data.y_points, data.num_y_points, scaling[0])
	if (data.num_uv_points[0] > 0) generate_scaling(8, data.uv_points[0], data.num_uv_points[0], scaling[1])
	if (data.num_uv_points[1] > 0) generate_scaling(8, data.uv_points[1], data.num_uv_points[1], scaling[2])
	
	// apply the noise
	val rows = (pictureHeight + 31) shr 5
	val ss_y = 0
	val ss_x = 1
	val cpw = (pictureWidth + ss_x) shr ss_x
	val is_id = 0
	
	for (row in 0 until rows - 1) {
		
		// generate y noise
		val luma_src = IntArray(pictureWidth * BLOCK_SIZE)
		for (i in 0 until BLOCK_SIZE) {
			for (j in 0 until pictureWidth) {
				luma_src[i * pictureWidth + j] = srcYRaster[row * BLOCK_SIZE + i][j]
			}
		}
		val luma_dst = IntArray(pictureWidth * BLOCK_SIZE) { 255 }
		
		if (data.num_y_points != 0) {
			val bh = imin(pictureHeight - row * BLOCK_SIZE, BLOCK_SIZE)
			fgy_32x32xn_c(luma_dst, luma_src, pictureWidth, data, pictureWidth, scaling[0], grainLut[0], bh, row)
		}
		
		for (i in 0 until BLOCK_SIZE) {
			for (j in 0 until pictureWidth) {
				dstYRaster[row * BLOCK_SIZE + i][j] = luma_dst[i * pictureWidth + j]
			}
		}
		
		// TODO: there's a continue here if uv noise isn't a thing
		
		val bh = (imin(pictureHeight - row * BLOCK_SIZE, BLOCK_SIZE) + ss_y) shr ss_y
		
		// TODO: expand padding pixels?
		
		val uv_off = row * BLOCK_SIZE * PXSTRIDE(300) shr ss_y
		
		val uv_ss = 1
		
		val u_src = IntArray(pictureWidth / uv_ss * BLOCK_SIZE)
		val v_src = IntArray(pictureWidth / uv_ss * BLOCK_SIZE)
		val u_dst = u_src.copyOf()
		val v_dst = v_src.copyOf()
		
		val uv_src = arrayOf(u_src, v_src)
		val uv_dst = arrayOf(u_dst, v_dst)
		
		for (i in 0 until BLOCK_SIZE) {
			for (j in 0 until (pictureWidth / uv_ss)) {
				u_src[i * pictureWidth / uv_ss + j] = srcURaster[row * BLOCK_SIZE + i][j]
				v_src[i * pictureWidth / uv_ss + j] = srcVRaster[row * BLOCK_SIZE + i][j]
			}
		}
		
		// uv noise
		// TODO: this doesn't support subsampling
		if (data.chroma_scaling_from_luma != 0) {
			
			for (pl in 0 until 2) {
				val in_stride_1 = pictureWidth
				val in_stride_0 = pictureWidth
				fguv_32x32xn_c(uv_dst[pl], uv_src[pl], in_stride_1, data, cpw, scaling[0], grainLut[1 + pl], bh, row, luma_src, in_stride_0, pl, is_id, ss_x, ss_y)
			}
			
			
		} else {
			
			for (pl in 0 until 2) {
				if (data.num_uv_points[pl] != 0) {
					val in_stride_1 = pictureWidth
					val in_stride_0 = pictureWidth
					fguv_32x32xn_c(uv_dst[pl], uv_src[pl], in_stride_1, data, cpw, scaling[1 + pl], grainLut[1 + pl], bh, row, luma_src, in_stride_0, pl, is_id, ss_x, ss_y)
				}
			}
			
		}
		
		
		for (i in 0 until BLOCK_SIZE) {
			for (j in 0 until (pictureWidth / uv_ss)) {
				dstURaster[row * BLOCK_SIZE + i][j] = u_dst[i * pictureWidth / uv_ss + j]
				dstVRaster[row * BLOCK_SIZE + i][j] = v_dst[i * pictureWidth / uv_ss + j]
			}
		}
		
	}
	
	// load pixels from yuv into buffered image
	val newImg = createImg(dstYRaster, dstURaster, dstVRaster, ss = 1)
	
	println("done")
	
}

fun rgb2yuv(R: Int, G: Int, B: Int): Triple<Int, Int, Int> {
	// https://gist.github.com/yohhoy/dafa5a47dade85d8b40625261af3776a
	val a = 0.2126
	val b = 0.7152
	val c = 0.0722
	val d = 1.8556
	val e = 1.5748
	val Y  = a * R + b * G + c * B
	val Cb = (B - Y) / d
	val Cr = (R - Y) / e
	return Triple(Y.toInt(), Cb.toInt(), Cr.toInt())
}

fun yuv2rgb(Y: Int, Cb: Int, Cr: Int): Triple<Int, Int, Int> {
	// https://gist.github.com/yohhoy/dafa5a47dade85d8b40625261af3776a
	val a = 0.2126
	val b = 0.7152
	val c = 0.0722
	val d = 1.8556
	val e = 1.5748
	val R = Y + e * Cr
	val G = Y - (a * e / b) * Cr - (c * d / b) * Cb
	val B = Y + d * Cb
	return Triple(R.toInt().coerceIn(0, 255), G.toInt().coerceIn(0, 255), B.toInt().coerceIn(0, 255))
}
