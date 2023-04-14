package tools

import boofcv.abst.feature.detect.interest.ConfigFastHessian
import boofcv.abst.feature.detect.interest.ConfigPointDetector
import boofcv.abst.feature.detect.interest.PointDetectorTypes
import boofcv.abst.tracker.PointTracker
import boofcv.alg.filter.derivative.GImageDerivativeOps
import boofcv.alg.tracker.klt.ConfigPKlt
import boofcv.factory.tracker.FactoryPointTracker
import boofcv.struct.image.GrayF32
import boofcv.struct.pyramid.ConfigDiscreteLevels
fun KLT(): PointTracker<GrayF32> {

    val configKlt = ConfigPKlt().apply {
        templateRadius = 3
        pyramidLevels = ConfigDiscreteLevels.levels(4)
    }

    val configDetector = ConfigPointDetector().apply {
        type = PointDetectorTypes.SHI_TOMASI
        general.apply {
            maxFeatures = -1
            radius = 6
            threshold = 1.0f
        }
    }

    val imageType = GrayF32::class.java
    val derivType = GImageDerivativeOps.getDerivativeType(imageType)

    return FactoryPointTracker.klt(configKlt, configDetector, imageType, derivType)
}

fun SURF(): PointTracker<GrayF32> {
    val configDetector = ConfigFastHessian().apply {
        maxFeaturesPerScale = 250
        initialSampleStep = 2
        extract.radius = 3
    }

    val imageType = GrayF32::class.java
    return FactoryPointTracker.dda_FH_SURF_Fast(configDetector, null, null, imageType)
}

