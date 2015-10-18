
package me.angrybyte.numberpicker.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import me.angrybyte.numberpicker.BuildConfig;
import me.angrybyte.numberpicker.R;

/**
 * A horizontal number picker widget. Every aspect of the view is configurable, for more information see the view's attribute set
 * (everything is self-explanatory).
 */
public class ActualNumberPicker extends View {

    private static final String TAG = ActualNumberPicker.class.getSimpleName();
    private static final int DEFAULT_BAR_COUNT = 11;
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

    private Paint mBarPaint;
    private RectF mBarBounds = new RectF(0, 0, 0, 0);
    private int mBarCount = DEFAULT_BAR_COUNT;
    private int mMinBarWidth;
    private int mBarWidth = mMinBarWidth;
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
     * Initializes the view from any constructor, utilizing the theme engine, and using the assigned attributes.
     *
     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource that supplies default values for
     *            the view. Can be 0 to not look for defaults
     * @param defStyleRes A resource identifier of a style resource that supplies default values for the view, used only if defStyleAttr is
     *            0 or can not be found in the theme. Can be 0 to not look for defaults
     */
    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ActualNumberPicker, defStyleAttr, defStyleRes);

        mShowBars = attributes.getBoolean(R.styleable.ActualNumberPicker_show_bars, true);
        mShowControls = attributes.getBoolean(R.styleable.ActualNumberPicker_show_controls, true);
        mShowFastControls = attributes.getBoolean(R.styleable.ActualNumberPicker_show_fast_controls, true);

        int barsColor = attributes.getColor(R.styleable.ActualNumberPicker_bar_color, 0xFF404040);
        mBarPaint = new Paint();
        mBarPaint.setAntiAlias(true);
        mBarPaint.setStyle(Paint.Style.FILL);
        mBarPaint.setColor(barsColor);

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

        mBarCount = attributes.getInteger(R.styleable.ActualNumberPicker_bars_count, DEFAULT_BAR_COUNT);
        if (mBarCount < 3) {
            mBarCount = DEFAULT_BAR_COUNT;
        } else if (mBarCount % 2 != 0) {
            mBarCount++;
        }
        mMinBarWidth = context.getResources().getDimensionPixelSize(R.dimen.min_bar_width);
        mBarWidth = attributes.getDimensionPixelSize(R.styleable.ActualNumberPicker_bar_width, mMinBarWidth);
        if (mBarWidth < mMinBarWidth) {
            mBarWidth = mMinBarWidth;
        }

        attributes.recycle();

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
     * Does necessary calculations to ensure there is enough space for all components (value, right/left controls, and fast controls).<br>
     * Width required is:
     * <ol>
     * <li><b>When all controls are shown</b> - Minimum 5x bigger than the height</li>
     * <li><b>When only one set of controls is shown</b> (right/left OR fast controls) - Minimum 3x bigger than the height</li>
     * <li><b>When no controls are shown</b> - Either a fixed width, a parent-matching width or minimum 5x bigger than the height</li>
     * </ol>
     *
     * @param requestedWidth A value that was requested for width (by {@link #onSizeChanged(int, int, int, int)} or by
     *            {@link #onMeasure(int, int)}), in pixels
     * @param requestedWidthMode An integer constant from {@link android.view.View.MeasureSpec} that declares measuring mode
     * @param height Pre-defined height of the view, in pixels
     * @return Calculated width of the view, in pixels
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
     * This sets the optimal text size for the value number. If there is a predefined {@link #mTextSize}, then that value is used. If there
     * is no defined value for text size, this method calculated the optimal size, which is around 60% of the View's height.
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
        // accurate measure for height
        mTextPaint.getTextBounds(text, 0, text.length(), mTextBounds);
        mTextDimens.y = Math.abs(mTextBounds.height());

        // accurate measure for width
        mTextDimens.x = (int) Math.floor(mTextPaint.measureText(text));
        // update text bounds, will need it for later
        mTextBounds.set(mTextBounds.left, mTextBounds.top, mTextBounds.left + mTextDimens.x, mTextBounds.bottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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

    /**
     * Penner's linear easing function, plotted by time and distance for a motion tween. Can be used for density, width and other properties
     * that should behave the same.
     *
     * @param b The beginning value of the property
     * @param t The current time (or position) of the tween. This can be seconds or frames, steps, ms, whatever – as long as the unit is the
     *            same as is used for the total time
     * @param d The total time of the tween
     * @param c The change between the beginning and destination value of the property
     * @return The new value that has resulted from the equation
     */
    public float linear(float t, float b, float c, float d) {
        return c * (t / d) + b;
    }

    /**
     * Penner's sine easing in function, plotted by time and distance for a motion tween. Can be used for density, width and other
     * properties that should behave the same.
     *
     * @param b The beginning value of the property
     * @param t The current time (or position) of the tween. This can be seconds or frames, steps, ms, whatever – as long as the unit is the
     *            same as is used for the total time
     * @param d The total time of the tween
     * @param c The change between the beginning and destination value of the property
     * @return The new value that has resulted from the equation
     */
    public float easeIn(float t, float b, float c, float d) {
        return -c * (float) Math.cos(t / d * (Math.PI / 2)) + c + b;
    }

    /**
     * Penner's sine easing out function, plotted by time and distance for a motion tween. Can be used for density, width and other
     * properties that should behave the same.
     *
     * @param b The beginning value of the property
     * @param t The current time (or position) of the tween. This can be seconds or frames, steps, ms, whatever – as long as the unit is the
     *            same as is used for the total time
     * @param d The total time of the tween
     * @param c The change between the beginning and destination value of the property
     * @return The new value that has resulted from the equation
     */
    public float easeOut(float t, float b, float c, float d) {
        return c * (float) Math.sin(t / d * (Math.PI / 2)) + b;
    }

    /**
     * Calculates how high the bar needs to be for the given index. Higher ones appear near the middle, i.e. when index is near the 1/2 of
     * the total bar count.
     *
     * @param minHeight Minimum allowed height of a bar
     * @param maxHeight Maximum allowed height of a bar
     * @param barCount How many bars are there
     * @param index Which bar is being measured
     * @return Correct, scaled height of the given bar (determined by the index parameter)
     */
    private int calculateBarHeight(int minHeight, int maxHeight, int barCount, int index) {
        float height;
        if (index < barCount / 2) {
            height = linear(index, minHeight, maxHeight - minHeight, barCount / 2f);
        } else {
            height = linear(index - barCount / 2f, maxHeight, minHeight - maxHeight, barCount / 2f);
        }

        return (int) Math.floor(height);
    }

    /**
     * Calculates where the given bar should be, more dense bars appear near the edges of the view.
     *
     * @param barWidth How wide is a single bar
     * @param totalWidth How wide is the whole container
     * @param barCount How many bars are there
     * @param index Which bar is being measured
     * @return X coordinate of the given bar (determined by the index parameter)
     */
    private int calculateBarX(int barWidth, int totalWidth, int barCount, int index) {
        // (mWidth / (mBarCount + 1)) * (i + 1) - mBarWidth / 2;
        float margin = 2;
        float x;
        if (index <= barCount / 2) {
            x = easeIn(index, margin, totalWidth / 2f, barCount / 2f);
        } else {
            x = easeOut(index - barCount / 2f, totalWidth / 2f, totalWidth / 2f - margin, barCount / 2f);
        }

        return (int) Math.floor(x - barWidth / 2f);
    }

    /**
     * @param minOpacity Minimum allowed opacity of a bar
     * @param maxOpacity Maximum allowed opacity of a bar
     * @param barCount How many bars are there
     * @param index Which bar is being measured
     * @return Correct opacity for the given bar (determined by the index parameter)
     */
    private float calculateBarOpacity(float minOpacity, float maxOpacity, int barCount, int index) {
        if (index < barCount / 2) {
            return linear(index, minOpacity, maxOpacity - minOpacity, barCount / 2f);
        } else {
            return linear(index - barCount / 2f, maxOpacity, minOpacity - maxOpacity, barCount / 2f);
        }
    }

    /**
     * Checks whether the text overlaps the bar that is about to be drawn.<br>
     * <b>Note</b>: This method fakes the text width, i.e. increases it to allow for some horizontal padding.
     * 
     * @param textBounds Which text bounds to measure
     * @param barBounds Which bar bounds to measure
     * @return {@code True} if bounds overlap each other, {@code false} if not
     */
    private boolean textOverlapsBars(Rect textBounds, RectF barBounds) {
        // increase original text width to give some padding to the text
        int textL = textBounds.left - (int) Math.floor(textBounds.width() * 0.6f);
        int textR = textBounds.right + (int) Math.floor(textBounds.width() * 0.6f);
        int textT = textBounds.top;
        int textB = textBounds.bottom;
        return barBounds.intersects(textL, textT, textR, textB);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mShowText) {
            String value = String.valueOf((int) Math.floor(mValue));
            // this will save dimensions to mTextDimens
            measureText(value);
            int x = mWidth / 2 - mTextDimens.x / 2;
            int y = mHeight / 2 + mTextDimens.y / 2;
            canvas.drawText(value, x, y, mTextPaint);
            // update bounds to re-use later
            mTextBounds.set(x, y, x + mTextBounds.width(), y + mTextBounds.height());
        }

        if (mShowBars) {
            int x, y, minBarH, maxBarH, barH, opacity;
            // draw all bars, but draw one more in the end with '<=' instead of '<' (to be symmetric)
            for (int i = 0; i <= mBarCount; i++) {
                // smaller ones should be nearer to the sides
                maxBarH = (int) Math.floor(0.4f * mHeight);
                minBarH = (int) Math.floor(maxBarH * 0.9f);
                barH = calculateBarHeight(minBarH, maxBarH, mBarCount, i);
                x = calculateBarX(mBarWidth, mWidth, mBarCount, i);
                y = mHeight / 2 - barH / 2;
                mBarBounds.set(x, y, x + mBarWidth, y + barH);
                // don't draw if it overlaps the text
                if (!textOverlapsBars(mTextBounds, mBarBounds)) {
                    opacity = (int) Math.floor(calculateBarOpacity(0.3f, 1f, mBarCount, i) * 255);
                    Log.d(TAG, "Opacity is " + opacity);
                    mBarPaint.setAlpha(opacity);
                    canvas.drawRoundRect(mBarBounds, mBarBounds.width() / 3f, mBarBounds.width() / 3f, mBarPaint);
                }
            }
        }
    }

}
