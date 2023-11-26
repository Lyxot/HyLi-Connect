package xyz.hyli.connect.utils

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.util.Log
import android.view.WindowManager


class VirtualDisplayUtils(
    private val context: Context
) {
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayManager: DisplayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var displayMap: MutableMap<Int, VirtualDisplay> = mutableMapOf()
    companion object {
        var imageMap: MutableMap<Int, ImageReader> = mutableMapOf()
    }


    fun createDisplay(packageName:String, width:Int, height:Int, dpi:Int): Int? {
        val virtualDisplay: VirtualDisplay
        val imageReader: ImageReader
        val windowsLayoutParams = WindowManager.LayoutParams()
        try {
            imageReader = ImageReader.newInstance(
                width,
                height,
                ImageFormat.YUV_420_888,
                2
            )
            virtualDisplay = displayManager.createVirtualDisplay(
                "HyLiConnect@$packageName",
                width,
                height,
                dpi,
                imageReader.surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            )
        } catch (e: Exception) {
            Log.e("VirtualDisplayUtils", "createDisplay: ", e)
            return null
        }

        val displayID = virtualDisplay.display.displayId
        displayMap.put(virtualDisplay.display.displayId, virtualDisplay)
        imageMap.put(virtualDisplay.display.displayId, imageReader)
        return displayID
    }
    fun resizeDisplay(displayID:Int, width:Int, height:Int, dpi:Int): VirtualDisplay? {
        val virtualDisplay = displayMap[displayID] ?: return null
        try {
            virtualDisplay.resize(width, height, dpi)
        } catch (e: Exception) {
            return null
        }
        return virtualDisplay
    }
    fun destroyDisplay(displayID: Int) {
        val virtualDisplay = displayMap[displayID] ?: return
        virtualDisplay.surface.release()
        virtualDisplay.release()
    }
    fun destroyAllDisplay() {
        displayMap.forEach {
            it.value.surface.release()
            it.value.release()
        }
    }
    fun getImageReader(displayID: Int): ImageReader? {
        return imageMap[displayID]
    }
}