package com.dev.framework

import android.content.Intent
import android.os.Handler
import android.os.Message
import com.dev.constant.HookConstant.Companion.PLUGIN_INTENT

class ActivityThreadHandlerCallback(private val rawHandler: Handler?) : Handler.Callback {
    override fun handleMessage(message: Message): Boolean {
        when (message.what) {
            100 -> {

                // case LAUNCH_ACTIVITY: {
                //        //          Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
                //        //          final ActivityClientRecord r = (ActivityClientRecord) msg.obj;
                //
                //        //          r.packageInfo = getPackageInfoNoCheck(
                //        //                  r.activityInfo.applicationInfo, r.compatInfo);
                //        //         handleLaunchActivity(r, null);

                // 替换 Intent
                // MainActivity -> PlaceHolderActivity => PluginActivity
                // LAUNCH_ACTIVITY
                try {
                    val intentField =
                        message.obj.javaClass.getDeclaredField("intent")?.apply {
                            isAccessible = true
                        }
                    val rawIntent = intentField?.get(message.obj) as? Intent
                    (rawIntent?.getParcelableExtra(PLUGIN_INTENT) as? Intent)?.let { targetIntent ->
                        intentField.set(message.obj, targetIntent)
                    }
                } catch (ex: NoSuchFieldException) {
                    ex.printStackTrace()
                } catch (ex: IllegalAccessException) {
                    ex.printStackTrace()
                }
            }
            159 -> {
                // public static final int EXECUTE_TRANSACTION = 159;
                try {
                    val mActivityCallbacksField =
                        Class.forName("android.app.servertransaction.ClientTransaction")
                            ?.getDeclaredField("mActivityCallbacks")?.apply {
                                isAccessible = true
                            }
                    val mActivityCallbacks =
                        mActivityCallbacksField?.get(message.obj) as? List<*>
                    mActivityCallbacks ?: return false
                    for (i in mActivityCallbacks.indices) {
                        if ("android.app.servertransaction.LaunchActivityItem" == mActivityCallbacks[i]?.javaClass?.name) {
                            val launchActivityItem = mActivityCallbacks[i]
                            val mIntentField =
                                launchActivityItem?.javaClass?.getDeclaredField("mIntent")
                                    ?.apply {
                                        isAccessible = true
                                    }
                            val rawIntent =
                                mIntentField?.get(launchActivityItem) as? Intent
                            (rawIntent?.getParcelableExtra(PLUGIN_INTENT) as? Intent)?.let { targetIntent ->
                                mIntentField.set(launchActivityItem, targetIntent)
                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        rawHandler?.handleMessage(message)
        return true
    }
}