package com.example.viewpagerdemo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by 00265372 on 2017/1/17.
 */

public class rwUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    public static final String TAG = "rwUncaught";

    private static rwUncaughtExceptionHandler INSTANCE = new rwUncaughtExceptionHandler();


    private Context mContext;// 程序的Context对象

    private Map<String, String> info = new HashMap<String, String>();// 用来存储设备信息和异常信息
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");// 用于格式化日期,作为日志文件名的一部分

    /** 保证只有一个CrashHandler实例 */
    private rwUncaughtExceptionHandler() {

    }

    /** 获取rwUncaughtExceptionHandler实例 ,单例模式 */
    public static rwUncaughtExceptionHandler getInstance() {
        return INSTANCE;
    }

    public void init(Context context){
        mContext = context;

        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        //设置该rwUncaughtExceptionHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }
            Intent intent = new Intent(mContext.getApplicationContext(), ViewPagerDemo.class);
            PendingIntent restartIntent = PendingIntent.getActivity(mContext.getApplicationContext(), 0,
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    0);
            //退出程序
            AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000,
                    restartIntent); // 1秒钟后重启应用

            exceptionHandlerApplication.finishActivity();
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        //使用Toast来显示异常信息
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出并重启。",
                        Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }.start();

        // 收集设备参数信息
        collectDeviceInfo(mContext);
        // 保存日志文件
        saveCrashInfo2File(ex);

        return true;
    }

    /**
     * 收集设备参数信息
     *
     * @param context
     */
    public void collectDeviceInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();// 获得包管理器
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_ACTIVITIES);// 得到该应用的信息，即主Activity
            if (pi != null) {
                String versionName = pi.versionName == null ? "null"
                        : pi.versionName;
                String versionCode = pi.versionCode + "";
                info.put("versionName", versionName);
                info.put("versionCode", versionCode);

                /**
                 * 获取手机型号，系统版本，以及SDK版本
                 */
                info.put("手机型号:", android.os.Build.MODEL);
                info.put("系统版本", ""+android.os.Build.VERSION.SDK);
                info.put("Android版本", android.os.Build.VERSION.RELEASE);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                info.put(field.getName(), field.get("").toString());
                Log.d(TAG, field.getName() + ":" + field.get(""));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private String saveCrashInfo2File(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : info.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\r\n");
        }
        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        ex.printStackTrace(pw);
        Throwable cause = ex.getCause();
        // 循环着把所有的异常信息写入writer中
        while (cause != null) {
            cause.printStackTrace(pw);
            cause = cause.getCause();
        }
        pw.close();// 记得关闭
        String result = writer.toString();
        sb.append(result);
        // 保存文件
        long timetamp = System.currentTimeMillis();
        String time = format.format(new Date());

        String fileName = "crash-" + time + "-" + timetamp + ".log";
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            try {
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "crash");
                Log.i("CrashHandler", dir.toString());
                if (!dir.exists())
                    dir.mkdir();
                FileOutputStream fos = new FileOutputStream(new File(dir,
                        fileName));
                fos.write(sb.toString().getBytes());
                fos.close();
                return fileName;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

//    /**
//     * 收集设备参数信息
//     *
//     * @param ctx
//     */
//    public void collectDeviceInfo2(Context ctx) {
//        try {
//            PackageManager pm = ctx.getPackageManager();
//            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
//                    PackageManager.GET_ACTIVITIES);
//            if (pi != null) {
//                String versionName = pi.versionName + "";
//                String versionCode = pi.versionCode + "";
//                infos.put("versionName", versionName);
//                infos.put("versionCode", versionCode);
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//            Log.e(TAG, "an error occured when collect package info", e);
//        }
//        Field[] fields = Build.class.getDeclaredFields();
//        for (Field field : fields) {
//            try {
//                field.setAccessible(true);
//                infos.put(field.getName(), field.get(null).toString());
//            } catch (Exception e) {
//                Log.e(TAG, "an error occured when collect crash info", e);
//            }
//        }
//    }
//
//    /**
//     * 保存错误信息到文件中
//     *
//     * @param ex
//     * @return 返回文件名称, 便于将文件传送到服务器
//     * @throws Exception
//     */
//    private String saveCrashInfoFile(Throwable ex) throws Exception {
//        StringBuffer sb = new StringBuffer();
//        try {
//            SimpleDateFormat sDateFormat = new SimpleDateFormat(
//                    "yyyy-MM-dd HH:mm:ss");
//            String date = sDateFormat.format(new java.util.Date());
//            sb.append("\r\n" + date + "\n");
//            for (Map.Entry<String, String> entry : infos.entrySet()) {
//                String key = entry.getKey();
//                String value = entry.getValue();
//                sb.append(key + "=" + value + "\n");
//            }
//
//            Writer writer = new StringWriter();
//            PrintWriter printWriter = new PrintWriter(writer);
//            ex.printStackTrace(printWriter);
//            Throwable cause = ex.getCause();
//            while (cause != null) {
//                cause.printStackTrace(printWriter);
//                cause = cause.getCause();
//            }
//            printWriter.flush();
//            printWriter.close();
//            String result = writer.toString();
//            sb.append(result);
//
//            String fileName = writeFile(sb.toString());
//            return fileName;
//        } catch (Exception e) {
//            Log.e(TAG, "an error occured while writing file...", e);
//            sb.append("an error occured while writing file...\r\n");
//            writeFile(sb.toString());
//        }
//        return null;
//    }
//
//    private String writeFile(String sb) throws Exception {
//        String time = formatter.format(new Date());
//        String fileName = "crash-" + time + ".log";
//        if (FileUtil.hasSdcard()) {
//            String path = getGlobalpath();
//            File dir = new File(path);
//            if (!dir.exists())
//                dir.mkdirs();
//
//            FileOutputStream fos = new FileOutputStream(path + fileName, true);
//            fos.write(sb.getBytes());
//            fos.flush();
//            fos.close();
//        }
//        return fileName;
//    }
//
//    public static String getGlobalpath() {
//        return Environment.getExternalStorageDirectory().getAbsolutePath()
//                + File.separator + "crash" + File.separator;
//    }
//
//    public static void setTag(String tag) {
//        TAG = tag;
//    }
//
//    /**
//     * 文件删除
//     * @param day 文件保存天数
//     */
//    public void autoClear(final int autoClearDay) {
//        FileUtil.delete(getGlobalpath(), new FilenameFilter() {
//
//            @Override
//            public boolean accept(File file, String filename) {
//                String s = FileUtil.getFileNameWithoutExtension(filename);
//                int day = autoClearDay < 0 ? autoClearDay : -1 * autoClearDay;
//                String date = "crash-" + DateUtil.getOtherDay(day);
//                return date.compareTo(s) >= 0;
//            }
//        });
//
//    }
}
