package io.github.karino2.photomdmemo

import android.graphics.Matrix
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PhotoViewActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private var scaleFactor = 1.0f
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private val matrix = Matrix()
    
    // For panning
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_photo_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageView = findViewById(R.id.photoView)
        imageView.scaleType = ImageView.ScaleType.MATRIX
        
        val uri = intent.data ?: return
        imageView.setImageURI(uri)

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val lastScale = scaleFactor
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.1f, 10.0f)
                
                val adjustedScale = scaleFactor / lastScale
                matrix.postScale(adjustedScale, adjustedScale, detector.focusX, detector.focusY)
                imageView.imageMatrix = matrix
                return true
            }
        })

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                matrix.postTranslate(-distanceX, -distanceY)
                imageView.imageMatrix = matrix
                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }
}
