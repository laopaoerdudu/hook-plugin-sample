package com.dev.manager

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.Log
import com.dev.helper.FileHelper
import com.dev.helper.PluginHelper
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method

object HookBroadCastManager {
    private val sCache = hashMapOf<ActivityInfo?, List<out IntentFilter>?>()

    fun loadBroadcast(context: Context) {
        parserReceivers(context)
        try {
            sCache.keys?.forEach {
                it?.let { activityInfo ->
                    Log.i("WWE", "receiver: ${activityInfo.name}")
                    val receiver =
                        PluginHelper.getPluginClassLoader(context).loadClass(activityInfo.name)
                            .newInstance() as? BroadcastReceiver
                    sCache[activityInfo]?.forEach { intentFilter ->
                        context.registerReceiver(receiver, intentFilter)
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * 解析Apk文件中的 <receiver>, 并存储起来
     * @param apk
     */
    private fun parserReceivers(context: Context) {
        try {
            // 1，获取到 apk 对应的 Package 对象
            val `PackageParserClass` = Class.forName("android.content.pm.PackageParser")
            val parsePackageMethod: Method = `PackageParserClass`.getDeclaredMethod(
                "parsePackage",
                File::class.java,
                Int::class.java
            )
            val rawPackageParser = `PackageParserClass`.newInstance()
            val rawPackage = parsePackageMethod.invoke(
                rawPackageParser,
                FileHelper.getFile(context),
                PackageManager.GET_RECEIVERS
            )

            // 2， 读取 Package 里面的 receivers 字段,注意这是一个 List<Activity> (没错,底层把 <receiver> 当作 <activity> 处理)
            val receiversField: Field = rawPackage.javaClass.getDeclaredField("receivers")
            val receivers = receiversField.get(rawPackage) as? List<Activity>

            // 3，android.content.pm.PackageParser #generateActivityInfo(...)
            val `packageParser$Activity_Class` =
                Class.forName("android.content.pm.PackageParser\$Activity")
            val `packageUserStateClass` = Class.forName("android.content.pm.PackageUserState")
            val rawPackageUserState = `packageUserStateClass`.newInstance()
            val `userHandlerClass` = Class.forName("android.os.UserHandle")
            val getCallingUserIdMethod: Method =
                `userHandlerClass`.getDeclaredMethod("getCallingUserId")
            val userId = getCallingUserIdMethod.invoke(null) as? Int
            val `packageParser$Component_Class` =
                Class.forName("android.content.pm.PackageParser\$Component")
            val intentsField: Field = `packageParser$Component_Class`.getDeclaredField("intents")
            val generateActivityInfoMethod: Method = `PackageParserClass`.getDeclaredMethod(
                "generateActivityInfo",
                `packageParser$Activity_Class`,
                Int::class.java,
                `packageUserStateClass`,
                Int::class.java
            )

            // 4，解析 receiver 对应的 intentFilter
            receivers?.forEach { receiver ->
                val activityInfo = generateActivityInfoMethod.invoke(
                    rawPackageParser,
                    receiver,
                    0,
                    rawPackageUserState,
                    userId
                ) as? ActivityInfo
                val filters = intentsField.get(receiver) as? List<out IntentFilter>
                sCache[activityInfo] = filters
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}