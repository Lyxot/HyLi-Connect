package xyz.hyli.connect.ui.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import androidx.activity.ComponentActivity
import xyz.hyli.connect.R
import xyz.hyli.connect.utils.VirtualDisplayUtils
import java.io.ByteArrayOutputStream

class DisplayActivity : ComponentActivity() {
    private var displayID: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)
        val intent = intent
        displayID = intent.getIntExtra("displayID", 0)
        val imageReader = VirtualDisplayUtils.imageMap[displayID]
        imageReader?.setOnImageAvailableListener({
            val image = imageReader.acquireLatestImage()
            val planes = image.planes
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
            val imageBytes = out.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            Log.i("DisplayActivity", "onCreate: $bitmap")

            image.close()
        }, null)
    }
    override fun onDestroy() {
        super.onDestroy()
        VirtualDisplayUtils(this).destroyDisplay(displayID)
    }
}