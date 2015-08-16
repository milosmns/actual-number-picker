
package me.angrybyte.numberpicker.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

import me.angrybyte.numberpicker.BuildConfig;
import me.angrybyte.numberpicker.R;

public class NumberPickerView extends View {

    private static final String TAG = NumberPickerView.class.getSimpleName();
    private static final int INVALID_POINTER = -1;
    private static final int MAX_SCROLL_DURATION = 2000;
    public static final int NO_POSITION = -1;
    public static final int TOUCH_SLOP_PAGING = 1;
    public static final int TOUCH_SLOP_DEFAULT = 0;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;

    private OnScrollListener mScrollListener;
    private ViewFlinger mViewFlinger = new ViewFlinger();
    private VelocityTracker mVelocityTracker;
    private int mScrollState = SCROLL_STATE_IDLE;
    private int mPointerId = INVALID_POINTER;
    private int mTouchSlop;
    private int mLastTouchX;
    private int mLastTouchY;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private boolean mIsAttached;
    private boolean mHasFixedSize;
    private boolean mFirstLayoutComplete;

    public NumberPickerView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public NumberPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public NumberPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NumberPickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, 0, 0);
    }

    private void init(Context context, AttributeSet attrs, int i, int defStyle) {
        mVelocityTracker = VelocityTracker.obtain();

        ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.NumberPickerView, defStyle, 0);

        a.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    private boolean canScrollLeft() {
        return true; // TODO: add a real check here
    }

    private boolean canScrollRight() {
        return true; // TODO: add a real check here
    }

    @Override
    public void scrollBy(int x, int y) {
        if (canScrollLeft() || canScrollRight()) {
            // TODO check x position if it's positive/negative?? 
            scrollByInternal(x, 0);
        }
    }

    private void notifyOnScrolled(int hresult, int vresult) {
        // dummy values, View's implementation does not use these
        onScrollChanged(0, 0, 0, 0);
        if (mScrollListener != null) {
            mScrollListener.onScrolled(this, hresult, vresult);
        }
    }

    /**
     * Does not perform bounds checking. Used by internal methods that have already validated input.
     *
     * @return True if any scroll was consumed in either direction, false otherwise
     */
    private boolean scrollByInternal(int x, int y) {
        int hresult = 0;
        // TODO: update hresult (horizontal scroll value) - move items by that much?

        consumePendingUpdateOperations();
        if (hresult != 0) {
            notifyOnScrolled(hresult, 0);
        }

        if (!awakenScrollBars()) {
            invalidate();
        }

        // TODO: should more conditions invalidate the view?

        return hresult != 0;
    }

    private void consumePendingUpdateOperations() {
        mUpdateInnerElements.run();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // TODO: if click happened, cancel touch and get input for text        

        boolean canScrollLeft = canScrollLeft();
        boolean canScrollRight = canScrollRight();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(e);

        int action = MotionEventCompat.getActionMasked(e);
        int actionIndex = MotionEventCompat.getActionIndex(e);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mPointerId = MotionEventCompat.getPointerId(e, 0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                mPointerId = MotionEventCompat.getPointerId(e, actionIndex);
                mInitialTouchX = mLastTouchX = (int) (MotionEventCompat.getX(e, actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (MotionEventCompat.getY(e, actionIndex) + 0.5f);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int index = MotionEventCompat.findPointerIndex(e, mPointerId);
                if (index < 0) {
                    String errText = "Error processing scroll; pointer index for id %s not found. Did any MotionEvents get skipped?";
                    Log.e(TAG, String.format(errText, mPointerId));
                    return false;
                }

                int x = (int) (MotionEventCompat.getX(e, index) + 0.5f);
                int y = (int) (MotionEventCompat.getY(e, index) + 0.5f);

                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    int dx = x - mInitialTouchX;
                    int dy = y - mInitialTouchY;
                    boolean startScroll = false;
                    // TODO: check if dx is negative/positive to determine direction?
                    if ((canScrollLeft || canScrollRight) && Math.abs(dx) > mTouchSlop) {
                        mLastTouchX = mInitialTouchX + mTouchSlop * (dx < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }

                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    int dx = x - mLastTouchX;
                    // TODO: check conditions here (maybe they are reversed)
                    if (dx < 0) {
                        // going left
                        if (scrollByInternal(canScrollLeft ? -dx : 0, 0)) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    } else {
                        // going right
                        if (scrollByInternal(canScrollRight ? -dx : 0, 0)) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                mLastTouchX = x;
                mLastTouchY = y;
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(e);
                break;
            }
            case MotionEvent.ACTION_UP: {
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                // TODO: again, check conditions to determine x direction
                float xvel = (canScrollLeft || canScrollRight) ? -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mPointerId) : 0;
                if (!((xvel != 0) && fling((int) xvel, 0))) {
                    setScrollState(SCROLL_STATE_IDLE);
                }
                mVelocityTracker.clear();
                // TODO: if edge glow is added (ever), hide it here (both left and right)
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                cancelTouch();
                break;
            }
        }

        return true;
    }

    private void cancelTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
        // TODO: if edge glow is added (ever), hide it here (both left and right)

        setScrollState(SCROLL_STATE_IDLE);
    }

    private void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Setting scroll state to " + state + " from " + mScrollState, new Exception());
        }

        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {
            stopScrollersInternal();
        }
        if (mScrollListener != null) {
            mScrollListener.onScrollStateChanged(this, state);
        }
    }

    public void smoothScrollBy(int dx, int dy) {
        // TODO check conditions (reverse?)
        if (canScrollLeft() && dx < 0) {
            mViewFlinger.smoothScrollBy(dx, 0);
        } else if (canScrollRight() && dx > 0) {
            mViewFlinger.smoothScrollBy(dx, 0);
        }
    }

    public boolean fling(int velocityX, int velocityY) {
        boolean canScrollLeft = canScrollLeft();
        boolean canScrollRight = canScrollRight();

        // TODO: check x direction?
        if (!(canScrollLeft || canScrollRight) || Math.abs(velocityX) < mMinFlingVelocity) {
            velocityX = 0;
        }
        velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
        if (velocityX != 0 || velocityY != 0) {
            mViewFlinger.fling(velocityX, velocityY);
            return true;
        }
        return false;
    }

    public void stopScroll() {
        setScrollState(SCROLL_STATE_IDLE);
        stopScrollersInternal();
    }

    private void stopScrollersInternal() {
        mViewFlinger.stop();
    }

    private void onPointerUp(MotionEvent e) {
        int actionIndex = MotionEventCompat.getActionIndex(e);
        if (MotionEventCompat.getPointerId(e, actionIndex) == mPointerId) {
            // pick a new pointer to pick up the slack
            int newIndex = actionIndex == 0 ? 1 : 0;
            mPointerId = MotionEventCompat.getPointerId(e, newIndex);
            // some cheating here..
            mInitialTouchX = mLastTouchX = (int) (MotionEventCompat.getX(e, newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (MotionEventCompat.getY(e, newIndex) + 0.5f);
        }
    }

    public void setScrollingTouchSlop(int slopConstant) {
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        switch (slopConstant) {
            default:
            case TOUCH_SLOP_DEFAULT: {
                mTouchSlop = vc.getScaledTouchSlop();
                break;
            }
            case TOUCH_SLOP_PAGING: {
                mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(vc);
                break;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttached = true;
        mFirstLayoutComplete = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayoutComplete = false;
        stopScroll();
        mIsAttached = false;
    }

    /* View Flinger Implementation */

    private class ViewFlinger implements Runnable {
        private int mLastFlingX;
        private int mLastFlingY;
        private ScrollerCompat mScroller;
        private Interpolator mInterpolator = sQuinticInterpolator;

        // When set to true, postOnAnimation callbacks are delayed until the run method completes
        private boolean mEatRunOnAnimationRequest = false;

        // Tracks if postAnimationCallback should be re-attached when it is done
        private boolean mReSchedulePostAnimationCallback = false;

        public ViewFlinger() {
            mScroller = ScrollerCompat.create(getContext(), sQuinticInterpolator);
        }

        @Override
        public void run() {
            disableRunOnAnimationRequests();
            consumePendingUpdateOperations();
            // keep a local reference so that if it is changed during onAnimation method, it won't
            // cause unexpected behaviors
            final ScrollerCompat scroller = mScroller;

            // TODO: implement a smooth scroller
            enableRunOnAnimationRequests();
        }

        private void disableRunOnAnimationRequests() {
            mReSchedulePostAnimationCallback = false;
            mEatRunOnAnimationRequest = true;
        }

        private void enableRunOnAnimationRequests() {
            mEatRunOnAnimationRequest = false;
            if (mReSchedulePostAnimationCallback) {
                postOnAnimation();
            }
        }

        void postOnAnimation() {
            if (mEatRunOnAnimationRequest) {
                mReSchedulePostAnimationCallback = true;
            } else {
                removeCallbacks(this);
                ViewCompat.postOnAnimation(NumberPickerView.this, this);
            }
        }

        public void fling(int velocityX, int velocityY) {
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = mLastFlingY = 0;
            mScroller.fling(0, 0, velocityX, velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            postOnAnimation();
        }

        public void smoothScrollBy(int dx, int dy) {
            smoothScrollBy(dx, dy, 0, 0);
        }

        public void smoothScrollBy(int dx, int dy, int vx, int vy) {
            smoothScrollBy(dx, dy, computeScrollDuration(dx, dy, vx, vy));
        }

        private float distanceInfluenceForSnapDuration(float f) {
            f -= 0.5f; // center the values about 0.
            f *= 0.3f * Math.PI / 2.0f;
            return (float) Math.sin(f);
        }

        private int computeScrollDuration(int dx, int dy, int vx, int vy) {
            final int absDx = Math.abs(dx);
            final int absDy = Math.abs(dy);
            final boolean horizontal = absDx > absDy;
            final int velocity = (int) Math.sqrt(vx * vx + vy * vy);
            final int delta = (int) Math.sqrt(dx * dx + dy * dy);
            final int containerSize = horizontal ? getWidth() : getHeight();
            final int halfContainerSize = containerSize / 2;
            final float distanceRatio = Math.min(1.f, 1.f * delta / containerSize);
            final float distance = halfContainerSize + halfContainerSize * distanceInfluenceForSnapDuration(distanceRatio);

            final int duration;
            if (velocity > 0) {
                duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
            } else {
                float absDelta = (float) (horizontal ? absDx : absDy);
                duration = (int) (((absDelta / containerSize) + 1) * 300);
            }
            return Math.min(duration, MAX_SCROLL_DURATION);
        }

        public void smoothScrollBy(int dx, int dy, int duration) {
            smoothScrollBy(dx, dy, duration, sQuinticInterpolator);
        }

        public void smoothScrollBy(int dx, int dy, int duration, Interpolator interpolator) {
            if (mInterpolator != interpolator) {
                mInterpolator = interpolator;
                mScroller = ScrollerCompat.create(getContext(), interpolator);
            }
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = mLastFlingY = 0;
            mScroller.startScroll(0, 0, dx, dy, duration);
            postOnAnimation();
        }

        public void stop() {
            removeCallbacks(this);
            mScroller.abortAnimation();
        }

    }

    /* Inner Elements Updater Implementation */

    // @formatter:off
    /**
     * Note: this Runnable is only posted if:
     * 1) We've been through first layout
     * 2) We know we have a fixed size (mHasFixedSize)
     * 3) We're attached to a Window
     */
    // @formatter:on
    private final Runnable mUpdateInnerElements = new Runnable() {
        public void run() {
            if (!mFirstLayoutComplete) {
                // a layout request will happen, we should not do layout here
                return;
            }
            // TODO: should we even do anything here?
        }
    };

    /* Quintic Interpolator Implementation */

    private final Interpolator sQuinticInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    /* On Scroll Listener Adapter Class */

    public static abstract class OnScrollListener {
        public void onScrollStateChanged(NumberPickerView numberPicker, int newState) {
        }

        public void onScrolled(NumberPickerView numberPicker, int dx, int dy) {
        }
    }

}
