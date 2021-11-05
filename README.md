
该 Sample 主要演示利用 Hook 技术实现插件化。

#### Activity 的插件化

Activity 拥有任务栈的概念。任务栈的概念使得 Activity 的创建就代表着入栈，销毁则代表出栈；
又由于 Activity 代表着与用户交互的界面，所以这个栈的深度不可能太深——Activity 栈太深意味着用户需要狂点 back 键才能回到初始界面，这种体验显然有问题；
因此，插件框架要处理的 Activity 数量其实是有限的，所以我们在 AndroidManifest.xml 中声明有限个 StubActivity 就能满足插件启动近乎无限个插件 Activity 的需求。



#### 静态广播插件化

对静态 BroadcastReceiver 无能为力吗？想一想这里的难点是什么？

没错，主要是静态 BroadcastReceiver 里面的 IntentFilter 我们事先无法确定，它是动态变化的；
但是，动态 BroadcastReceiver 可以动态添加 IntentFilter 吗！！

静态 BroadcastReceiver 与动态 BroadcastReceiver 一个非常大的不同之处在于：
动态 BroadcastReceiver 在进程死亡之后是无法接收广播的，而静态 BroadcastReceiver 则可以，系统会唤醒 Receiver 所在进程；

要把插件中的静态 BroadcastReceiver 当作动态 BroadcastReceiver 处理，我们首先得知道插件中到底注册了哪些广播；
这个过程归根结底就是获取 AndroidManifest.xml  中的 <receiver> 标签下面的内容，
我们可以选择手动解析 xml 文件；这里我们选择使用系统的 PackageParser 帮助解析

#### Service 的插件化

Service 组件则代表后台任务，除了内存不足系统回收之外，它的生命周期完全由我们的代码控制，与用户的交互无关。

Service 组件与 Activity 有着非常多的相似之处：它们都是通过 Context 类完成启动，
接着通过 ActivityManagerNative 进入 AMS，最后又通过 IApplicationThread 这个 Binder IPC到 App 进程的 Binder 线程池，
然后通过 H 转发消息到 App 进程的主线程，最终完成组件生命周期的回调；

AMS 转发了一个消息给 ActivityThread 的 H 这个 Handler，H 类收到这个消息之后，
直接调用了 ActivityThread 类的 handleCreateService 方法，创建出了 Service 对象，并且调用了它的 onCreate方法；



```
# ActivityThread
public final void scheduleCreateService(IBinder token,
        ServiceInfo info, CompatibilityInfo compatInfo, int processState) {
    updateProcessState(processState, false);
    CreateServiceData s = new CreateServiceData();
    s.token = token;
    s.info = info;
    s.compatInfo = compatInfo;

    sendMessage(H.CREATE_SERVICE, s);
}

# ActivityThread
private void handleCreateService(CreateServiceData data) {
    unscheduleGcIdler();

    LoadedApk packageInfo = getPackageInfoNoCheck(
            data.info.applicationInfo, data.compatInfo);
    Service service = null;
    try {
        java.lang.ClassLoader cl = packageInfo.getClassLoader();
        service = (Service) cl.loadClass(data.info.name).newInstance();
    } catch (Exception e) {
    }

    try {
        ContextImpl context = ContextImpl.createAppContext(this, packageInfo);
        context.setOuterContext(service);

        Application app = packageInfo.makeApplication(false, mInstrumentation);
        service.attach(context, this, data.info.name, data.token, app,
                ActivityManagerNative.getDefault());
        service.onCreate();
        mServices.put(data.token, service);
        try {
            ActivityManagerNative.getDefault().serviceDoneExecuting(
                    data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
        } catch (RemoteException e) {
            // nothing to do.
        }
    } catch (Exception e) {
    }
}

```



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















































 






