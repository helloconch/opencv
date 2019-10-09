package com.ikangtai.shecare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.MessageQueue;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.ikangtai.shecare.tensorflow.Classifier;
import com.ikangtai.shecare.tensorflow.TensorFlowImageClassifier;
import com.ikangtai.shecare.utils.FileUtil;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int OPEN_SETTING_REQUEST_COED = 110;
    private static final int TAKE_PHOTO_REQUEST_CODE = 120;
    private static final int PICTURE_REQUEST_CODE = 911;

    private static final int PERMISSIONS_REQUEST = 108;
    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 119;

    private static final String CURRENT_TAKE_PHOTO_URI = "currentTakePhotoUri";

    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";

    private static final String MODEL_FILE = "file:///android_asset/model/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/model/imagenet_comp_graph_label_strings.txt";
    private Executor executor;
    private Uri currentTakePhotoUri;

    private TextView result;
    private TextView blurLevel1;
    private TextView blurLevel2;
    private TextView blurLevel3;
    private ImageView ivPicture;
    private Classifier classifier;
    private File photoFile;
    private File[] lhfiles;
    private int lhIndex;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTaskRoot()) {
            finish();
        }
        setContentView(R.layout.activity_main);
        findViewById(R.id.iv_choose_picture).setOnClickListener(this);
        findViewById(R.id.iv_take_photo).setOnClickListener(this);
        findViewById(R.id.opencv).setOnClickListener(this);
        findViewById(R.id.smartpaper).setOnClickListener(this);
        findViewById(R.id.tensorflow).setOnClickListener(this);
        findViewById(R.id.tensorflowOpencv).setOnClickListener(this);
        findViewById(R.id.nextPic).setOnClickListener(this);
        ivPicture = findViewById(R.id.iv_picture);
        result = findViewById(R.id.tv_classifier_info);
        blurLevel1 = findViewById(R.id.blurLevel1);
        blurLevel2 = findViewById(R.id.blurLevel2);
        blurLevel3 = findViewById(R.id.blurLevel3);
        XXPermissions.with(MainActivity.this)
                .permission(Permission.Group.STORAGE, Permission.Group.CAMERA)
                .request(new OnPermission() {
                    @Override
                    public void hasPermission(List<String> granted, boolean isAll) {
                        if (isAll) {


                            int a = com.ikangtai.YunchengLhCut2.test(1000, 1000);

                            StringBuilder builder = new StringBuilder();
                            builder.append(String.valueOf(a));

                            result.setText(builder.toString());


                            // 避免耗时任务占用 CPU 时间片造成UI绘制卡顿，提升启动页面加载速度
                            Looper.myQueue().addIdleHandler(idleHandler);

                        }
                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {
                        finish();
                    }
                });
        readAssetsLhPaper();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // 防止拍照后无法返回当前 activity 时数据丢失
        savedInstanceState.putParcelable(CURRENT_TAKE_PHOTO_URI, currentTakePhotoUri);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            currentTakePhotoUri = savedInstanceState.getParcelable(CURRENT_TAKE_PHOTO_URI);
        }
    }

    /**
     * 主线程消息队列空闲时（视图第一帧绘制完成时）处理耗时事件
     */
    MessageQueue.IdleHandler idleHandler = new MessageQueue.IdleHandler() {
        @Override
        public boolean queueIdle() {

            if (classifier == null) {
                // 创建 Classifier
                classifier = TensorFlowImageClassifier.create(MainActivity.this.getAssets(),
                        MODEL_FILE, LABEL_FILE, INPUT_SIZE, IMAGE_MEAN, IMAGE_STD, INPUT_NAME, OUTPUT_NAME);
            }

            // 初始化线程池
            executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setName("ThreadPool-ImageClassifier");
                    return thread;
                }
            });

            return false;
        }
    };


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_choose_picture:
                choosePicture();
                break;
            case R.id.iv_take_photo:
                takePhoto();
                break;
            case R.id.opencv:
                startActivity(new Intent(MainActivity.this,
                        OpenCVActivity.class));
                break;

            case R.id.smartpaper:
                startActivity(new Intent(MainActivity.this,
                        SmartPaperActivity.class));
                break;


            case R.id.tensorflow:
                startActivity(new Intent(MainActivity.this,
                        TensorflowActivity.class));
                break;

            case R.id.tensorflowOpencv:
                startActivity(new Intent(MainActivity.this,
                        AutoTestPaper.class));
                break;

            case R.id.nextPic:
                if (lhfiles != null && lhfiles.length > 0) {
                    if (lhIndex >= lhfiles.length) {
                        lhIndex = 0;
                    }
                    String path = lhfiles[lhIndex].getPath();
                    Uri uri = Uri.fromFile(lhfiles[lhIndex]);
                    handleInputPhoto(uri);
                    //01
                    blurLevel1.setText("base algorithm:\n" +
                            String.valueOf(roundTo2DecimalPlaces(blurLevel1(path).doubleValue())));
                    //02
                    blurLevel2.setText("very similar to blurLevel1:\n"
                            + String.valueOf(roundTo2DecimalPlaces(blurLevel2(path).doubleValue())));
                    //03
                    blurLevel3.setText("standard deviation based algorithm:\n"
                            + String.valueOf(roundTo2DecimalPlaces(blurLevel3(path).doubleValue())));

                    lhIndex++;
                }


                break;


            default:
                break;
        }
    }

    /**
     * 选择一张图片并裁剪获得一个小图
     */
    private void choosePicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICTURE_REQUEST_CODE);
    }

    /**
     * 使用系统相机拍照
     */
    private void takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSIONS_REQUEST_CODE);
        } else {
            openSystemCamera();
        }
    }

    /**
     * 打开系统相机
     */
    private void openSystemCamera() {
        //调用系统相机
        Intent takePhotoIntent = new Intent();
        takePhotoIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        //这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
        if (takePhotoIntent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "当前系统没有可用的相机应用", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "TF_" + System.currentTimeMillis() + ".jpg";
        photoFile = new File(FileUtil.getPhotoCacheFolder(), fileName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //通过FileProvider创建一个content类型的Uri
            currentTakePhotoUri = FileProvider.getUriForFile(this,
                    "gdut.bsx.tensorflowtraining.fileprovider", photoFile);
            //对目标应用临时授权该 Uri 所代表的文件
            takePhotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            currentTakePhotoUri = Uri.fromFile(photoFile);
        }

        //将拍照结果保存至 outputFile 的Uri中，不保留在相册中
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentTakePhotoUri);
        startActivityForResult(takePhotoIntent, TAKE_PHOTO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICTURE_REQUEST_CODE) {
                // 处理选择的图片
                handleInputPhoto(data.getData());
            } else if (requestCode == TAKE_PHOTO_REQUEST_CODE) {
                // 如果拍照成功，加载图片并识别
                handleInputPhoto(currentTakePhotoUri);

                if (photoFile != null) {
                    String path = photoFile.getPath();
                    //01
                    blurLevel1.setText("base algorithm:\n" +
                            String.valueOf(roundTo2DecimalPlaces(blurLevel1(path).doubleValue())));
                    //02
                    blurLevel2.setText("very similar to blurLevel1:\n"
                            + String.valueOf(roundTo2DecimalPlaces(blurLevel2(path).doubleValue())));
                    //03
                    blurLevel3.setText("standard deviation based algorithm:\n"
                            + String.valueOf(roundTo2DecimalPlaces(blurLevel3(path).doubleValue())));
                }


            }
        }
    }

    /**
     * 处理图片
     *
     * @param imageUri
     */
    private void handleInputPhoto(Uri imageUri) {
        // 加载图片
        GlideApp.with(MainActivity.this).asBitmap().listener(new RequestListener<Bitmap>() {

            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                Log.d(TAG, "handleInputPhoto onLoadFailed");
                Toast.makeText(MainActivity.this, "图片加载失败", Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                Log.d(TAG, "handleInputPhoto onResourceReady");
                startImageClassifier(resource);
                return false;
            }
        }).load(imageUri).into(ivPicture);

        result.setText("Processing...");
    }

    /**
     * 开始图片识别匹配
     *
     * @param bitmap
     */
    private void startImageClassifier(final Bitmap bitmap) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, Thread.currentThread().getName() + " startImageClassifier");
                    Bitmap croppedBitmap = getScaleBitmap(bitmap, INPUT_SIZE);

                    final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
                    Log.i(TAG, "startImageClassifier results: " + results);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            result.setText(String.format("results: %s", results));
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "startImageClassifier getScaleBitmap " + e.getMessage());
                    e.printStackTrace();
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

    /**
     * base algorithm
     *
     * @param path
     * @return
     */
    private Double blurLevel1(String path) {
        Mat image = Imgcodecs.imread(path, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        if (image.empty()) {
            Log.e(TAG, "CANNOT OPEN IMAGE!");
            return 0.0;
        } else {
            Log.d(TAG, "Image: $image");
            Mat destination = new Mat();
            Mat matGray = new Mat();

            Imgproc.cvtColor(image, matGray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.Laplacian(matGray, destination, 3);
            MatOfDouble median = new MatOfDouble();
            MatOfDouble std = new MatOfDouble();
            Core.meanStdDev(destination, median, std);
            return Math.pow(std.get(0, 0)[0], 2.0);
        }
    }

    /**
     * very similar to blurLevel1
     *
     * @param path
     * @return
     */
    private Double blurLevel2(String path) {
        Mat image = Imgcodecs.imread(path, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        if (image.empty()) {
            Log.e(TAG, "CANNOT OPEN IMAGE!");
            return 0.0;
        } else {
            Mat destination = new Mat();
            Mat matGray = new Mat();
            Mat kernel = new Mat(3, 3, CvType.CV_32F) {
                {
                    put(0, 0, 0.0);
                    put(0, 1, -1.0);
                    put(0, 2, 0.0);

                    put(1, 0, -1.0);
                    put(1, 1, 4.0);
                    put(1, 2, -1.0);

                    put(2, 0, 0.0);
                    put(2, 1, -1.0);
                    put(2, 2, 0.0);
                }
            };

            Imgproc.cvtColor(image, matGray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.filter2D(matGray, destination, -1, kernel);
            MatOfDouble median = new MatOfDouble();
            MatOfDouble std = new MatOfDouble();
            Core.meanStdDev(destination, median, std);
            return Math.pow(std.get(0, 0)[0], 2.0);
        }
    }

    /**
     * standard deviation based algorithm
     *
     * @return
     */
    private Double blurLevel3(String path) {
        Mat image = Imgcodecs.imread(path, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
        // mean
        MatOfDouble mu = new MatOfDouble();
        // standard deviation
        MatOfDouble sigma = new MatOfDouble();
        Core.meanStdDev(image, mu, sigma);
        return Math.pow(mu.get(0, 0)[0], 2.0);
    }

    public final double roundTo2DecimalPlaces(double $receiver) {

        return (new BigDecimal($receiver)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }


    private void readAssetsLhPaper() {
        try {
            String lhPaperFolder = Environment.getExternalStorageDirectory().getPath() + "/lhpaper";
            File lhPaperFile = new File(lhPaperFolder);
            if (lhPaperFile.exists()) {
                lhfiles = lhPaperFile.listFiles();
            }

        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }

    }

}
