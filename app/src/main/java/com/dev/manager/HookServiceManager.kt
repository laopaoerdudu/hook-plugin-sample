package com.dev.manager

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.ArrayMap
import com.dev.constant.HookConstant
import com.dev.constant.HookConstant.Companion.HOST_APP_PACKAGE_NAME
import com.dev.constant.HookConstant.Companion.HOST_PROXY_SERVICE
import com.dev.constant.HookConstant.Companion.KEY_RAW_INTENT
import com.dev.framework.IActivityManagerHandler
import com.dev.util.safeLeft
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

object HookServiceManager {
    // 存储插件的 Service 信息
    private val mServiceInfoMap = hashMapOf<ComponentName, ServiceInfo>()
    private val mServiceMap = hashMapOf<String, Service>()
    private lateinit var mContext: Context

    fun hookIActivityManager(context: Context) {
        mContext = context
        try {
            var IActivityManagerField: Field?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                IActivityManagerField = Class.forName("android.app.ActivityManager")
                    .getDeclaredField("IActivityManagerSingleton").apply {
                        isAccessible = true
                    }
            } else {
                IActivityManagerField =
                    Class.forName("android.app.ActivityManagerNative")
                        .getDeclaredField("gDefault")
                        .apply {
                            isAccessible = true
                        }
            }

            // 获取 Singleton<IActivityManager>
            val singleton = IActivityManagerField?.get(null)

            // 取出单例里面的 IActivityManager
            val mInstanceField =
                Class.forName("android.util.Singleton").getDeclaredField("mInstance").apply {
                    isAccessible = true
                }
            val rawIActivityManager = mInstanceField.get(singleton)

            // 创建代理对象，让代理对象帮忙干活
            val proxyIActivityManager = Proxy.newProxyInstance(
                Thread.currentThread().contextClassLoader,
                arrayOf(Class.forName("android.app.IActivityManager")),
                IActivityManagerHandler(context, rawIActivityManager)
            )

            mInstanceField.set(singleton, proxyIActivityManager)
        } catch (ex: ClassNotFoundException) {
            ex.printStackTrace()
        } catch (ex: NoSuchMethodException) {
            ex.printStackTrace()
        } catch (ex: InvocationTargetException) {
            ex.printStackTrace()
        } catch (ex: IllegalAccessException) {
            ex.printStackTrace()
        } catch (ex: NoSuchFieldException) {
            ex.printStackTrace()
        }
    }

    /**
     * 解析 Apk  文件中的 <service>, 并存储起来
     * 主要是调用 PackageParser 类的 generateServiceInfo =方法
     * @param apkFile 插件 apk
     */
    fun preLoadServices(apk: File) {
        try {
            val `PackageParser_Class` = Class.forName("android.content.pm.PackageParser")
            val rawPackageParser = `PackageParser_Class`.newInstance()
            val parsePackageMethod = `PackageParser_Class`.getDeclaredMethod(
                "parsePackage",
                File::class.java,
                Int::class.java
            )

            // 1，获取到 apk 对应的 Package 对象
            val rawPackage =
                parsePackageMethod.invoke(rawPackageParser, apk, PackageManager.GET_SERVICES)

            // 2，读取 Package 对象里面的 services 字段
            val servicesField = rawPackage.javaClass.getDeclaredField("services")
            val services = servicesField.get(rawPackage) as? List<Service>

            // 3，调用 generateServiceInfo 方法, 把 PackageParser.Service 转换成 ServiceInfo
            val `PackageParser$Service_Class` =
                Class.forName("android.content.pm.PackageParser\$Service")
            val `PackageUserState_Class` = Class.forName("android.content.pm.PackageUserState")
            val rawPackageUserState = `PackageUserState_Class`.newInstance()
            val `UserHandle_Class` = Class.forName("android.os.UserHandle")
            val userId = `UserHandle_Class`.getDeclaredMethod("getCallingUserId").invoke(null)

            val generateServiceInfoMethod = `PackageParser_Class`.getDeclaredMethod(
                "generateServiceInfo",
                `PackageParser$Service_Class`,
                Int::class.java,
                `PackageUserState_Class`,
                Int::class.java
            )

            // 解析出 intent 对应的 Service 组件
            services?.forEach { service ->
                val info = generateServiceInfoMethod.invoke(
                    rawPackageParser,
                    service,
                    0,
                    rawPackageUserState,
                    userId
                ) as? ServiceInfo

                safeLeft(info, info?.packageName, info?.name) { serviceInfo, pkg, name ->
                    mServiceInfoMap[ComponentName(pkg, name)] = serviceInfo
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun onStart(proxyIntent: Intent?, startId: Int) {
        val pluginIntent = proxyIntent?.getParcelableExtra<Intent>(KEY_RAW_INTENT)
        val serviceInfo = getPluginService(pluginIntent)
        serviceInfo ?: return
        try {
            if (!mServiceMap.containsKey(serviceInfo.name)) {
                proxyCreateService(serviceInfo)
            }
            val service = mServiceMap[serviceInfo.name]
            service?.onStart(pluginIntent, startId)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun stopService(context: Context?, args: Array<Any>?) {
        val pair = getIntentByArgs(args)
        if (args.isNullOrEmpty() || pair == null) {
            return
        }
        if (pair.second?.component?.packageName != context?.packageName) {
            // 插件的 intent 才做 hook
            val serviceInfo = getPluginService(pair.second)
            serviceInfo ?: return
            val service = mServiceMap[serviceInfo.name]
            service ?: return
            service.onDestroy()
            mServiceMap.remove(serviceInfo.name)
            if (mServiceMap.isEmpty()) {
                context?.stopService(Intent().apply {
                    component = ComponentName(context.packageName, HOST_PROXY_SERVICE)
                })
            }
        }
    }

    fun replacePluginIntentWithProxyIntent(args: Array<Any>?) {
        val pair = getIntentByArgs(args)
        if (args.isNullOrEmpty() || pair == null) {
            return
        }
        args[pair.first] = Intent().apply {
            component = ComponentName(
                HOST_APP_PACKAGE_NAME,
                HOST_PROXY_SERVICE
            )
            // 保存原始的插件 Service
            putExtra(HookConstant.KEY_RAW_INTENT, pair.second)
        }
    }

    private fun getPluginService(pluginIntent: Intent?): ServiceInfo? {
        mServiceInfoMap.keys.forEach { component ->
            if (component == pluginIntent?.component) {
                return mServiceInfoMap[component]
            }
        }
        return null
    }

    private fun getIntentByArgs(args: Array<Any>?): Pair<Int, Intent?>? {
        args ?: return null
        var rawIntent: Intent?
        var index = -1
        for (i in args.indices) {
            if (args[i] is Intent) {
                index = i
                break
            }
        }
        if (index != -1) {
            rawIntent = args[index] as? Intent
            return Pair(index, rawIntent)
        }
        return null
    }

    private fun proxyCreateService(serviceInfo: ServiceInfo) {
        try {
            val token = Binder()
            val `ActivityThread$CreateServiceData_Class` =
                Class.forName("android.app.ActivityThread\$CreateServiceData")
            val constructor =
                `ActivityThread$CreateServiceData_Class`.getDeclaredConstructor().apply {
                    isAccessible = true
                }
            val rawCreateServiceData = constructor.newInstance()

            val tokenField =
                `ActivityThread$CreateServiceData_Class`.getDeclaredField("token").apply {
                    isAccessible = true
                }
            tokenField.set(rawCreateServiceData, token)

            serviceInfo.applicationInfo.packageName = mContext.packageName
            val infoField =
                `ActivityThread$CreateServiceData_Class`.getDeclaredField("info").apply {
                    isAccessible = true
                }
            infoField.set(rawCreateServiceData, serviceInfo)

            val `CompatibilityInfo_Class` = Class.forName("android.content.res.CompatibilityInfo")
            val defaultCompatibilityField =
                `CompatibilityInfo_Class`.getDeclaredField("DEFAULT_COMPATIBILITY_INFO")
            val defaultCompatibility = defaultCompatibilityField.get(null)
            val compatInfoField =
                `ActivityThread$CreateServiceData_Class`.getDeclaredField("compatInfo").apply {
                    isAccessible = true
                }
            compatInfoField.set(rawCreateServiceData, defaultCompatibility)

            val `ActivityThread_Class` = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod =
                `ActivityThread_Class`.getDeclaredMethod("currentActivityThread")
            val currentActivityThread = currentActivityThreadMethod.invoke(null)

            val handleCreateServiceMethod = `ActivityThread_Class`.getDeclaredMethod(
                "handleCreateService",
                `ActivityThread$CreateServiceData_Class`
            ).apply {
                isAccessible = true
            }
            handleCreateServiceMethod.invoke(currentActivityThread, rawCreateServiceData)

            val mServicesField = `ActivityThread_Class`.getDeclaredField("mServices").apply {
                isAccessible = true
            }
            // final ArrayMap<IBinder, Service> mServices = new ArrayMap<>();
            val mServices = mServicesField.get(currentActivityThread) as? ArrayMap<IBinder, Service>
            val service = mServices?.get(token)
            mServices?.remove(token)
            service?.let {
                mServiceMap[serviceInfo.name] = it
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}