package com.example.minipaint

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat

// Define a constant for the stroke width
private const val STROKE_WIDTH = 12f // has to be float

class MyCanvasView(context: Context) : View(context) {

    // Canvas and Bitmap for caching what has been drawn before
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    // Background color of the Canvas
    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)

    /*
     * Define a variable drawColor for holding the color to draw with and initialize it with the
     * colorPaint resource you defined earlier.
     */
    private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)

    // Set up the paint with which to draw.
    private val paint = Paint().apply {
        // Color of the paint
        color = drawColor
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = STROKE_WIDTH // default: Hairline-width (really thin)
    }

    /*
    In MyCanvasView, add a variable path and initialize it with a Path object to store the path
    that is being drawn when following the user's touch on the screen. Import android.graphics.Path
    for the Path.
     */
    private var path = Path()

    /*
    add the missing motionTouchEventX and motionTouchEventY variables for caching the x and y
    coordinates of the current touch event (the MotionEvent coordinates). Initialize them to 0f.
     */
    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    /*
    At the class level, add variables to cache the latest x and y values. After the user stops
    moving and lifts their touch, these are the starting point for the next path (the next segment
    of the line to draw).
     */
    private var currentX = 0f
    private var currentY = 0f

    // Add a touchTolerance variable and set it to ViewConfiguration.get(context).scaledTouchSlop.
    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    private lateinit var frame: Rect

    /*
     * This callback method is called by the Android system with the changed screen dimensions,
     * that is, with a new width and height (to change to) and the old width and height (to change
     * from).
     */
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        /*
         * Looking at onSizeChanged(), a new bitmap and canvas are created every time the function
         * executes. You need a new bitmap, because the size has changed. However, this is a memory
         * leak, leaving the old bitmaps around. To fix this, recycle extraBitmap before creating
         * the next one.
         */
        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        /*
         * Inside onSizeChanged(), create an instance of Bitmap with the new width and height,
         * which are the screen size, and assign it to extraBitmap. The third argument is the
         * bitmap color configuration. ARGB_8888 stores each color in 4 bytes and is recommended.
         */
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Create a Canvas instance from extraBitmap and assign it to extraCanvas.
        extraCanvas = Canvas(extraBitmap)
        // Specify the background color in which to fill extraCanvas.
        extraCanvas.drawColor(backgroundColor)

        // Calculate a rectangular frame around the picture.
        val inset = 40
        frame = Rect(inset, inset, width - inset, height - inset)
    }

    /*
     * Override onDraw() and draw the contents of the cached extraBitmap on the canvas associated
     * with the view. The drawBitmap() Canvas method comes in several versions. In this code, you
     * provide the bitmap, the x and y coordinates (in pixels) of the top left corner, and null for
     * the Paint, as you'll set that later.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
        // Draw a frame around the canvas.
        canvas.drawRect(frame, paint)
    }
    /*
     * Note: The 2D coordinate system used for drawing on a Canvas is in pixels, and the
     * origin (0,0) is at the top left corner of the Canvas.
     */

    // Create stubs for the three functions touchStart(), touchMove(), and touchUp().

    /*
    Reset the path, move to the x-y coordinates of the touch event (motionTouchEventX and
    motionTouchEventY) and assign currentX and currentY to that value.
     */
    private fun touchStart() {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    /*
    Calculate the traveled distance (dx, dy), create a curve between the two points and store it in
    path, update the running currentX and currentY tally, and draw the path. Then call invalidate()
    to force redrawing of the screen with the updated path.
     */
    private fun touchMove() {
        // 1. Calculate the distance that has been moved
        val dx = Math.abs(motionTouchEventX - currentX)
        val dy = Math.abs(motionTouchEventY - currentY)
        // 2. If movement was further than the touch tolerance
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point (x1,y1), and ending at (x2,y2).
                // 3. Add a segment to the path
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            // 4. Set the starting point
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // Draw the path in the extra bitmap to cache it.
            extraCanvas.drawPath(path, paint)
        }
        // 5. Call invalidate
        // force redrawing of the screen with the updated path
        invalidate()
    }

    private fun touchUp() {
        // Reset the path so it doesn't get drawn again.
        path.reset()
    }

    /*
    In MyCanvasView, override the onTouchEvent() method to cache the x and y coordinates of the
    passed in event. Then use a when expression to handle motion events for touching down on the
    screen, moving on the screen, and releasing touch on the screen. These are the events of
    interest for drawing a line on the screen. For each event type, call a utility method, as
    shown in the code below. See the MotionEvent class documentation for a full list of touch events.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }

}

/**
* In the current app, the cumulative drawing information is cached in a bitmap. While this is a good
solution, it is not the only possible way. How you store your drawing history depends on the app,
and your various requirements. For example, if you are drawing shapes, you could save a list of
shapes with their location and dimensions. For the MiniPaint app, you could save the path as a
Path. Below is the general outline on how to do that, if you want to try it.

* 1. Remove all the code for extraCanvas and extraBitmap.
* 2. Add variables for the path so far, and the path being drawn currently.
    // Path representing the drawing so far
    private val drawing = Path()

    // Path representing what's currently being drawn
    private val curPath = Path()

* 3. In onDraw(), instead of drawing the bitmap, draw the stored and current paths.
    // Draw the drawing so far
    canvas.drawPath(drawing, paint)
    // Draw any current squiggle
    canvas.drawPath(curPath, paint)
    // Draw a frame around the canvas
    canvas.drawRect(frame, paint)
* 4. In touchUp() , add the current path to the previous path and reset the current path.
    // Add the current path to the drawing so far
    drawing.addPath(curPath)
    // Rewind the current path for the next touch
    curPath.reset()
* 5. Run your app, and yes, there should be no difference whatsoever.
 */
