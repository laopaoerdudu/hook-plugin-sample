package com.dev.framework;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static com.dev.constant.HookConstant.HOST_APP_PACKAGE_NAME;
import static com.dev.constant.HookConstant.PLACE_HOLDER_ACTIVITY_NAME;
import static com.dev.constant.HookConstant.PLUGIN_INTENT;

public class IActivityManagerHandler implements InvocationHandler {
    Object rawIActivityManager;

    public IActivityManagerHandler(Object rawIActivityManager) {
        this.rawIActivityManager = rawIActivityManager;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        if ("startActivity".equals(method.getName())) {
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }
            Intent rawIntent = (Intent) args[index];
            Intent targetIntent = new Intent();
            // 这里我们把启动的插件 Activity 临时替换为 PlaceHolderActivity，骗过 AMS 的检查
            ComponentName componentName = new ComponentName(HOST_APP_PACKAGE_NAME, PLACE_HOLDER_ACTIVITY_NAME);
            targetIntent.setComponent(componentName);
            targetIntent.putExtra(PLUGIN_INTENT, rawIntent);

            // 替换掉 Intent, 达到欺骗 AMS 的目的
            args[index] = targetIntent;

            Log.d("WWE", "startActivity -> Replace plugin activity with PlaceHolderActivity is success >>>");
            return method.invoke(rawIActivityManager, args);
        }
        return method.invoke(rawIActivityManager, args);
    }
}
