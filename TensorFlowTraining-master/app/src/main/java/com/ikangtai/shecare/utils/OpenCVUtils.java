package com.ikangtai.shecare.utils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.ikangtai.YunchengLhCut2;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;


public class OpenCVUtils {
    public static Observable<Bitmap> handleImg(final String originImagePath,
                                               final String imageGrayPath,
                                               final String outputPath) {
        return Observable.create(new ObservableOnSubscribe<Bitmap>() {
            @Override
            public void subscribe(ObservableEmitter<Bitmap> emitter) throws Exception {

                File imageFile = new File(originImagePath);
                File imageGayFile = new File(imageGrayPath);
                if (imageFile.exists() && imageGayFile.exists()) {
                    Bitmap imageBitmap = BitmapFactory.decodeFile(imageFile.getPath());
                    Bitmap imageGayBitmap = BitmapFactory.decodeFile(imageGayFile.getPath());
                    Mat image = new Mat();
                    Utils.bitmapToMat(imageBitmap, image);
                    Mat imageGay = new Mat();
                    Utils.bitmapToMat(imageGayBitmap, imageGay);
                    int h = image.height();
                    int w = image.width();
                    int hg = imageGay.height();
                    int wg = imageGay.width();
                    int[] source = new int[h * w * 4];
                    int[] source3 = new int[h * w * 3];
                    image.convertTo(image, CvType.CV_32SC3);
                    image.get(0, 0, source);

                    int[] sourceGray = new int[hg * wg * 4];
                    int[] sourceGray3 = new int[hg * wg * 3];
                    imageGay.convertTo(imageGay, CvType.CV_32SC3);
                    imageGay.get(0, 0, sourceGray);
                    String result1 = "";
                    chanel4ToChanel3(source, source3);
                    chanel4ToChanel3(sourceGray, sourceGray3);
                    int[] result = YunchengLhCut2.getlh(source3, w, h, sourceGray3, wg, hg, result1);
                    byte[] resultLast = new byte[result.length - 11];
                    intToByte(result, resultLast, 11);
                    Mat imageL = new Mat(result[10], result[9], CvType.CV_8UC3);
                    imageL.put(0, 0, resultLast);
                    Imgcodecs.imwrite(outputPath, imageL);
                    Bitmap bitmap = BitmapFactory.decodeFile(outputPath);
                    emitter.onNext(bitmap);
                }
            }
        });
    }

    public static void byteToInt(int[] result, byte[] source) {
        int length = source.length;
        for (int i = 0; i < length; i++) {
            result[i] = source[i] & 0xff;
        }
    }

    private static void chanel4ToChanel3(int[] chan4, int[] chan3) {
        for (int i = 0; i < chan4.length; i = i + 4) {
            chan3[i / 4 * 3] = chan4[i + 2];
            chan3[i / 4 * 3 + 1] = chan4[i + 1];
            chan3[i / 4 * 3 + 2] = chan4[i];
        }
    }

    public static void intToByte(int[] source, byte[] result, int start) {
        int length = source.length;
        for (int i = start; i < length; i++) {
            result[i - start] = (byte) source[i];
        }
    }


    /**
     * 对图片进行缩放
     *
     * @param bitmap
     * @param size
     * @return
     * @throws IOException
     */
    public static Bitmap getScaleBitmap(Bitmap bitmap, int size) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) size) / width;
        float scaleHeight = ((float) size) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }


    public static void saveBitmap(Bitmap bmp, String strFileName) {
        try {
            File file = new File(strFileName);
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {

            System.out.print(e.getMessage());

        }

    }

}
