
该 Sample 主要演示利用 Hook 技术实现 Activity 插件化。

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

















































 






