package com.dev.helper

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception

class FileHelper {
    companion object {
        @JvmStatic
        fun copyAssetsFileToCache(context: Context, fileName: String): String? {
            val cacheDir = context.filesDir.apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            Log.i("WWE", "context.filesDir -> ${cacheDir.absolutePath}")
            val outputPath = File(cacheDir, fileName).apply {
                if (exists()) {
                    delete()
                }
            }
            var ins: InputStream? = null
            var fos: FileOutputStream? = null
            try {
                if (outputPath.createNewFile()) {
                    ins = context.assets.open(fileName)
                    fos = FileOutputStream(outputPath)
                    val buf = ByteArray(ins.available())
                    var byteCount = 0
                    while (ins.read(buf).also { byteCount = it } != -1) {
                        fos.write(buf, 0, byteCount)
                    }
                    println("outputPath -> ${outputPath.absolutePath} write successfully !")
                    Log.i("WWE", "Copy assets file to cache is succeed !")
                    return outputPath.absolutePath
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                try {
                    fos?.apply {
                        flush()
                        close()
                    }
                    ins?.apply {
                        close()
                    }
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
            return null
        }
    }
}