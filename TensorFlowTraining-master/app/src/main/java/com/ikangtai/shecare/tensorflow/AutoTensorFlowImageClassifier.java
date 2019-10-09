/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.ikangtai.shecare.tensorflow;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ikangtai.shecare.AutoTestPaper;
import com.ikangtai.shecare.TensorflowActivity;
import com.ikangtai.shecare.utils.OpenCVUtils;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * A classifier specialized to label images using TensorFlow.
 */
public class AutoTensorFlowImageClassifier implements AutoClassifier {
    private static final String TAG = "AutoTensorFlow";

    // Config values.
    private String inputName;
    private String outputName;
    private int inputSize;
    private int imageMean;
    private float imageStd;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private float[] floatValues;
    private float[] outputs;
    private String[] outputNames;

    private boolean logStats = false;

    private TensorFlowInferenceInterface inferenceInterface;

    private AutoTensorFlowImageClassifier() {
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager  The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param inputSize     The input size. A square image of inputSize x inputSize is assumed.
     * @param imageMean     The assumed mean of the image values.
     * @param imageStd      The assumed std of the image values.
     * @param inputName     The label of the image input node.
     * @param outputName    The label of the output node.
     * @throws IOException
     */
    public static AutoClassifier create(
            AssetManager assetManager,
            String modelFilename,
            int inputSize,
            int imageMean,
            float imageStd,
            String inputName,
            String outputName) {
        AutoTensorFlowImageClassifier c = new AutoTensorFlowImageClassifier();
        c.inputName = inputName;
        c.outputName = outputName;
        c.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);
        // The shape of the output is [N, NUM_CLASSES], where N is the batch size.
        final Operation operation = c.inferenceInterface.graphOperation(outputName);
        final int numClasses = (int) operation.output(0).shape().size(1);
        Log.i(TAG, "Read " + c.labels.size() + " labels, output layer size is " + numClasses);

        // Ideally, inputSize could have been retrieved from the shape of the input operation.  Alas,
        // the placeholder node for input in the graphdef typically used does not specify a shape, so it
        // must be passed in as a parameter.
        c.inputSize = inputSize;
        c.imageMean = imageMean;
        c.imageStd = imageStd;

        // Pre-allocate buffers.
        c.outputNames = new String[]{outputName};
        c.intValues = new int[inputSize * inputSize];
        c.floatValues = new float[inputSize * inputSize * 3];
        c.outputs = new float[numClasses];

        return c;
    }


    @Override
    public Observable<String> recognizeBitmap(final String imgPath, final String outPath) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                // Log this method so that it can be analyzed with systrace.
                // Preprocess the image data from 0-255 int to normalized float based
                // on the provided parameters.
                Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
                bitmap = OpenCVUtils.getScaleBitmap(bitmap, 256);
                Mat imageL = bitmapToMat(bitmap);
                Imgcodecs.imwrite(outPath, imageL);
                emitter.onNext(outPath);
            }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<String> recognizeBitmap(final String imgPath) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                // Log this method so that it can be analyzed with systrace.
                // Preprocess the image data from 0-255 int to normalized float based
                // on the provided parameters.
                Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
                bitmap = OpenCVUtils.getScaleBitmap(bitmap, 256);
                Mat imageL = bitmapToMat(bitmap);
                String path = Environment.getExternalStorageDirectory().getPath();
                String picName = System.currentTimeMillis() + ".png";
                String imagePath = path + File.separator + picName;
                Imgcodecs.imwrite(imagePath, imageL);
                emitter.onNext(imagePath);
            }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<String> recognizeBitmap(final Bitmap bitmap) {

        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                // Log this method so that it can be analyzed with systrace.
                // Preprocess the image data from 0-255 int to normalized float based
                // on the provided parameters.
                Mat imageL = bitmapToMat(bitmap);
                String path = Environment.getExternalStorageDirectory().getPath();
                String picName = System.currentTimeMillis() + ".png";
                String imagePath = path + File.separator + picName;
                Imgcodecs.imwrite(imagePath, imageL);
                emitter.onNext(imagePath);
            }
        }).subscribeOn(Schedulers.io());

    }

    @NonNull
    private Mat bitmapToMat(Bitmap bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = ((float) ((val >> 16) & 0xFF)) / 255;
            floatValues[i * 3 + 1] = ((float) ((val >> 8) & 0xFF)) / 255;
            floatValues[i * 3 + 2] = ((float) (val & 0xFF)) / 255;
        }
        // Copy the input data into TensorFlow.
        Log.i(TAG, "feed-start");
        inferenceInterface.feed(inputName, floatValues, 1, inputSize, inputSize, 3);
        boolean[] bb = {false};
        inferenceInterface.feed(AutoTestPaper.isTrainningName, bb);
        Log.i(TAG, "feed-end");
        // Run the inference call.
        Log.i(TAG, "run-start");
        inferenceInterface.run(outputNames, logStats);
        Log.i(TAG, "run-end");
        float[] out1 = new float[inputSize * inputSize];
        // Copy the output Tensor back into the output array.
        Log.i(TAG, "fetch-start");
        inferenceInterface.fetch(outputName, out1);
        Log.i(TAG, "fetch-end");
        int[] out2 = new int[out1.length];
        for (int i = 0; i < out1.length; i++) {
            out2[i] = (int) out1[i] * 255;
        }
        byte[] resultLast = new byte[out2.length];
        OpenCVUtils.intToByte(out2, resultLast, 0);
        Mat imageL = new Mat(inputSize, inputSize, CvType.CV_8UC1);
        imageL.put(0, 0, resultLast);
        return imageL;
    }

    @Override
    public void enableStatLogging(boolean logStats) {
        this.logStats = logStats;
    }

    @Override
    public String getStatString() {
        return inferenceInterface.getStatString();
    }

    @Override
    public void close() {
        inferenceInterface.close();
    }
}