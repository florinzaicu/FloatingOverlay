package nz.co.zsd.floatingvolume

import android.content.Context
import android.media.AudioManager
import android.os.CountDownTimer
import android.provider.MediaStore.Audio
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.getSystemService


class OverlayView(context: Context) : View(context) {

    // Window manager that displays the overlay
    private val windowManager = context.getSystemService<WindowManager>()!!
    // Instance of the overlay currently displayed on the window manager
    private val overlay : FrameLayout = FrameLayout(context)

    // Layout parameters applied to the current overlay. Overlay should wrap content, be displayed
    // as an overlay and show in the top left corner
    private val layoutParams : WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        android.graphics.PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.LEFT or Gravity.TOP
    }

    // Flag that indicates if the overlay is collapsed (true) or not (false)
    private var collapsed = false
    // Countdown timer instance that handles auto-collapse functionality
    private var timer: CountDownTimer? = null;

    /**
     * Initiate the overlay and display it on the screen
     */
    init {
        // Inflate the layout of the overlay and add it to the overlay frame to display
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.overlay_layout, overlay)
        windowManager.addView(overlay, layoutParams)


        // Bind drag action to the overlay drag handle (allow user to move overlay)
        overlay.findViewById<View>(R.id.dragIcon)
            .setOnTouchListener(object : OnTouchListener {
                // NOTE: cannot use kotlin SAM conversion as we need to declare member attrs
                // Current x and y position of the overlay and touch location
                private var x : Int = 0;
                private var y : Int = 0;
                private var touchX : Int = 0;
                private var touchY : Int = 0;

                override fun onTouch(view: View, motionEvent: MotionEvent) : Boolean {
                    val eventX : Int = motionEvent.rawX.toInt()
                    val eventY : Int = motionEvent.rawY.toInt()

                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            x = layoutParams.x
                            y = layoutParams.y
                            touchX = eventX
                            touchY = eventY

                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (collapsed == false ) {
                                // TODO: init timer
                            }
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            layoutParams.x = x + (eventX - touchX)
                            layoutParams.y = y + (eventY - touchY)
                            windowManager.updateViewLayout(overlay, layoutParams)
                            return true
                        }

                        else -> {
                            return false
                        }
                    }
                }
            })

        overlay.findViewById<ImageView>(R.id.stopServiceBtn).setOnClickListener {
            Log.e(javaClass.simpleName, "Volume up button pressed")
            VolumeUp()
        }
    }

    /**
     * Increase the volume of the device (current most relevant stream)
     */
    fun VolumeUp () {
        val manager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVol = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.i(javaClass.simpleName, "Current volume is $currentVol. Increase called")
        manager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }

    fun VolumeDown () {
        val manager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVol = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.i(javaClass.simpleName, "Current volume is $currentVol. Decrease called")
        manager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Remove the overlay from the screen and free any resources
     */
    public fun destroy() {
        windowManager.removeView(overlay)
    }
}