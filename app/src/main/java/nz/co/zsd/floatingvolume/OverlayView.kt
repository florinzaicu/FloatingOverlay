package nz.co.zsd.floatingvolume

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.content.getSystemService
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.size
import com.google.android.material.color.DynamicColors
import kotlin.math.log


/**
 * View that defines the components of the floating overlay and its controls
 */
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
        x = 0
        y = 0
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

        // Initiate and bind a gesture listener to the overlay
        val floatingOverlayGestListener = object : GestureDetector.SimpleOnGestureListener() {
            /**
             * Override the on down event and return true to consume it (make sure listener is
             * used for other gestures)
             */
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            /**
             * On single tap confirmed, trigger the volume up command
             */
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                volumeUp()
                return true
            }

            /**
             * On double tap confirmed, trigger the volume down command
             */
            override fun onDoubleTap (e: MotionEvent): Boolean {
                volumeDown()
                return true
            }

            /**
             * On long press, expand the overlay
             */
            override fun onLongPress(e: MotionEvent) {
                expandControls()
            }

            /**
             * On fling gesture move the overlay to an edge of the screen.
             * @param e1 The first down motion event that initiated the fling
             * @param e2 The second motion event captured on release of the fling
             * @param velocityX The velocity of this fling measured in px/s along the x axis
             * @param velocityY The velocity of this fling measured in px/s along the y axis
             */
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // If the overlay is expanded, do not perform fling detection
                if (!collapsed)
                    return true;

                var flingDirection: Int = if (velocityX < 0) FLING_LEFT else FLING_RIGHT
                flingOverlay(flingDirection)
                return true
            }
        }

        val gestureDetector = GestureDetector(context, floatingOverlayGestListener)
        overlay.findViewById<View>(R.id.gestureIcon).setOnTouchListener {_, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        }

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

                            // Snap the overlay to the edge of the screen if its x and y position
                            // are over the bounds of the usable area of teh screen
                            val bounds: Rect = getMaxSize()
                            if (x > bounds.right) {
                                x = bounds.right
                            }
                            if (y > bounds.bottom) {
                                y = bounds.bottom
                            }
                            layoutParams.x = x
                            layoutParams.y = y
                            windowManager.updateViewLayout(overlay, layoutParams)

                            // Clear the collapse timer when dragging
                            clearCollapseTimer()
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            // When dragging released initiate the collapse timer if not collapsed
                            if (!collapsed) {
                                initCollapseTimer()
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

        // Bind click actions to the overlay buttons
        overlay.findViewById<ImageView>(R.id.volumeUp).setOnClickListener {
            Log.d(javaClass.simpleName, "Volume up button pressed")
            volumeUp()
        }
        overlay.findViewById<ImageView>(R.id.volumeDown).setOnClickListener {
            Log.d(javaClass.simpleName, "Volume down button pressed")
            volumeDown()
        }
        overlay.findViewById<ImageView>(R.id.exitOverlay).setOnClickListener {
            Log.d(javaClass.simpleName, "Exit button pressed")
            exitOverlay()
        }

        // Collapse the UI (only show expand)
        collapseControls();
    }

    /**
     * Fling the overlay to a specified location on screen (snap to max bounds of usable area)
     * @param direction Direction to fling the screen (see FLING constants)
     */
    private fun flingOverlay(direction: Int) {
        // Get the size of the screen and decide how to fling it on the x axis
        val bounds: Rect = getMaxSize()
        var x = 0;
        if (direction and FLING_LEFT == FLING_LEFT) {
            x = 0
        } else if (direction and FLING_RIGHT == FLING_RIGHT) {
            x = bounds.right
        }

        // Animate flinging the overlay to the new x position
        ValueAnimator.ofInt(layoutParams.x, x).apply {
            duration = 300
            start()
        }.addUpdateListener {animation ->
            layoutParams.x = animation.animatedValue as Int
            windowManager.updateViewLayout(overlay, layoutParams)
        }
    }

    /**
     * Increase the volume of the device (current most relevant stream)
     */
    private fun volumeUp () {
        val manager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVol = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.i(javaClass.simpleName, "Current volume is $currentVol. Increase called")
        manager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Decrease the volume of the device (current most relevant stream)
     */
    private fun volumeDown () {
        val manager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVol = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.i(javaClass.simpleName, "Current volume is $currentVol. Decrease called")
        manager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Exit the overlay by stopping the overlay service
     */
    private fun exitOverlay() {
        Log.i(javaClass.simpleName, "Exit button pressed. Stopping overlay.")
        context.stopService(Intent(context, OverlayService::class.java))
    }

    /**
     * Destroy the view by removing it from the window manager
     */
    fun destroy() {
        windowManager.removeView(overlay)
    }

    /* ----- Collapse overlay methods ----- */

    /**
     * Collapse the overlay controls by hiding all icons (expect the drag icon), clear the collapse
     * timer, and set the collapsed flag to true
     */
    private fun collapseControls() {
        collapsed = true
        overlay.findViewById<ImageView>(R.id.dragIcon).visibility = GONE
        overlay.findViewById<ImageView>(R.id.volumeDown).visibility = GONE
        overlay.findViewById<ImageView>(R.id.volumeUp).visibility = GONE
        overlay.findViewById<ImageView>(R.id.exitOverlay).visibility = GONE

        clearCollapseTimer()
    }

    /**
     * Expand the overlay controls by showing all hidden icons, triggering the collapse timer,
     * and setting the collapsed status to false
     */
    private fun expandControls() {
        collapsed = false
        overlay.findViewById<ImageView>(R.id.dragIcon).visibility = VISIBLE
        overlay.findViewById<ImageView>(R.id.volumeDown).visibility = VISIBLE
        overlay.findViewById<ImageView>(R.id.volumeUp).visibility = VISIBLE
        overlay.findViewById<ImageView>(R.id.exitOverlay).visibility = VISIBLE

        initCollapseTimer()
    }

    /**
     * Initiate the collapse timer to collapse the overlay after 3 seconds
     */
    private fun initCollapseTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(3000, 3000) {
            override fun onTick(millisUntilFinished: Long) {
                // Do nothing (no tick)
            }

            override fun onFinish() {
                timer = null
                collapseControls()
            }
        }

        timer?.start();
    }

    /**
     * Cancel and stop the collapse timer
     */
    private fun clearCollapseTimer() {
        timer?.cancel();
        timer = null;
    }

    /**
     * Get the maximum size of the usable area of the screen (canvas)
     * @return rect object with the max bounds of the screen where left and bottom represent
     *  width and height and left and top are always 0
     */
    private fun getMaxSize(): Rect {
        // Get the size of the screen (canvas) to move the overlay
        var rect = Rect(0, 0, 0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            rect.right = windowManager.maximumWindowMetrics.bounds.width()
            rect.bottom = windowManager.maximumWindowMetrics.bounds.height()
        } else {
            // NOTE: this was only deprecated in API 30. Anything lower can use this call
            @Suppress("DEPRECATION")
            rect.right = windowManager.defaultDisplay.width
            @Suppress("DEPRECATION")
            rect.bottom = windowManager.defaultDisplay.height
        }

        // Adjust the screen size with the view size to get the max bounds to move the overlay
        val overlayHeight = overlay.findViewById<View>(R.id.activity_docked_overlay).measuredHeightAndState
        val overlayWidth = overlay.findViewById<View>(R.id.activity_docked_overlay).measuredWidthAndState
        rect.right -= overlayWidth
        rect.bottom -= overlayHeight
        return rect
    }


    companion object {
        /**
         * Tag to use when logging information to logcat
         */
        private val LOG_TAG: String = OverlayView::class.simpleName ?: "OverlayView"

        /**
         * Constants the specify the fling location of the overlay.
         * OR to get a combination of fling directions. Binary representation of positions:
         *  0) 0000 = MIDDLE CENTER
         *  1) 0001 = MIDDLE LEFT
         *  2) 0010 = MIDDLE RIGHT
         *  3) 0011 = MIDDLE CENTER
         *  4) UNDEFINED
         *  5) 0101 = TOP LEFT
         *  6) 0110 = TOP RIGHT
         *  7) 0111 = TOP CENTER
         *  8) UNDEFINED
         *  9) 0001 = BOTTOM LEFT
         * 10) 1010 = BOTTOM RIGHT
         * 11) 1011 = BOTTOM CENTER
         */
        public const val FLING_LEFT = 1;
        public const val FLING_RIGHT = 2;
        public const val FLING_TOP = 4;
        public const val FLING_BOTTOM = 8;
    }
}