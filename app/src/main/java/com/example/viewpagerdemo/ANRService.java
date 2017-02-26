package com.example.viewpagerdemo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by 00265372 on 2017/1/18.
 */

public class ANRService extends Service {

    private int workThreadTick = 0;
    private int mainThreadTick = 0;

    private boolean flag = true;

    private Handler mHandler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("ANRService","onCreate");
        exception();
    }

    private void exception(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(flag){
                    workThreadTick = mainThreadTick;


                    mHandler.post(tickerRunnable);//向主线程发送消息 计数器值+1
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(workThreadTick == mainThreadTick){
                        flag = false;
                        Log.e("gac","anr happned in here");

                        Thread mainThread = Looper.getMainLooper().getThread();
                        StackTraceElement[] stackElements = mainThread.getStackTrace();

                        String stackString = null;

                        if (stackElements != null) {
                            for (int i = 0; i < stackElements.length; i++) {
                                stackString = stackString + stackElements[i].getClassName() + "."
                                        + stackElements[i].getMethodName() + " ("
                                        + stackElements[i].getFileName() + ":"
                                        + stackElements[i].getLineNumber() + ")" + "\n";
                            }

                            Log.e("gac", stackString);
                        }

//                        // 获取当前线程的堆栈
//                        for (StackTraceElement i : Thread.currentThread().getStackTrace()) {
//                            Log.e("gac", i.toString());
//                        }

//                        try {
//                            handleAnrError();
//                        }catch (ANRException e) {
//                            e.printStackTrace();

//                            StackTraceElement[] stackElements = e.getStackTrace();
//                            if (stackElements != null) {
//                                for (int i = 0; i < stackElements.length; i++) {
//                                    String statckString = stackElements[i].getClassName() + "."
//                                            + stackElements[i].getMethodName() + " ("
//                                            + stackElements[i].getFileName() + ":"
//                                            + stackElements[i].getLineNumber() + ")";
//                                    System.out.println(statckString);
//                                }
//                            }

//                            RuntimeException re = new RuntimeException();
//                            re.fillInStackTrace();
//                            Log.e("gac", "info", re);


//                        }

                    }
                }
            }
        }).start();
    }

    //发生anr的时候，在此处写逻辑
    private void handleAnrError(){

//        throw new ANRException("ANRException");

    }
    private final Runnable tickerRunnable = new Runnable() {
        @Override public void run() {
            mainThreadTick = (mainThreadTick + 1) % 10;


        }
    };
}
