import java.awt.Color
import java.awt.image.BufferedImage

/**
 * Created by will on 8/29/20 at 8:56 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */

fun createImg(yr: IArray2D, u: IArray2D, v: IArray2D, ss: Int = 2): BufferedImage {
	
	val width = yr.size
	val height = yr[0].size
	val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
	
	for (y in 0 until height) {
		for (x in 0 until width) {
			val yp = yr[x][y]
			val up = u[x / ss][y / ss]
			val vp = v[x / ss][y / ss]
			val (r, g, b) = yuv2rgb(yp, up, vp)
			val rgb = Color(r, g, b).rgb
			img.setRGB(x, y, rgb)
		}
	}
	
	return img
	
}

fun extractRasterY(img: BufferedImage): IArray2D {
	
	val yRaster = Array(img.width) { c -> IntArray(img.height) { r ->
		val color = Color(img.getRGB(c, r))
		val (y, _, _) = rgb2yuv(color.red, color.green, color.blue)
		y.toInt()
	} }
	return yRaster
	
}

fun extractRasterU(img: BufferedImage, ss: Int = 2): IArray2D {
	
	val uRaster = Array(img.width / ss) { c -> IntArray(img.height / ss) { r ->
		val color = Color(img.getRGB(ss * c, ss * r))
		val (_, u, _) = rgb2yuv(color.red, color.green, color.blue)
		u.toInt()
	} }
	return uRaster
	
}

fun extractRasterV(img: BufferedImage, ss: Int = 2): IArray2D {
	
	val vRaster = Array(img.width / ss) { c -> IntArray(img.height / ss) { r ->
		val color = Color(img.getRGB(ss * c, ss * r))
		val (_, _, v) = rgb2yuv(color.red, color.green, color.blue)
		v.toInt()
	} }
	return vRaster
	
}
