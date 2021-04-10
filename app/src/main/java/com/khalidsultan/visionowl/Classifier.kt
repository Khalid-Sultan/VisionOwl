package com.khalidsultan.cataracts

import android.graphics.Bitmap
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import org.pytorch.IValue
import org.pytorch.Module
import android.util.Log

class Classifier(modelPath: String?) {
    var model: Module = Module.load(modelPath)
    var mean = floatArrayOf(0.485f, 0.456f, 0.406f)
    var std = floatArrayOf(0.229f, 0.224f, 0.225f)
    fun setMeanAndStd(mean: FloatArray, std: FloatArray) {
        this.mean = mean
        this.std = std
    }

    private fun pre_process(bitmap: Bitmap?, size: Int): Tensor {
        val bmp = Bitmap.createScaledBitmap(bitmap!!, size, size, false)
        return TensorImageUtils.bitmapToFloat32Tensor(bmp, mean, std)
    }

    fun predict(bitmap: Bitmap?): String {
        val tensor = pre_process(bitmap, 224)
        val inputs = IValue.from(tensor)
        val outputs = model.forward(inputs).toTensor()
        val scores = outputs.dataAsFloatArray
        if (scores.size!=1 || scores[0]<0.5){
            return Constants.PT_CLASSES[0]
        }
        return Constants.PT_CLASSES[1]
    }

}