package com.dev.helper

import java.lang.reflect.Field
import java.lang.reflect.Method

class ReflectHelper {
    companion object {
        fun getField(instance: Any, fieldName: String): Field? {
            var clazz: Class<*>? = instance::class.java
            while (clazz != null) {
                try {
                    return (clazz.getDeclaredField(fieldName))?.apply {
                        isAccessible = true
                    }
                } catch (ex: NoSuchFieldException) {
                    ex.printStackTrace()
                }
                clazz = clazz.superclass
            }
            return null
        }

        fun getMethod(instance: Any, methodName: String, vararg paramTypes: Class<*>): Method? {
            var typeClass: Class<*>? = instance::class.java
            while (typeClass != null) {
                try {
                    return typeClass.getDeclaredMethod(methodName, *paramTypes)?.apply {
                        isAccessible = true
                    }
                } catch (ex: NoSuchFieldException) {
                    ex.printStackTrace()
                }
                typeClass = typeClass.superclass
            }
            return null
        }
    }
}