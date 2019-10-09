package com.ikangtai.shecare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.ikangtai.shecare.tensorflow.Classifier;
import com.ikangtai.shecare.tensorflow.ShecareTensorFlowImageClassifier;

import java.io.File;
import java.io.IOException;

import io.reactivex.schedulers.Schedulers;

public class TensorflowActivity extends AppCompatActivity {

    private final String TAG = "TensorflowActivity";
    private static final String MODEL_SHECARE_FILE = "file:///android_asset/model/hed_graph.pb";
    private static final int INPUT_SIZE = 256;
    private static final int IMAGE_MEAN = 256;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "hed_input:0";
    private static final String OUTPUT_NAME = "hed/dsn_fuse/conv2d/BiasAdd";
    public static final String isTrainningName = "is_training:0";

    private Classifier classifier;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tensorflow);
        loadData();

        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startImageClassifier();
            }
        });
    }

    private void loadData() {

        if (classifier == null) {
            // 创建 Classifier
            classifier = ShecareTensorFlowImageClassifier.create(TensorflowActivity.this.getAssets(),
                    MODEL_SHECARE_FILE,
                    INPUT_SIZE,
                    IMAGE_MEAN,
                    IMAGE_STD,
                    INPUT_NAME,
                    OUTPUT_NAME);
        }
    }

    /**
     * 开始图片识别匹配
     */
    private void startImageClassifier() {
        Schedulers.io().createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = Environment.getExternalStorageDirectory().getPath();
                    String imagePath = path + File.separator + "c.png";
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    Bitmap croppedBitmap = getScaleBitmap(bitmap, INPUT_SIZE);
                    classifier.recognizeBitmap(croppedBitmap);

                } catch (Exception e) {
                    Log.i(TAG, e.getMessage());
                }

            }
        });

    }


    /**
     * 对图片进行缩放
     *
     * @param bitmap
     * @param size
     * @return
     * @throws IOException
     */
    private static Bitmap getScaleBitmap(Bitmap bitmap, int size) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) size) / width;
        float scaleHeight = ((float) size) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
}
