package com.ikangtai.shecare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class OpenCVActivity extends AppCompatActivity {
    private final String TAG = "OPEN_CV";
    ImageView img1;
    ImageView img2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_opencv);
        img1 = findViewById(R.id.img1);
        img2 = findViewById(R.id.img2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Internal OpenCV library not found.");
        } else {
            Log.e(TAG, "OpenCV library found");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //只有等opencv 加载成功才能进行OpenCV操作
                    handle();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private void handle() {
        //只有等opencv 加载成功才能进行OpenCV操作
        Mat mat = new Mat();
        //Bitmap图片
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cover);
        //Bitmap图片转Mat对象
        Utils.bitmapToMat(bitmap, mat);
        Mat dst = new Mat();
        // 图片转化为灰度图 得到新的Mat dst
        Imgproc.cvtColor(mat, dst, Imgproc.COLOR_BGRA2GRAY);
        //先创建空白 等大小的Bitmap对象
        Bitmap mCacheBitmap = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
        //灰度Mat 写入 空白Bitmap 得到 灰度 Bitmap
        Utils.matToBitmap(dst, mCacheBitmap);
        //显示
        img2.setImageBitmap(mCacheBitmap);
    }
}

