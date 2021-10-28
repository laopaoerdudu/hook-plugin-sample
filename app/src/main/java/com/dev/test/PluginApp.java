package com.dev.test;

import android.content.res.Resources;

public class PluginApp {
    public Resources mResources;
    public ClassLoader mClassLoader;

    public PluginApp(Resources mResources) {
        this.mResources = mResources;
    }
}
