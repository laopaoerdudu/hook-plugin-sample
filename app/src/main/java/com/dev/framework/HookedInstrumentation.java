package com.dev.framework;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.dev.manager.HookManager;

import java.lang.reflect.Method;

import androidx.annotation.NonNull;

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
        if (HookManager.INSTANCE.isPluginIntentSetup(intent)) {
            String targetClassName = intent.getComponent().getClassName();
            Activity activity = rawInstrumentation.newActivity(HookManager.INSTANCE.getClassLoader(), targetClassName, intent);
            activity.setIntent(intent);
            HookManager.INSTANCE.hookResource(activity, HookManager.INSTANCE.getResources());
            return activity;
        }
        return super.newActivity(cl, className, intent);
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        HookManager.INSTANCE.setPlaceHolderIntent(intent);
        try {
            Method execStartActivityMethod = Instrumentation.class.getDeclaredMethod(
                    "execStartActivity", Context.class, IBinder.class, IBinder.class,
                    Activity.class, Intent.class, int.class, Bundle.class);
            execStartActivityMethod.setAccessible(true);
            return (ActivityResult) execStartActivityMethod.invoke(rawInstrumentation, who,
                    contextThread, token, target, intent, requestCode, options);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("do not support!!!" + e.getMessage());
        }
    }
}
