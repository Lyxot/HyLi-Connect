package xyz.hyli.connect.ui.test

import android.graphics.SurfaceTexture
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.activity.ComponentActivity
import xyz.hyli.connect.R
import xyz.hyli.connect.utils.VirtualDisplayUtils

class DisplayActivity : ComponentActivity() {
    private var displayID: Int = 0
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var textureView: TextureView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)
        val intent = intent
        displayID = intent.getIntExtra("displayID", 0)
        virtualDisplay = VirtualDisplayUtils.displayMap[displayID] ?: return
        textureView = findViewById(R.id.virtualDisplay)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                virtualDisplay?.surface = Surface(surface)
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//                virtualDisplay?.surface = null
                return true
            }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        VirtualDisplayUtils(this).destroyDisplay(displayID)
    }
}
