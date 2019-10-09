package com.ikangtai.shecare.tensorflow;

import android.graphics.Bitmap;

import io.reactivex.Observable;

public interface AutoClassifier {
    void enableStatLogging(final boolean debug);

    String getStatString();

    void close();

    /**
     * 通过Tensorflow将参数bitmap转为256*256图片 并将图片路径输出
     *
     * @param bitmap 原始正方形图片
     * @return
     */
    Observable<String> recognizeBitmap(Bitmap bitmap);

    /**
     * 通过Tensorflow将参数imgFile转为256*256图片 并将图片路径输出
     *
     * @param imgPath 原始正方形图片
     * @return
     */
    Observable<String> recognizeBitmap(String imgPath);


    /**
     * 通过Tensorflow将参数imgFile转为256*256图片 并将图片路径输出
     * @param imgPath imgFile 原始正方形图片
     * @param outPath 训练完毕后输出目录
     * @return
     */
    Observable<String> recognizeBitmap(String imgPath, String outPath);

}
