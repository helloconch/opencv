package com.ikangtai.shecare;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.ikangtai.shecare.utils.Keys;
import com.ikangtai.shecare.utils.OpenCVUtils;
import com.ikangtai.shecare.utils.ToastUtils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class SmartPaperActivity extends AppCompatActivity {

    private ImageView resultImg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_smart_paper);
        resultImg = findViewById(R.id.resultImg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    ToastUtils.show(getApplicationContext(), "OpenCV loaded successfully ");
                    handle();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    private void handle() {
        String path = Environment.getExternalStorageDirectory().getPath();
        Log.i(Keys.TAG, path);
//        String imagePath = path + File.separator + "a.jpg";
//        String imageGayPath = path + File.separator + "b.jpg";
        String imagePath = path + File.separator + "c.png";
        String imageGayPath = path + File.separator + "r7.png";

//        String imagePath = path + File.separator + "m.jpg";
//        String imageGayPath = path + File.separator + "n.jpg";

        OpenCVUtils.handleImg(imagePath, imageGayPath, "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Bitmap>() {
                    @Override
                    public void accept(Bitmap bitmap) throws Exception {
                        if (resultImg != null) {
                            resultImg.setImageBitmap(bitmap);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.i(Keys.TAG, "出现异常:" + throwable.getMessage());
                    }
                });


    }
}
