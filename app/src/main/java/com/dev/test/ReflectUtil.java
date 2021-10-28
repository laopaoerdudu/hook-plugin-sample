package com.dev.test;

import android.app.Activity;
import android.app.Instrumentation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtil {
    public static final String METHOD_currentActivityThread = "currentActivityThread";
    public static final String CLASS_ActivityThread = "android.app.ActivityThread";
    public static final String FIELD_mInstrumentation = "mInstrumentation";
    public static final String TAG = "ReflectUtil";

    private static Instrumentation sInstrumentation;
    private static Instrumentation sActivityInstrumentation;
    private static Field sActivityThreadInstrumentationField;
    private static Field sActivityInstrumentationField;
    private static Object sActivityThread;

    public static boolean init() {
        //获取当前的ActivityThread对象
        Class<?> activityThreadClass = null;
        try {
            activityThreadClass = Class.forName(CLASS_ActivityThread);
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod(METHOD_currentActivityThread);
            currentActivityThreadMethod.setAccessible(true);
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);


            //拿到在ActivityThread类里面的原始mInstrumentation对象
            Field instrumentationField = activityThreadClass.getDeclaredField(FIELD_mInstrumentation);
            instrumentationField.setAccessible(true);
            sActivityThreadInstrumentationField = instrumentationField;

            sInstrumentation = (Instrumentation) instrumentationField.get(currentActivityThread);
            sActivityThread = currentActivityThread;


            sActivityInstrumentationField =  Activity.class.getDeclaredField(FIELD_mInstrumentation);
            sActivityInstrumentationField.setAccessible(true);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | NoSuchFieldException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static Instrumentation getInstrumentation() {
        return sInstrumentation;
    }

    public static Object getActivityThread() {
        return sActivityThread;
    }

    public static void setInstrumentation(Object activityThread, HookedInstrumentation hookedInstrumentation) {
        try {
            sActivityThreadInstrumentationField.set(activityThread, hookedInstrumentation);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void setActivityInstrumentation(Activity activity, PluginManager manager) {
        try {
            sActivityInstrumentation = (Instrumentation) sActivityInstrumentationField.get(activity);
            HookedInstrumentation instrumentation = new HookedInstrumentation(sActivityInstrumentation, manager);
            sActivityInstrumentationField.set(activity, instrumentation);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void setField(Class clazz, Object target, String field, Object object) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
