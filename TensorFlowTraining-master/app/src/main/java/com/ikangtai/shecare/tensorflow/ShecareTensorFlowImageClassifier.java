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
import android.os.Environment;
import android.os.Trace;
import android.text.TextUtils;
import android.util.Log;

import com.ikangtai.shecare.TensorflowActivity;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Tensor;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

/**
 * A classifier specialized to label images using TensorFlow.
 */
public class ShecareTensorFlowImageClassifier implements Classifier {
    private static final String TAG = "ShecareTensorFlow";

    // Only return this many results with at least this confidence.
    private static final int MAX_RESULTS = 3;
    private static final float THRESHOLD = 0.1f;

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

    private ShecareTensorFlowImageClassifier() {
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
    public static Classifier create(
            AssetManager assetManager,
            String modelFilename,
            int inputSize,
            int imageMean,
            float imageStd,
            String inputName,
            String outputName) {
        ShecareTensorFlowImageClassifier c = new ShecareTensorFlowImageClassifier();
        c.inputName = inputName;
        c.outputName = outputName;
        c.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);

//        Graph graph = c.inferenceInterface.graph();
//
//        Iterator<Operation> iterators = graph.operations();
//
//        while (iterators.hasNext()) {
//            Operation operation = iterators.next();
//            String operationName = operation.name();
//            Log.i(TAG, "operationName>>>" + operationName);
//        }

        // The shape of the output is [N, NUM_CLASSES], where N is the batch size.
//        final Operation operation = c.inferenceInterface.graphOperation(outputName);
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
    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        return null;
    }

    @Override
    public void recognizeBitmap(Bitmap bitmap) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");
        Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = ((float) ((val >> 16) & 0xFF)) / 255;
            floatValues[i * 3 + 1] = ((float) ((val >> 8) & 0xFF)) / 255;
            floatValues[i * 3 + 2] = ((float) (val & 0xFF)) / 255;
        }
        Trace.endSection();

//        float[] floatArray = new float[256 * 256];

        // Copy the input data into TensorFlow.
        Log.i(TAG, "feed-start");
        Trace.beginSection("feed");
        inferenceInterface.feed(inputName, floatValues, 1, inputSize, inputSize, 3);

        boolean[] bb = {false};
        inferenceInterface.feed(TensorflowActivity.isTrainningName, bb);
        Trace.endSection();
        Log.i(TAG, "feed-end");

        // Run the inference call.
        Log.i(TAG, "run-start");
        Trace.beginSection("run");
        inferenceInterface.run(outputNames, logStats);
        Trace.endSection();
        Log.i(TAG, "run-end");


        float[] out1 = new float[inputSize * inputSize];
//        Tensor t = Tensor.create(out1);
        // Copy the output Tensor back into the output array.
        Log.i(TAG, "fetch-start");
        Trace.beginSection("fetch");
        inferenceInterface.fetch(outputName, out1);
        Trace.endSection();
        Log.i(TAG, "fetch-end");

        int[] out2 = new int[out1.length];

        for (int i = 0; i < out1.length; i++) {
            out2[i] = (int) out1[i] * 255;
        }


        byte[] resultLast = new byte[out2.length];
        intToByte(out2, resultLast, 0);
        Mat imageL = new Mat(inputSize, inputSize, CvType.CV_8UC1);
        imageL.put(0, 0, resultLast);
        String path = Environment.getExternalStorageDirectory().getPath();
        String imagePath = path + File.separator + "r7.png";
        Imgcodecs.imwrite(imagePath, imageL);

    }

    public static void intToByte(int[] source, byte[] result, int start) {
        int length = source.length;
        for (int i = start; i < length; i++) {
            result[i - start] = (byte) source[i];
        }
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