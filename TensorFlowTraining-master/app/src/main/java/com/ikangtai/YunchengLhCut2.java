package com.ikangtai;

import android.util.Log;

import com.ikangtai.shecare.utils.Keys;

/**
 * @author wangwei
 * @version V1.0
 * @description
 * @date Created in 12:04 2019/8/29
 * @modify
 */
public class YunchengLhCut2 {
    private static final String libraryC = "lhcut";

    static {
        Log.i(Keys.TAG, "YunchengLhCut2");
        System.loadLibrary(libraryC);
    }

    public static native int[] getlh(int[] rawpic, int wsource, int hsource, int[] graypic, int wgray, int hgray, String result);

    public static native int test(int a, int b);

}