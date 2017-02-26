package com.example.viewpagerdemo;

import android.app.Activity;
import android.app.Application;

import java.util.ArrayList;

/**
 * Created by 00265372 on 2017/1/17.
 */

public class exceptionHandlerApplication extends Application {
    private static ArrayList<Activity> list = new ArrayList<Activity>();

    @Override
    public void onCreate() {
        super.onCreate();
        rwUncaughtExceptionHandler.getInstance().init(this);
    }

    /**
     * Activity关闭时，删除Activity列表中的Activity对象*/
    public void removeActivity(Activity a){
        list.remove(a);
    }

    /**
     * 向Activity列表中添加Activity对象*/
    public static void addActivity(Activity a){
        list.add(a);
    }

    /**
     * 关闭Activity列表中的所有Activity*/
    public static void finishActivity(){
        for (Activity activity : list) {
            if (null != activity) {
                activity.finish();
            }
        }
        //杀死该应用进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }

}
