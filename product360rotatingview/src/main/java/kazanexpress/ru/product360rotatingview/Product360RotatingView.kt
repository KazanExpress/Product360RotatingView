package kazanexpress.ru.product360rotatingview

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LevelListDrawable
import android.os.Handler
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import kotlin.math.roundToInt

open class Product360RotatingView : ImageView, GestureDetector.OnGestureListener {

    private var mDirectionMultiplier = 1
    private var mRotationsPerScreen = DEFAULT_ROTATIONS_PER_SCREEN
    private var mLevelDrawable: LevelListDrawable
    private var mRemakeNeeded = false
    private val mHandler = Handler()
    private var mCurrentLevel = 0
    private var mImageCount = 0
    private var mPixelsPerLevel = 0
    private var mStartX = 0
    private var mEndX = 0

    private var mDisplayMetrics: DisplayMetrics
    private var mDetector: GestureDetectorCompat

    constructor(context: Context)
            : this(context, null)

    constructor(context: Context, attr: AttributeSet?)
            : this(context, attr, 0)

    constructor(context: Context, attr: AttributeSet?, defStyleAttr: Int)
            : super(context, attr, defStyleAttr) {
        val attributes = context.obtainStyledAttributes(attr, R.styleable.Product360RotatingView,
                defStyleAttr, 0)
        if (attributes.hasValue(R.styleable.Product360RotatingView_reversed_rotation)) {
            mDirectionMultiplier = if (attributes.getBoolean(R.styleable.Product360RotatingView_reversed_rotation, false)) {
                -1
            } else {
                1
            }
        }

        if (attributes.hasValue(R.styleable.Product360RotatingView_rotations_per_screen)) {
            mRotationsPerScreen = attributes.getFloat(R.styleable.Product360RotatingView_rotations_per_screen, DEFAULT_ROTATIONS_PER_SCREEN)
        }
        attributes.recycle()

        setImageResource(R.drawable.level_list_drawable)
        mLevelDrawable = drawable as LevelListDrawable
        mDetector = GestureDetectorCompat(context, this)
        mDisplayMetrics = context.resources.displayMetrics
    }


    /**
     * @param images sorted list of product photos drawables
     */
    open fun setImagesDrawables(images: List<Drawable>) {
        mImageCount = images.size
        mPixelsPerLevel = (mDisplayMetrics.widthPixels / (mImageCount * mRotationsPerScreen)).roundToInt()
        if (mRemakeNeeded) {
            setImageResource(R.drawable.level_list_drawable)
            mLevelDrawable = drawable as LevelListDrawable
        }
        mRemakeNeeded = true
        for ((i, image) in images.withIndex()) {
            mLevelDrawable.addLevel(i, i, image)
        }
    }

    /**
     * @param images sorted list of product photos urls
     */
    open fun setImagesLinks(images: List<String>) {
        mImageCount = images.size
        mPixelsPerLevel = (mDisplayMetrics.widthPixels / (mImageCount * mRotationsPerScreen)).roundToInt()
        if (mRemakeNeeded) {
            setImageResource(R.drawable.level_list_drawable)
            mLevelDrawable = drawable as LevelListDrawable
        }
        mRemakeNeeded = true

        throw NotImplementedError()
    }

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }


    /**
     * Stops current rotation
     */
    override fun onDown(e: MotionEvent?): Boolean {
        mHandler.removeCallbacksAndMessages(null)
        return false
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        if (Math.abs(velocityX) > 2 * Math.abs(velocityY)) {
            changeLevelWithMomentum(velocityX)
        }
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (mDetector.onTouchEvent(event)) {
            true
        } else {
            val action = event.actionMasked

            when (action) {
                (MotionEvent.ACTION_DOWN) -> {

                    mStartX = event.x.toInt()
                    return true

                }
                (MotionEvent.ACTION_MOVE) -> {

                    mEndX = event.x.toInt()

                    val fractionOfScreenMoved = Math.abs(mEndX - mStartX).toDouble() / mDisplayMetrics.widthPixels.toDouble()
                    val numberOfLevelsChanged = (mImageCount * mRotationsPerScreen * fractionOfScreenMoved).toInt()

                    if ((mEndX - mStartX) * mDirectionMultiplier > 2) {

                        mCurrentLevel -= numberOfLevelsChanged
                        if (mCurrentLevel < 0) {
                            mCurrentLevel = mCurrentLevel.rem(mImageCount)
                            if (mCurrentLevel < 0) {
                                mCurrentLevel += mImageCount
                            }
                        }

                        mStartX += numberOfLevelsChanged * mPixelsPerLevel * mDirectionMultiplier
                        mLevelDrawable.level = mCurrentLevel

                    } else if ((mEndX - mStartX) * mDirectionMultiplier < -2) {
                        mCurrentLevel += numberOfLevelsChanged
                        if (mCurrentLevel > mImageCount - 1) {
                            mCurrentLevel = mCurrentLevel.rem(mImageCount)
                        }

                        mStartX -= numberOfLevelsChanged * mPixelsPerLevel * mDirectionMultiplier

                        mLevelDrawable.level = mCurrentLevel

                    }
                    return true

                }
                (MotionEvent.ACTION_UP) -> {
                    mEndX = event.x.toInt()

                    return true

                }

                (MotionEvent.ACTION_CANCEL) -> {
                    return true
                }

                (MotionEvent.ACTION_OUTSIDE) -> {
                    return true
                }

                else -> {
                    return super.onTouchEvent(event)
                }
            }
        }


    }

    private fun changeLevelWithMomentum(velocityX: Float) {
        val momentum: Int
        when {
            Math.abs(velocityX) < 200.toFloat() -> {
                momentum = (mImageCount * 0.33).roundToInt()
            }

            Math.abs(velocityX) < 1000.toFloat() -> {
                momentum = (mImageCount * 0.66).roundToInt()
            }

            Math.abs(velocityX) < 5000.toFloat() -> {
                momentum = mImageCount
            }

            Math.abs(velocityX) < 10000.toFloat() -> {
                momentum = (mImageCount * 1.33).roundToInt()
            }

            else -> {
                momentum = (mImageCount * 1.66).roundToInt()
            }
        }
        if (velocityX > 0) {
            momentumChange(momentum, -1)
        } else {
            momentumChange(momentum, 1)
        }

    }

    private fun momentumChange(momentum: Int, direction: Int) {
        if (momentum <= 0) {
            return
        }
        for (i in 0..(momentum.toDouble() / 20).roundToInt()) {
            changeLevel(direction)
        }
        mHandler.postDelayed({
            momentumChange(momentum - 1, direction)
        }, DEFAULT_IMAGE_SWAP_TIME)
    }

    private fun changeLevel(direction: Int) {
        if (direction * mDirectionMultiplier < 0) {
            if (mCurrentLevel <= 0) {
                mCurrentLevel = mImageCount - 1
            } else {
                mCurrentLevel -= 1
            }
        } else {
            mCurrentLevel = (mCurrentLevel + 1).rem(mImageCount)
        }
        mLevelDrawable.level = mCurrentLevel
    }

    companion object {
        private const val DEFAULT_ROTATIONS_PER_SCREEN: Float = 0.5.toFloat()
        private const val DEFAULT_IMAGE_SWAP_TIME: Long = 16
    }
}