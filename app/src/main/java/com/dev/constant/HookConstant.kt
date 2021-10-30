package com.dev.constant

class HookConstant {
    companion object {
        const val HOST_APP_PACKAGE_NAME = "com.dev"
        const val HOST_PLACE_HOLDER_ACTIVITY = "com.dev.PlaceHolderActivity"

        const val PLUGIN_APK_NAME = "plugin.apk"
        const val PLUGIN_PACKAGE_NAME = "com.dev.plugin"
        const val PLUGIN_ACTIVITY = "com.dev.plugin.MainActivity"

        const val KEY_IS_PLUGIN = "key_is_plugin"
        const val KEY_PACKAGE = "key_package"
        const val KEY_ACTIVITY = "key_activity"

        @Deprecated("Temporarily useless")
        const val START_ACTIVITY_METHOD_NAME = "startActivity"

        @Deprecated("Temporarily useless")
        const val KEY_RAW_INTENT = "raw_intent"

        @Deprecated("Temporarily useless")
        const val LAUNCH_ACTIVITY = 100

        @Deprecated("Temporarily useless")
        const val EXECUTE_TRANSACTION = 159
    }
}