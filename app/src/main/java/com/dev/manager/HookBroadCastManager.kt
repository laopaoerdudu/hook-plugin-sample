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

object HookBroadCastManager {
    private val sCache = hashMapOf<ActivityInfo?, List<out IntentFilter>?>()

    fun loadBroadcast(context: Context) {
        parserReceivers(context)
        try {
            sCache.keys?.forEach {
                it?.let { activityInfo ->
                    Log.i("WWE", "receiver: ${activityInfo.name}")
                    val pluginClassLoader = PluginHelper.getPluginClassLoader(context)
                    sCache[activityInfo]?.forEach { intentFilter ->
                        val receiver = pluginClassLoader.loadClass(activityInfo.name)
                            .newInstance() as? BroadcastReceiver
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
            val packageParserClass = Class.forName("android.content.pm.PackageParser")
            val packageParser = packageParserClass.newInstance()
            val parsePackageMethod = packageParserClass.getDeclaredMethod(
                "parsePackage",
                File::class.java,
                Int::class.java
            ).apply {
                isAccessible = true
            }
            // 1，获取到 apk 对应的 Package 对象
            val rawPackage =
                parsePackageMethod.invoke(packageParser, FileHelper.getDexPath(context), PackageManager.GET_RECEIVERS)

            // 2， 读取 Package 里面的 receivers 字段,注意这是一个 List<Activity> (没错,底层把 <receiver> 当作 <activity> 处理)
            val receiversField = rawPackage.javaClass.getDeclaredField("receivers").apply {
                isAccessible = true
            }
            val receivers = receiversField.get(rawPackage) as? List<Activity>

            // 获取 userId
            val userHandler = Class.forName("android.os.UserHandle")
            val getCallingUserIdMethod = userHandler.getDeclaredMethod("getCallingUserId").apply {
                isAccessible = true
            }
            val userId = getCallingUserIdMethod.invoke(null)

            // 3，解析 receiver 对应的 intentFilter
            val packageParserActivityClass =
                Class.forName("android.content.pm.PackageParser\$Activity")

            val packageUserStateClass = Class.forName("android.content.pm.PackageUserState")
            val rawPackageUserState = packageUserStateClass.newInstance()

            val packageParserComponentClass =
                Class.forName("android.content.pm.PackageParser\$Component")
            val intentsField = packageParserComponentClass.getDeclaredField("intents").apply {
                isAccessible = true
            }
            val generateActivityInfoMethod = packageParserClass.getDeclaredMethod(
                "generateActivityInfo",
                packageParserActivityClass,
                Int::class.java,
                packageUserStateClass,
                Int::class.java
            ).apply {
                isAccessible = true
            }
            receivers?.forEach { receiver ->
                val activityInfo = generateActivityInfoMethod.invoke(
                    packageParser,
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