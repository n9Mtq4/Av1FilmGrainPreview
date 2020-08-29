import com.n9mtq4.av1grain.Dav1dFilmGrainData
import java.awt.Color
import java.awt.image.BufferedImage

fun main(args: Array<String>) {
	
	val img = BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB)
	
	val data = Dav1dFilmGrainData()
	data.num_y_points = 14
	
	
	val imgRaster = Array(300) { IntArray(300) { 255 / 2 } }
	
	generate_grain_y_c(imgRaster, data)
	val grainLut = Array(3) { Array(GRAIN_HEIGHT + 1) { IntArray(GRAIN_WIDTH) { 0 } } }
	generate_grain_y_c(grainLut[0], data)
	val scaling = Array(3) { IntArray(SCALING_SIZE) { 0 } }
	generate_scaling(8, data.y_points, data.num_y_points, scaling[0])
	fgy_32x32xn_c(imgRaster[0], imgRaster[1], 1, data, 100, scaling[0], grainLut[0], 100, 0)
	
	for (y in 0 until 300) {
		for (x in 0 until 300) {
			val grey = imgRaster[x][y]
			val color = Color(grey, grey, grey)
			img.setRGB(x, y, color.rgb)
		}
	}
	
	println("done")
	
}
