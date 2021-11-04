
该 Sample 主要演示利用 Hook 技术实现 Activity 插件化。

**资源参考**：

[VirtualAPK](https://github.com/didi/VirtualAPK)

[深入理解Android插件化技术](https://zhuanlan.zhihu.com/p/33017826)

[Android 插件化之加载插件资源](https://github.com/13767004362/HookDemo/blob/master/document/Android%E6%8F%92%E4%BB%B6%E5%8C%96%E4%B9%8B%E5%8A%A0%E8%BD%BDResource%E8%B5%84%E6%BA%90.md)

[Android 插件化之ClassLoader加载so库](https://github.com/13767004362/HookDemo/blob/master/document/Android%E6%8F%92%E4%BB%B6%E5%8C%96%E4%B9%8Bso%E5%8A%A0%E8%BD%BD.md)

[Android 插件化之aapt修改资源前缀](https://github.com/13767004362/HookDemo/blob/master/aapt/Android%E6%8F%92%E4%BB%B6%E5%8C%96%E4%B9%8Baapt%E4%BF%AE%E6%94%B9%E8%B5%84%E6%BA%90%E5%89%8D%E7%BC%80.md)

[Android插件化之动态替换Application](https://github.com/13767004362/HookDemo/blob/master/document/%E6%8F%92%E4%BB%B6%E5%8C%96%E4%B9%8B%E5%8A%A8%E6%80%81%E6%9B%BF%E6%8D%A2application.md)

[Android 插件化之Fragment重建问题](https://github.com/13767004362/HookDemo/blob/master/document/Android%E6%8F%92%E4%BB%B6%E5%8C%96%E4%B9%8BFragment.md)

[Android插件化原理和实践 (五) 之 解决合并资源后资源Id冲突的问题](https://blog.csdn.net/hwliu51/article/details/76945286)

[另一种绕过 Android P以上非公开API限制的办法](http://weishu.me/2019/03/16/another-free-reflection-above-android-p/)

[FreeReflection ](https://github.com/tiann/FreeReflection)
```
// package android.util;
public abstract class Singleton<T> {
    private T mInstance;

    protected abstract T create();

    public final T get() {
        synchronized (this) {
            if (mInstance == null) {
                mInstance = create();
            }
            return mInstance;
        }
    }
}
```
```
private static final Singleton<IActivityManager> IActivityManagerSingleton =
    new Singleton<IActivityManager>() {
        @Override
        protected IActivityManager create() {
            final IBinder b = ServiceManager.getService(Context.ACTIVITY_SERVICE);
            final IActivityManager am = IActivityManager.Stub.asInterface(b);
            return am;
    }
};
```

```
public static IActivityManager getService() {
    return IActivityManagerSingleton.get();
}
```

```
int result = ActivityManager.getService()
    .startActivity(whoThread, who.getBasePackageName(), intent,
        intent.resolveTypeIfNeeded(who.getContentResolver()),
        token, target != null ? target.mEmbeddedID : null,
        requestCode, 0, null, options);
```

```
public final ArrayList<Activity> activities = new ArrayList<Activity>(0);
6243        public final ArrayList<Activity> receivers = new ArrayList<Activity>(0);
6244        public final ArrayList<Provider> providers = new ArrayList<Provider>(0);
6245        public final ArrayList<Service> services = new ArrayList<Service>(0);
```















































 






