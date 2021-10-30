package com.dev.framework;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.dev.manager.PluginManager;

import java.lang.reflect.Method;

import androidx.annotation.NonNull;

import static com.dev.constant.HookConstant.HOST_APP_PACKAGE_NAME;
import static com.dev.constant.HookConstant.HOST_PLACE_HOLDER_ACTIVITY;
import static com.dev.constant.HookConstant.KEY_ACTIVITY;
import static com.dev.constant.HookConstant.KEY_IS_PLUGIN;
import static com.dev.constant.HookConstant.KEY_PACKAGE;

public class HookedInstrumentation extends Instrumentation implements Handler.Callback {
    private Instrumentation rawInstrumentation;

    public HookedInstrumentation(Instrumentation base) {
        this.rawInstrumentation = base;
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        super.callActivityOnCreate(activity, icicle);
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        return false;
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (isPluginIntent(intent)) {
            String targetClassName = intent.getComponent().getClassName();
            Activity activity = rawInstrumentation.newActivity(PluginManager.INSTANCE.getClassLoader(), targetClassName, intent);
            activity.setIntent(intent);
            AMSHookManager.INSTANCE.hookResource(activity, PluginManager.INSTANCE.getResources());
            return activity;
        }
        return super.newActivity(cl, className, intent);
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        String targetPackageName = intent.getComponent().getPackageName();
        if (HOST_APP_PACKAGE_NAME != targetPackageName) {
            intent.setClassName(HOST_APP_PACKAGE_NAME, HOST_PLACE_HOLDER_ACTIVITY);
            intent.putExtra(KEY_IS_PLUGIN, true);
            intent.putExtra(KEY_PACKAGE, targetPackageName);
            intent.putExtra(KEY_ACTIVITY, intent.getComponent().getClassName());
        }
        try {
            Method execStartActivity = Instrumentation.class.getDeclaredMethod(
                    "execStartActivity", Context.class, IBinder.class, IBinder.class,
                    Activity.class, Intent.class, int.class, Bundle.class);
            execStartActivity.setAccessible(true);
            return (ActivityResult) execStartActivity.invoke(rawInstrumentation, who,
                    contextThread, token, target, intent, requestCode, options);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("do not support!!!" + e.getMessage());
        }
    }

    private Boolean isPluginIntent(Intent intent) {
        if (intent.getBooleanExtra(KEY_IS_PLUGIN, false)) {
            intent.setClassName(intent.getStringExtra(KEY_PACKAGE), intent.getStringExtra(KEY_ACTIVITY));
            return true;
        }
        return false;
    }
}
