import com.n9mtq4.av1grain.Dav1dFilmGrainData
import com.n9mtq4.av1grain.imin
import java.awt.Color
import java.awt.image.BufferedImage

fun main(args: Array<String>) {
	
	val pictureHeight = 300
	val pictureWidth = 300
	
	val img = BufferedImage(pictureWidth, pictureHeight, BufferedImage.TYPE_INT_RGB)
	
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
	
	// create image raster
	val imgRaster = Array(pictureWidth) { IntArray(pictureHeight) { 255 / 2 } }
	val srcImgRaster = imgRaster.copyOf()
	
	// compute lut
	val grainLut = Array(3) { Array(GRAIN_HEIGHT + 1) { IntArray(GRAIN_WIDTH) { 0 } } }
	generate_grain_y_c(grainLut[0], data)
	
	// apply noise
	val scaling = Array(3) { IntArray(SCALING_SIZE) { 0 } }
	generate_scaling(8, data.y_points, data.num_y_points, scaling[0])
	
	// apply the noise
	val rows = (pictureHeight + 31) shr 5
	val ss_y = 0
	val ss_x = 1
	val cpw = (pictureWidth + ss_x) shr ss_x
	val is_id = 0
	
	for (row in 0 until rows - 1) {
		
		val luma_src = IntArray(pictureWidth * BLOCK_SIZE)
		for (i in 0 until BLOCK_SIZE) {
			for (j in 0 until pictureWidth) {
				luma_src[i * pictureWidth + j] = srcImgRaster[row * BLOCK_SIZE + i][j]
			}
		}
		val luma_dst = IntArray(pictureWidth * BLOCK_SIZE) { 255 }
		
		if (data.num_y_points != 0) {
			val bh = imin(pictureHeight - row * BLOCK_SIZE, BLOCK_SIZE)
			fgy_32x32xn_c(luma_dst, luma_src, pictureWidth, data, pictureWidth, scaling[0], grainLut[0], bh, row)
		}
		
		for (i in 0 until BLOCK_SIZE) {
			for (j in 0 until pictureWidth) {
				imgRaster[row * BLOCK_SIZE + i][j] = luma_dst[i * pictureWidth + j]
			}
		}
		
	}
	
	// load pixels from yuv into buffered image
	for (y in 0 until pictureHeight) {
		for (x in 0 until pictureWidth) {
			val grey = imgRaster[x][y]
			val color = Color(grey, grey, grey)
			img.setRGB(x, y, color.rgb)
		}
	}
	
	println("done")
	
}
