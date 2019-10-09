package com.ikangtai.shecare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ikangtai.shecare.tensorflow.AutoClassifier;
import com.ikangtai.shecare.tensorflow.AutoTensorFlowImageClassifier;
import com.ikangtai.shecare.utils.Keys;
import com.ikangtai.shecare.utils.OpenCVUtils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class AutoTestPaper extends AppCompatActivity {
    private final String TAG = "AutoTestPaper";
    private static final String MODEL_SHECARE_FILE = "file:///android_asset/model/hed_graph.pb";
    private static final int INPUT_SIZE = 256;
    private static final int IMAGE_MEAN = 256;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "hed_input:0";
    private static final String OUTPUT_NAME = "hed/dsn_fuse/conv2d/BiasAdd";
    public static final String isTrainningName = "is_training:0";
    private AutoClassifier autoClassifier;
    private String flag = "\n";
    private StringBuilder hintBuilder = new StringBuilder();
    private ImageView resultImg;
    private String sourcePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "source.png";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_auto_test_paper);
        resultImg = findViewById(R.id.resultImg);
        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleTensorflow();
            }
        });
        initData();
    }

    private void initData() {
        Schedulers.io().createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                File file = new File(sourcePath);
                if (!file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.c);
                    OpenCVUtils.saveBitmap(bitmap, sourcePath);
                }
            }
        });
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
                    addHint("opencv load success");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    private void handleTensorflow() {
        autoClassifier = AutoTensorFlowImageClassifier.create(getAssets(), MODEL_SHECARE_FILE,
                INPUT_SIZE,
                IMAGE_MEAN,
                IMAGE_STD,
                INPUT_NAME,
                OUTPUT_NAME
        );

        addHint("tensorflow load success");
        start();
    }


    private void start() {
        String path = Environment.getExternalStorageDirectory().getPath();
        final String outResultImg = path + File.separator + "out.png";
        final String outputTensorflowImg = path + File.separator + "tensorflow.png";
        if (autoClassifier != null) {
            autoClassifier.recognizeBitmap(sourcePath, outputTensorflowImg).subscribe(new Consumer<String>() {
                @Override
                public void accept(String result) throws Exception {
                    OpenCVUtils.handleImg(sourcePath, result, outResultImg)
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
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    Log.i(Keys.TAG, "出现异常:" + throwable.getMessage());
                }
            });
        }
    }


    private void addHint(String hint) {
        String content = hintBuilder.toString();
        hintBuilder.append(content);
        hintBuilder.append(flag);
        hintBuilder.append(hint);
    }
}
