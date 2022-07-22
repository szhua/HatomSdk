package com.fencer.hatomsdk;

import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Environment.DIRECTORY_PICTURES;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import java.io.File;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Locale;

public class MyUtils {
    private static final String TAG = "MyUtils";


    /**
     * 抓图路径格式：/storage/emulated/0/Android/data/com.hikvision.open.app/files/Pictures/_20180917151634445.jpg
     */
    public static String getCaptureImagePath(Context context) {
        File file = context.getExternalFilesDir(DIRECTORY_PICTURES);
        String path = file.getAbsolutePath() + File.separator + MyUtils.getFileName("") + ".jpg";
        Log.i(TAG, "getCaptureImagePath: " + path);
        return path;
    }

    /**
     * 抓图路径格式：/storage/emulated/0/Android/data/com.hikvision.open.app/files/Thumbnail/_20180917151634445.jpg
     */
    public static String getCaptureImageThumbnailPath(Context context) {
        File file = context.getExternalFilesDir(DIRECTORY_PICTURES);
        String path = file.getAbsolutePath() + File.separator + "thumb" + MyUtils.getFileName("") + ".jpg";
        Log.i(TAG, "getCaptureImagePath: " + path);
        return path;
    }


    /**
     * 录像路径格式：/storage/emulated/0/Android/data/com.hikvision.open.app/files/Movies/_20180917151636872.mp4
     */
    public static String getLocalRecordPath(Context context) {
        File file = context.getExternalFilesDir(DIRECTORY_MOVIES);
        String path = file.getAbsolutePath() + File.separator + MyUtils.getFileName("") + ".mp4";
        Log.i(TAG, "getLocalRecordPath: " + path);
        return path;
    }

    /**
     * 获取文件名称（监控点名称_年月日时分秒毫秒）
     *
     * @return 文件名称
     */
    public static String getFileName(String name) {
        Calendar calendar = Calendar.getInstance();
        return name + "_" +
                String.format(Locale.CHINA, "%04d%02d%02d%02d%02d%02d%03d",
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH),
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        calendar.get(Calendar.SECOND),
                        calendar.get(Calendar.MILLISECOND));
    }


    @NonNull
    public static Activity getActivity(View view) {
        for (Context context = view.getContext(); context instanceof ContextWrapper; context = ((ContextWrapper) context).getBaseContext()) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
        }

        throw new IllegalStateException("View " + view + " is not attached to an Activity");
    }

    /**
     * 将int错误码转换为固定格式的十六进制错误码 0x00000000
     *
     * @param errorCode 十进制错误码
     * @return 十六进制错误码
     */
    public static String convertToHexString(String errorCode) {
        if (errorCode.startsWith("0x")) {
            return errorCode;
        }
        if (TextUtils.isEmpty(errorCode)) {
            return "";
        }
        int parseInt = Integer.parseInt(errorCode);
        StringBuilder hexCode = new StringBuilder(Integer.toHexString(parseInt));
        if (hexCode.length() < 8) {
            int count = 8 - hexCode.length();
            for (int i = 0; i < count; i++) {
                hexCode.insert(0, "0");
            }
        }
        return MessageFormat.format("{0}{1}", "0x", hexCode.toString());
    }
}
