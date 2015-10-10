
package me.angrybyte.numberpicker.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import me.angrybyte.numberpicker.BuildConfig;
import me.angrybyte.numberpicker.R;

/**
 * FIXME: Add docs
 */
public class ActualNumberPicker extends View {

    private static final String TAG = ActualNumberPicker.class.getSimpleName();
    private static final int DEFAULT_BAR_COUNT = 10;
    private static final int ARR_LEFT = 0xC1;
    private static final int ARR_RIGHT = 0xC2;
    private static final int FAST_ARR_LEFT = 0xF1;
    private static final int FAST_ARR_RIGHT = 0xF2;
    private static final int CONTROL_TEXT = 0x00;

    private Rect mTextBounds = new Rect(0, 0, 0, 0);
    private Point mTextDimens = new Point(0, 0);
    private TextPaint mTextPaint;
    private float mTextSize = -1.0f;
    private boolean mShowText = true;

    private Paint mBarsPaint;
    private int mBarsCount = DEFAULT_BAR_COUNT;
    private int mMinBarWidth;
    private boolean mShowBars = true;

    private Paint mControlsPaint;
    private boolean mShowControls = true;

    private Paint mFastControlsPaint;
    private boolean mShowFastControls = true;

    private int mMinHeight;
    private int mWidth = 0;
    private int mHeight = 0;

    private int mMinValue = 0;
    private int mMaxValue = 1000;
    private int mValue = 50;
    private int mSelectedControl; // one of the constants from the top

    public ActualNumberPicker(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public ActualNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public ActualNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ActualNumberPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * FIXME: Add docs
     */
    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ActualNumberPicker, defStyleAttr, defStyleRes);

        mShowBars = attributes.getBoolean(R.styleable.ActualNumberPicker_show_bars, true);
        mShowControls = attributes.getBoolean(R.styleable.ActualNumberPicker_show_controls, true);
        mShowFastControls = attributes.getBoolean(R.styleable.ActualNumberPicker_show_fast_controls, true);

        int barsColor = attributes.getColor(R.styleable.ActualNumberPicker_bar_color, 0xFF404040);
        mBarsPaint = new Paint();
        mBarsPaint.setAntiAlias(true);
        mBarsPaint.setStyle(Paint.Style.FILL);
        mBarsPaint.setColor(barsColor);

        int controlsColor = attributes.getColor(R.styleable.ActualNumberPicker_controls_color, 0xFF404040);
        mControlsPaint = new Paint();
        mControlsPaint.setAntiAlias(true);
        mControlsPaint.setStyle(Paint.Style.FILL);
        mControlsPaint.setColor(controlsColor);

        int fastControlsColor = attributes.getColor(R.styleable.ActualNumberPicker_fast_controls_color, 0xFF404040);
        mFastControlsPaint = new Paint();
        mFastControlsPaint.setAntiAlias(true);
        mFastControlsPaint.setStyle(Paint.Style.FILL);
        mFastControlsPaint.setColor(fastControlsColor);

        int textColor = attributes.getColor(R.styleable.ActualNumberPicker_text_color, 0xFF4040FF);
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setLinearText(true);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mTextPaint.setHinting(Paint.HINTING_ON);
        }
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(textColor);

        mTextSize = attributes.getDimension(R.styleable.ActualNumberPicker_text_size, mTextSize);
        if (mTextSize != -1.0f) {
            mTextPaint.setTextSize(mTextSize);
        }

        mShowText = attributes.getBoolean(R.styleable.ActualNumberPicker_show_text, mShowText);

        mMinValue = attributes.getInt(R.styleable.ActualNumberPicker_min_value, mMinValue);
        mMaxValue = attributes.getInt(R.styleable.ActualNumberPicker_max_value, mMaxValue);
        if (mMaxValue <= mMinValue) {
            throw new RuntimeException("Cannot use max_value " + mMaxValue + " because the min_value is " + mMinValue);
        }

        mValue = attributes.getInt(R.styleable.ActualNumberPicker_value, (mMaxValue + mMinValue) / 2);
        if (mValue < mMinValue || mValue > mMaxValue) {
            throw new RuntimeException("Cannot use value " + mValue + " because it is out of range");
        }

        mBarsCount = attributes.getInteger(R.styleable.ActualNumberPicker_bars_count, DEFAULT_BAR_COUNT);

        attributes.recycle();

        mMinBarWidth = context.getResources().getDimensionPixelSize(R.dimen.min_bar_width);
        mMinHeight = context.getResources().getDimensionPixelSize(R.dimen.min_height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int height;

        if (heightMode == MeasureSpec.EXACTLY) {
            // respect min_height value
            height = Math.max(mMinHeight, heightSize);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            // take whichever is smaller, height <-> parent height
            if (mHeight == 0) {
                // no calculations yet, use min_height
                height = Math.min(mMinHeight, heightSize);
            } else {
                // secondary pass, already calculated height, so use that
                height = Math.min(mHeight, heightSize);
            }
        } else {
            // doesn't matter
            height = Math.max(mMinHeight, heightSize);
        }

        mHeight = height;
        mWidth = calculateWidth(widthSize, widthMode, mHeight); // fast_controls x2, controls x2, text

        // MUST CALL THIS
        setMeasuredDimension(mWidth, mHeight);
        updateTextSize();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "Size changing... " + oldw + "x" + oldh + " -> " + w + "x" + h);
        mHeight = Math.max(h, mHeight);
        mWidth = calculateWidth(w, MeasureSpec.EXACTLY, mHeight);
        updateTextSize();
        super.onSizeChanged(mWidth, mHeight, oldw, oldh);
    }

    /**
     * FIXME: Add docs
     */
    private int calculateWidth(int requestedWidth, int requestedWidthMode, int height) {
        if (mShowControls && mShowFastControls) {
            return height * 5;
        } else if (!mShowControls && !mShowFastControls) {
            if (requestedWidthMode == MeasureSpec.EXACTLY) {
                return requestedWidth;
            } else if (requestedWidthMode == MeasureSpec.AT_MOST) {
                return requestedWidth;
            } else {
                return height * 5;
            }
        } else {
            // only one of control sets is visible
            return height * 3;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (hasWindowFocus) {
            updateTextSize();
        }
    }

    /**
     * FIXME: Add docs
     */
    private void updateTextSize() {
        if (mTextSize != -1.0f) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Not calculating text size, a predefined value is set: " + mTextPaint.getTextSize());
            }
            return;
        }

        float size = 14f; // 14px on LDPI x system font factor
        Rect bounds = new Rect(0, 0, 0, 0);
        mTextPaint.setTextSize(size);
        mTextPaint.getTextBounds("00", 0, 1, bounds);

        // this loop exits when text size becomes too big
        while (bounds.height() < mHeight - mHeight * 0.4f) {
            mTextPaint.setTextSize(size++);
            mTextPaint.getTextBounds("AA", 0, 1, bounds);
        }
    }

    /**
     * Updates the indicator text size. Set to {@code -1.0f} to use maximum sized text.
     *
     * @param size A dimension representing the text size
     */
    public void setTextSize(float size) {
        mTextSize = size;
        if (mTextSize != -1.0f) {
            mTextPaint.setTextSize(size);
        }
        updateTextSize();
    }

    /**
     * Measures the given text and saves dimensions to the {@link #mTextDimens} field.
     *
     * @param text Which text to measure
     */
    private void measureText(String text) {
        // accurate for height
        mTextPaint.getTextBounds(text, 0, text.length(), mTextBounds);
        mTextDimens.y = Math.abs(mTextBounds.height());

        // accurate for width
        mTextDimens.x = (int) Math.floor(mTextPaint.measureText(text));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "Touching!" + event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float percent = event.getX() / (float) mWidth;
                mValue = (int) Math.floor(percent * mMaxValue + mMinValue);
                if (mValue < mMinValue) {
                    mValue = mMinValue;
                } else if (mValue > mMaxValue) {
                    mValue = mMaxValue;
                }
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mShowText) {
            String value = String.valueOf((int) Math.floor(mValue));
            measureText(value); // this will save dimensions to mTextDimens
            int x = mWidth / 2 - mTextDimens.x / 2;
            int y = mHeight / 2 + mTextDimens.y / 2;
            canvas.drawText(value, x, y, mTextPaint);
        }
    }

}
