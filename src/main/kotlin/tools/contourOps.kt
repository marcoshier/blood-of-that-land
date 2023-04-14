package tools

import boofcv.alg.filter.binary.BinaryImageOps
import boofcv.alg.filter.binary.GThresholdImageOps
import boofcv.alg.filter.binary.ThresholdImageOps
import boofcv.struct.ConnectRule
import boofcv.struct.image.GrayU8
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.boofcv.binding.toShapeContours
import org.openrndr.draw.ColorBuffer
import org.openrndr.shape.ShapeContour


fun computeContours(image: ColorBuffer): List<ShapeContour> {
    val input = image.toGrayF32()
    val binary = GrayU8(input.width, input.height)

    // Select a global threshold using Otsu's method.
    val threshold = GThresholdImageOps.computeOtsu(input, 0.0, 255.0)
    ThresholdImageOps.threshold(input, binary, threshold.toFloat(), true)

    var filtered = BinaryImageOps.erode8(binary, 1, null)
    filtered = BinaryImageOps.dilate8(filtered, 1, null)

    return BinaryImageOps.contour(filtered, ConnectRule.EIGHT, null).toShapeContours()
}