
package me.angrybyte.numberpicker.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;

import me.angrybyte.numberpicker.BuildConfig;
import me.angrybyte.numberpicker.Coloring;
import me.angrybyte.numberpicker.R;
import me.angrybyte.numberpicker.listener.OnValueChangeListener;

/**
 * A horizontal number picker widget. Every aspect of the view is configurable, for more information see the view's attribute set
 * (everything is self-explanatory).
 */
public class ActualNumberPicker extends View {

    // @formatter:off
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ CONTROL_NONE, ARR_LEFT, ARR_RIGHT, FAST_ARR_LEFT, FAST_ARR_RIGHT })
    public @interface Control {} // @formatter:on

    private static final String TAG = ActualNumberPicker.class.getSimpleName();
    private static final int DEFAULT_BAR_COUNT = 11;
    private static final int CONTROL_NONE = 0x00;
    private static final int ARR_LEFT = 0xC1;
    private static final int ARR_RIGHT = 0xC2;
    private static final int FAST_ARR_LEFT = 0xF1;
    private static final int FAST_ARR_RIGHT = 0xF2;
    private static final int CONTROL_TEXT = 0xAA;
    private static final int[] STATE_NORMAL = new int[] {};

    private final Rect mTextBounds = new Rect(0, 0, 0, 0);
    private final Point mTextDimens = new Point(0, 0);
    private TextPaint mTextPaint;
    private float mTextSize = -1.0f;
    private boolean mShowText = true;
    private boolean mDrawOverText = false;
    private boolean mDrawOverControls = true;

    private Paint mBarPaint;
    private final RectF mBarBounds = new RectF(0, 0, 0, 0);
    private int mBarCount = DEFAULT_BAR_COUNT;
    private int mMinBarWidth = 1;
    private String mMinBarHeight = "large";
    private int mBarWidth = mMinBarWidth;
    private boolean mShowBars = true;

    private Paint mHighlightPaint;
    private boolean mShowHighlight = true;

    private float mDensityFactor = 1;
    private float mLastX = Float.MAX_VALUE;
    private float mDelta = 0;

    private boolean mShowControls = true;
    private boolean mShowFastControls = true;

    private int mMinHeight = 0;
    private int mWidth = 0;
    private int mHeight = 0;

    private int mMinValue = 0;
    private int mMaxValue = 1000;
    private double mValue = 50;
    private double mValueAdjustment = 1.0d; // Set to 0.2 if you want the slider to go 40 -> 40.2 -> 40.4, etc.

    @Control
    // one of the constants from the top
    private int mSelectedControl = CONTROL_NONE;
    private int mMaxControlSize = mMinHeight;
    private int mSelectionColor = Color.GRAY;

    private Handler mHandler;
    private final SparseArray<Drawable> mControlIcons = new SparseArray<>(4);
    private final SparseArray<Drawable> mControlsBacks = new SparseArray<>(4);

    private OnValueChangeListener mListener;

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
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mHandler = new Handler();
        setClickable(true);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ActualNumberPicker, defStyleAttr, defStyleRes);

        mShowBars = attributes.getBoolean(R.styleable.ActualNumberPicker_show_bars, true);
        mShowControls = attributes.getBoolean(R.styleable.ActualNumberPicker_show_controls, true);
        mShowFastControls = attributes.getBoolean(R.styleable.ActualNumberPicker_show_fast_controls, true);

        mDrawOverText = attributes.getBoolean(R.styleable.ActualNumberPicker_draw_over_text, false);
        mDrawOverControls = attributes.getBoolean(R.styleable.ActualNumberPicker_draw_over_controls, true);

        int barsColor = attributes.getColor(R.styleable.ActualNumberPicker_bar_color, Color.DKGRAY);
        mBarPaint = new Paint();
        mBarPaint.setAntiAlias(true);
        mBarPaint.setStyle(Paint.Style.FILL);
        mBarPaint.setColor(barsColor);

        mShowHighlight = attributes.getBoolean(R.styleable.ActualNumberPicker_show_highlight, true);
        int highlightColor = attributes.getColor(R.styleable.ActualNumberPicker_highlight_color, Color.LTGRAY);
        mHighlightPaint = new Paint();
        mHighlightPaint.setAntiAlias(true);
        mHighlightPaint.setStyle(Paint.Style.FILL);
        mHighlightPaint.setColor(highlightColor);
        mHighlightPaint.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL));
        mHighlightPaint.setAlpha(100);

        mSelectionColor = attributes.getColor(R.styleable.ActualNumberPicker_selection_color, 0xB0444444);

        int textColor = attributes.getColor(R.styleable.ActualNumberPicker_text_color, Color.DKGRAY);
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setLinearText(true);
        mTextPaint.setHinting(Paint.HINTING_ON);
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

        mValue = attributes.getFloat(R.styleable.ActualNumberPicker_value, mMinValue);
        if (mValue < mMinValue || mValue > mMaxValue) {
            throw new RuntimeException("Cannot use value " + mValue + " because it is out of range");
        }

        // You can't set an attribute to a double. So we grab the float and round it to the nearest
        float valueAdjustmentFloat = attributes.getFloat(R.styleable.ActualNumberPicker_value_adjustment, 1f);
        setValueAdjustment(valueAdjustmentFloat);

        mBarCount = attributes.getInteger(R.styleable.ActualNumberPicker_bars_count, DEFAULT_BAR_COUNT);
        if (mBarCount < 3) {
            mBarCount = DEFAULT_BAR_COUNT;
        }

        mMinBarWidth = context.getResources().getDimensionPixelSize(R.dimen.min_bar_width);
        mBarWidth = attributes.getDimensionPixelSize(R.styleable.ActualNumberPicker_bar_width, mMinBarWidth);
        if (mBarWidth < mMinBarWidth) {
            mBarWidth = mMinBarWidth;
        }

        mMinBarHeight = "large";
        if (attributes.hasValue(R.styleable.ActualNumberPicker_min_bar_height)) {
            mMinBarHeight = attributes.getString(R.styleable.ActualNumberPicker_min_bar_height);
        }
        if (!mMinBarHeight.equals("large") && !mMinBarHeight.equals("small")) {
            throw new RuntimeException("Cannot use value " + mMinBarHeight + ". Only 'small' and 'large' are accepted");
        }

        loadControlIcons(attributes, context);

        attributes.recycle();

        // update density and metrics/dimensions
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int density; // LDPI is 120
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getRealMetrics(metrics);
        density = metrics.densityDpi;
        mDensityFactor = density / DisplayMetrics.DENSITY_LOW; // will be 1, 1.2, 1.5... etc

        mControlsBacks.put(ARR_LEFT, createControlBackground());
        mControlsBacks.put(ARR_RIGHT, createControlBackground());
        mControlsBacks.put(FAST_ARR_LEFT, createControlBackground());
        mControlsBacks.put(FAST_ARR_RIGHT, createControlBackground());

        mMinHeight = context.getResources().getDimensionPixelSize(R.dimen.min_height);
        mMaxControlSize = context.getResources().getDimensionPixelSize(R.dimen.control_size);
        calculateControlPositions();
    }

    /**
     * Loads all resource icons (arrows) to the sparse array.
     *
     * @param attributes Which typed array to use to get the colors from
     * @param context Which context to use for resources
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void loadControlIcons(@NonNull TypedArray attributes, @NonNull Context context) {
        int controlsColor = attributes.getColor(R.styleable.ActualNumberPicker_controls_color, Color.DKGRAY);
        Drawable arrLeft = context.getResources().getDrawable(R.drawable.ic_keyboard_arrow_left_black_24dp);
        arrLeft = Coloring.get().colorDrawable(context, arrLeft, controlsColor);
        mControlIcons.put(ARR_LEFT, arrLeft);

        Drawable arrRight = context.getResources().getDrawable(R.drawable.ic_keyboard_arrow_right_black_24dp);
        arrRight = Coloring.get().colorDrawable(context, arrRight, controlsColor);
        mControlIcons.put(ARR_RIGHT, arrRight);

        int fastControlsColor = attributes.getColor(R.styleable.ActualNumberPicker_fast_controls_color, Color.DKGRAY);

        Drawable fastArrLeft = context.getResources().getDrawable(R.drawable.ic_keyboard_2arrows_left_black_24dp);
        fastArrLeft = Coloring.get().colorDrawable(context, fastArrLeft, fastControlsColor);
        mControlIcons.put(FAST_ARR_LEFT, fastArrLeft);

        // noinspection deprecation
        Drawable fastArrRight = context.getResources().getDrawable(R.drawable.ic_keyboard_2arrows_right_black_24dp);
        fastArrRight = Coloring.get().colorDrawable(context, fastArrRight, fastControlsColor);
        mControlIcons.put(FAST_ARR_RIGHT, fastArrRight);
    }

    /**
     * Sets the {@link OnValueChangeListener} to this number picker.
     *
     * @param listener Which listener to set
     */
    public void setListener(OnValueChangeListener listener) {
        mListener = listener;
    }

    /**
     * @return Maximum number allowed on this picker
     */
    public int getMaxValue() {
        return mMaxValue;
    }

    /**
     * @return Current number value on this picker
     */
    public double getValue() {
        return mValue;
    }

    /**
     * @return Minimum number allowed on this picker
     */
    public int getMinValue() {
        return mMinValue;
    }

    /**
     * Sets the minimum value to this number picker.
     *
     * @param minValue Minimum value to display by the number picker
     */
    public void setMinValue(int minValue) {
        if (mMaxValue <= minValue) {
            throw new RuntimeException("Cannot use min_value " + minValue + " because the max_value is " + mMaxValue);
        }
        this.mMinValue = minValue;

        if (mMinValue > mValue) {
            setValue((mMaxValue + mMinValue) / 2);
        }
    }

    /**
     * Sets the maximum value to this number picker.
     *
     * @param maxValue Maximum value to display by the number picker
     */
    public void setMaxValue(int maxValue) {
        if (maxValue <= mMinValue) {
            throw new RuntimeException("Cannot use max_value " + maxValue + " because the min_value is " + mMinValue);
        }
        this.mMaxValue = maxValue;

        if (mMaxValue < mValue) {
            setValue((mMaxValue + mMinValue) / 2);
        }
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
        mMaxControlSize = Math.min(mHeight, mMaxControlSize);

        // MUST CALL THIS
        setMeasuredDimension(mWidth, mHeight);
        updateTextSize();
        calculateControlPositions();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        mHeight = Math.max(h, mHeight);
        mWidth = calculateWidth(w, MeasureSpec.EXACTLY, mHeight);
        mMaxControlSize = Math.min(mHeight, mMaxControlSize);
        updateTextSize();
        calculateControlPositions();
        super.onSizeChanged(mWidth, mHeight, oldW, oldH);
    }

    private Drawable createControlBackground() {
        Drawable back = Coloring.get().createBackgroundDrawable(Color.TRANSPARENT, mSelectionColor, mSelectionColor, true);
        back.setCallback(this);
        return back;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        boolean isControlBackground = false;
        for (int i = 0; i < mControlsBacks.size(); i++) {
            if (mControlsBacks.get(mControlsBacks.keyAt(i)) == who) {
                isControlBackground = true;
                break;
            }
        }
        return super.verifyDrawable(who) || isControlBackground;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        for (int i = 0; i < mControlsBacks.size(); i++) {
            mControlsBacks.get(mControlsBacks.keyAt(i)).jumpToCurrentState();
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        for (int i = 0; i < mControlsBacks.size(); i++) {
            mControlsBacks.get(mControlsBacks.keyAt(i)).setHotspot(x, y);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mSelectedControl != CONTROL_NONE) {
            mControlsBacks.get(mSelectedControl).setState(getDrawableState());

            mHandler.removeCallbacks(mInvalidator);
            mHandler.post(mInvalidator);
        }
    }

    private void calculateControlPositions() {
        // load icon dimensions
        int leftArrW = mControlIcons.get(ARR_LEFT).getMinimumWidth();
        int leftArrH = mControlIcons.get(ARR_LEFT).getMinimumHeight();
        int rightArrW = mControlIcons.get(ARR_RIGHT).getMinimumWidth();
        int rightArrH = mControlIcons.get(ARR_RIGHT).getMinimumHeight();
        int fastLeftArrW = mControlIcons.get(FAST_ARR_LEFT).getMinimumWidth();
        int fastLeftArrH = mControlIcons.get(FAST_ARR_LEFT).getMinimumHeight();
        int fastRightArrW = mControlIcons.get(FAST_ARR_RIGHT).getMinimumWidth();
        int fastRightArrH = mControlIcons.get(FAST_ARR_RIGHT).getMinimumHeight();

        int maxSelectionRadius = mHeight > mMinHeight ? mMaxControlSize : mMinHeight;

        // ***** left arrow selection ripple *****
        int leftCX = mWidth / 2 - maxSelectionRadius;
        int leftCY = mHeight / 2;
        mControlsBacks.get(ARR_LEFT).setBounds(leftCX - maxSelectionRadius / 2, leftCY - maxSelectionRadius / 2,
                leftCX + maxSelectionRadius / 2, leftCY + maxSelectionRadius / 2);
        // left arrow drawable bounds
        mControlIcons.get(ARR_LEFT).setBounds(leftCX - leftArrW / 2, leftCY - leftArrH / 2, leftCX + leftArrW / 2, leftCY + leftArrH / 2);

        // ***** right arrow selection ripple *****
        int rightCX = mWidth / 2 + maxSelectionRadius;
        int rightCY = mHeight / 2;
        mControlsBacks.get(ARR_RIGHT).setBounds(rightCX - maxSelectionRadius / 2, rightCY - maxSelectionRadius / 2,
                rightCX + maxSelectionRadius / 2, rightCY + maxSelectionRadius / 2);
        // right arrow drawable bounds
        mControlIcons.get(ARR_RIGHT).setBounds(rightCX - rightArrW / 2, rightCY - rightArrH / 2, rightCX + rightArrW / 2,
                rightCY + rightArrH / 2);

        // ***** left fast arrow selection ripple *****
        int fastLeftCX;
        if (mShowControls) {
            fastLeftCX = mWidth / 2 - maxSelectionRadius * 2;
        } else {
            fastLeftCX = leftCX;
        }
        int fastLeftCY = mHeight / 2;
        mControlsBacks.get(FAST_ARR_LEFT).setBounds(fastLeftCX - maxSelectionRadius / 2, fastLeftCY - maxSelectionRadius / 2,
                fastLeftCX + maxSelectionRadius / 2, fastLeftCY + maxSelectionRadius / 2);
        // left fast arrow drawable bounds
        mControlIcons.get(FAST_ARR_LEFT).setBounds(fastLeftCX - fastLeftArrW / 2, fastLeftCY - fastLeftArrH / 2,
                fastLeftCX + fastLeftArrW / 2, fastLeftCY + fastLeftArrH / 2);

        // ***** right fast arrow selection ripple *****
        int fastRightCX;
        if (mShowControls) {
            fastRightCX = mWidth / 2 + maxSelectionRadius * 2;
        } else {
            fastRightCX = rightCX;
        }
        int fastRightCY = mHeight / 2;
        mControlsBacks.get(FAST_ARR_RIGHT).setBounds(fastRightCX - maxSelectionRadius / 2, fastRightCY - maxSelectionRadius / 2,
                fastRightCX + maxSelectionRadius / 2, fastRightCY + maxSelectionRadius / 2);
        // right fast arrow drawable bounds
        mControlIcons.get(FAST_ARR_RIGHT).setBounds(fastRightCX - fastRightArrW / 2, fastRightCY - fastRightArrH / 2,
                fastRightCX + fastRightArrW / 2, fastRightCY + fastRightArrH / 2);
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

    /**
     * Restores the {@code mValue} into the [{@code mMinValue}, {@code mMaxValue}] range if necessary.
     */
    private void normalizeValue() {
        if (mValue < mMinValue) {
            mValue = mMinValue;
        } else if (mValue > mMaxValue) {
            mValue = mMaxValue;
        }
    }

    /**
     * Forces a new value onto the view. This will notify the listener and move the wheel to its starting position.<br>
     * <b>Note</b>: The value must be between {@link #mMinValue} and {@link #mMaxValue}.
     *
     * @param newValue Which value to set
     */
    public void setValue(double newValue) {
        double oldValue = mValue;
        mDelta = 0;
        mValue = newValue;
        mLastX = Float.MAX_VALUE;
        normalizeValue();
        if (oldValue != mValue) {
            notifyListener(oldValue, mValue);
        }
    }

    public void setValueAdjustment(float newValueAdjustment) {
        if (newValueAdjustment < 0 || newValueAdjustment > 1) {
            throw new RuntimeException("Cannot use value " + mValueAdjustment + " because it is must be between 0 and 1");
        }
        mValueAdjustment = Math.round(newValueAdjustment * 100.0) / 100.0;
        if ((1.0 * 100) % (mValueAdjustment * 100) != 0) {
            throw new RuntimeException("Cannot use value " + mValueAdjustment + " because there must be no remainder when dividing 1 by the value adjustment");
        }
    }

    /**
     * Invoked by the view when some of the controls are clicked (touched with ACTION_DOWN and ACTION_UP).
     *
     * @param which The control constant, any of the {@link Control}s
     */
    private void onControlClicked(@Control int which) {
        double oldValue = mValue;
        int changeX = 0;

        switch (which) {
            case ARR_LEFT: {
                mValue = BigDecimal.valueOf(mValue).subtract(BigDecimal.valueOf(mValueAdjustment)).doubleValue();
                changeX = -mBarWidth;
                break;
            }
            case ARR_RIGHT: {
                mValue = BigDecimal.valueOf(mValue).add(BigDecimal.valueOf(mValueAdjustment)).doubleValue();
                changeX = +mBarWidth;
                break;
            }
            case FAST_ARR_LEFT: {
                int valueChange = (mMaxValue - mMinValue) / 10;
                mValue -= valueChange;
                changeX = (int) -(0.1f * mWidth);
                break;
            }
            case FAST_ARR_RIGHT: {
                int valueChange = (mMaxValue - mMinValue) / 10;
                mValue += valueChange;
                changeX = (int) (0.1f * mWidth);
                break;
            }
        }

        normalizeValue();
        if (oldValue != mValue) {
            float x = mLastX + changeX;
            float thisDelta = mLastX - x;
            mLastX = x;
            // 'minus' because we want to go in the opposite direction
            mDelta -= thisDelta / (mDensityFactor / 2f);
            notifyListener(oldValue, mValue);
        }
    }

    /**
     * Calls {@link OnValueChangeListener#onValueChanged(double, double)}, but posts it to the main looper.
     */
    private void notifyListener(final double oldValue, final double newValue) {
        mHandler.post(() -> {
            if (mListener != null) {
                mListener.onValueChanged(oldValue, newValue);
            }
        });
    }

    /**
     * Checks whether the given [x, y] point fits into the hit point rectangle for any of the controls.
     *
     * @param x Where is the finger on the X-axis
     * @param y Where is the finger on the Y-axis
     * @return Identifier of the control if the pointer is 'touching' it, or {@link #CONTROL_NONE} if not
     */
    @Control
    private int isTouchingControls(float x, float y) {
        if (mShowControls) {
            if (mControlsBacks.get(ARR_LEFT).getBounds().contains((int) x, (int) y)) {
                return ARR_LEFT;
            } else if (mControlsBacks.get(ARR_RIGHT).getBounds().contains((int) x, (int) y)) {
                return ARR_RIGHT;
            }
        }

        if (mShowFastControls) {
            if (mControlsBacks.get(FAST_ARR_LEFT).getBounds().contains((int) x, (int) y)) {
                return FAST_ARR_LEFT;
            } else if (mControlsBacks.get(FAST_ARR_RIGHT).getBounds().contains((int) x, (int) y)) {
                return FAST_ARR_RIGHT;
            }
        }

        return CONTROL_NONE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int selectedControl = isTouchingControls(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                getParent().requestDisallowInterceptTouchEvent(true);
                mLastX = event.getX();

                mSelectedControl = selectedControl;
                if (mSelectedControl != CONTROL_NONE) {
                    mControlsBacks.get(selectedControl).setHotspot(event.getX(), event.getY());

                    mHandler.post(mInvalidator);
                }
                setPressed(true); // required to draw drawable transitions properly
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                Drawable current = mControlsBacks.get(selectedControl);
                if (current != null) { // happens only when [mSelectedControl != CONTROL_NONE] but couldn't find out why
                    current.setHotspot(event.getX(), event.getY());
                }

                if (mSelectedControl == CONTROL_NONE) {
                    float percent = event.getX() / (float) mWidth;
                    double oldValue = mValue;
                    mValue = (float) Math.floor(percent * (mMaxValue - mMinValue)) + mMinValue;
                    normalizeValue();

                    if (mValue != oldValue) {
                        float thisDelta = mLastX - event.getX();
                        mLastX = event.getX();
                        // 'minus' because we want to go in the opposite direction
                        mDelta -= thisDelta / (mDensityFactor / 2f);
                        notifyListener(oldValue, mValue);
                    }
                }

                mHandler.removeCallbacks(mInvalidator);
                mHandler.post(mInvalidator);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                getParent().requestDisallowInterceptTouchEvent(false);
                setPressed(false);

                Drawable current = mControlsBacks.get(selectedControl);
                if (current != null) { // happens only when [mSelectedControl != CONTROL_NONE] but couldn't find out why
                    current.setState(STATE_NORMAL);
                }
                if (mSelectedControl != CONTROL_NONE) {
                    onControlClicked(mSelectedControl);
                }
                mSelectedControl = CONTROL_NONE;

                mLastX = Float.MAX_VALUE;
                mHandler.post(mInvalidator);
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
    private float linear(float t, float b, float c, float d) {
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
    private float easeIn(float t, float b, float c, float d) {
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
    private float easeOut(float t, float b, float c, float d) {
        return c * (float) Math.sin(t / d * (Math.PI / 2)) + b;
    }

    /**
     * Penner's sine easing in and out function, plotted by time and distance for a motion tween. Can be used for density, width and other
     * properties that should behave the same.
     *
     * @param t The current time (or position) of the tween. This can be seconds or frames, steps, ms, whatever – as long as the unit is the
     *            same as is used for the total time
     * @param b The beginning value of the property
     * @param c The change between the beginning and destination value of the property
     * @param d The total time of the tween
     * @return The new value that has resulted from the equation
     */
    private double easeInOut(float t, float b, float c, float d) {
        return -c / 2 * (Math.cos(Math.PI * t / d) - 1) + b;
    }

    /**
     * Repositions the X coordinate back inside the {@code [0-containerW]} range. If X gets bigger than {@code containerW} then it is
     * repositioned to the left, symmetrically to the (X:{@code containerW / 2}) line. Analogously, if X gets smaller than {@code 0} then it
     * is repositioned to the right, symmetrically to the (X:{@code containerW / 2}) line.
     *
     * @param linearBarX Where is the X coordinate now (prior to reposition)
     * @param containerW How wide is the container
     * @return The repositioned X value, which will be inside the {@code [0-containerW]} range
     */
    public float repositionInside(float linearBarX, int containerW) {
        if (linearBarX < 0) {
            return containerW - (-linearBarX % containerW);
        } else {
            return linearBarX % containerW;
        }
    }

    /**
     * Scales the number by the given factor.
     *
     * @param what Number to scale up or down
     * @param factor Scaling factor (must be a positive integer)
     * @return The scaled value, <b>{@code what}</b> x <b>{@code factor}</b>
     */
    private int scale(int what, @FloatRange(from = 0) double factor) {
        return (int) (Math.floor((double) what * factor));
    }

    /**
     * Checks whether the text overlaps the bar that is about to be drawn.<br>
     * <b>Note</b>: This method fakes the text width, i.e. increases it to allow for some horizontal padding.
     *
     * @param textBounds Which text bounds to check
     * @param barBounds Which bar bounds to check
     * @return {@code True} if bounds overlap each other, {@code false} if not
     */
    private boolean textOverlapsBar(Rect textBounds, RectF barBounds) {
        if (!mShowText || mDrawOverText) {
            // no text, no overlapping; draw over text, no overlapping
            return false;
        }

        // increase original text width to give some padding to the text
        double factor = 0.6d;
        int textL = textBounds.left - scale(textBounds.width(), factor);
        int textT = textBounds.top;
        int textR = textBounds.right + scale(textBounds.width(), factor);
        int textB = textBounds.bottom;
        return barBounds.intersects(textL, textT, textR, textB);
    }

    /**
     * Checks whether the controls overlap the bar that is about to be drawn (icons only are measured).<br>
     * <b>Note</b>: This method fakes the icon size, i.e. reduces the bounds to make more bars show up.
     *
     * @param controlIcons Which set of {@link Drawable}s to check
     * @param barBounds Which bar bounds to check
     * @return {@code True} if bar bounds overlap any of the icon bounds, {@code false} if not
     */
    private boolean controlsOverlapBar(SparseArray<Drawable> controlIcons, RectF barBounds) {
        if (mDrawOverControls) {
            return false;
        }

        Rect icon;
        double scaleFactor = 0.2d;

        if (mShowControls) {
            icon = controlIcons.get(ARR_LEFT).getBounds();
            if (iconBoundsIntersectBar(barBounds, icon, scaleFactor)) {
                return true;
            }

            icon = controlIcons.get(ARR_RIGHT).getBounds();
            if (iconBoundsIntersectBar(barBounds, icon, scaleFactor)) {
                return true;
            }
        }

        if (mShowFastControls) {
            icon = controlIcons.get(FAST_ARR_LEFT).getBounds();
            if (iconBoundsIntersectBar(barBounds, icon, scaleFactor)) {
                return true;
            }

            icon = controlIcons.get(FAST_ARR_RIGHT).getBounds();
            if (iconBoundsIntersectBar(barBounds, icon, scaleFactor)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper method for calculating intersections for control icons and bars.
     *
     * @see #controlsOverlapBar(SparseArray, RectF)
     */
    private boolean iconBoundsIntersectBar(RectF barBounds, Rect icon, double scaleFactor) {
        int iconL = icon.left + scale(icon.width(), scaleFactor);
        int iconT = icon.top + scale(icon.height(), scaleFactor);
        int iconR = icon.right - scale(icon.width(), scaleFactor);
        int iconB = icon.bottom - scale(icon.height(), scaleFactor);
        return barBounds.intersects(iconL, iconT, iconR, iconB);
    }

    /**
     * Calculates how high the bar needs to be for the given X coordinate. Higher ones appear near the middle, i.e. when X is near the 1/2
     * of the container width
     *
     * @param minHeight Minimum allowed height of the bar
     * @param maxHeight Maximum allowed height of the bar
     * @param barX Where is the bar located on the X-axis
     * @param containerWidth How wide is the view container
     * @return Correct, scaled height of the given bar (determined by the index parameter)
     */
    private int calculateBarHeight(@IntRange(from = 0) int minHeight, @IntRange(from = 0) int maxHeight, float barX, int containerWidth) {
        float height;
        if (barX <= containerWidth / 2) {
            height = easeOut(barX, minHeight, maxHeight - minHeight, containerWidth / 2f);
        } else {
            height = easeIn(barX - containerWidth / 2f, maxHeight, minHeight - maxHeight, containerWidth / 2f);
        }
        return (int) Math.floor(height);
    }

    /**
     * Calculates how transparent the bar needs to be for the given X coordinate. More opaque ones appear near the middle, i.e. when X is
     * near the 1/2 of the container width.
     *
     * @param minOpacity Minimum allowed opacity of the bar (must be between 0 and 255)
     * @param maxOpacity Maximum allowed opacity of the bar (must be between 0 and 255)
     * @param barX Where is the bar located on the X-axis
     * @param containerWidth How wide is the view container
     * @return Correct, scaled height of the given bar (determined by the index parameter)
     */
    private int calculateBarOpacity(@IntRange(from = 0, to = 255) int minOpacity, @IntRange(from = 0, to = 255) int maxOpacity, float barX,
            int containerWidth) {
        float height;
        if (barX <= containerWidth / 2) {
            height = easeOut(barX, minOpacity, maxOpacity - minOpacity, containerWidth / 2f);
        } else {
            height = easeIn(barX - containerWidth / 2f, maxOpacity, minOpacity - maxOpacity, containerWidth / 2f);
        }
        return (int) Math.floor(height);
    }

    /**
     * A periodic updater for animations. This should be kept clean, as it forces a call to the {@link #onDraw(Canvas)} method.
     */
    private final Runnable mInvalidator = this::invalidate;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mShowText) {
            // Show the decimal place only if the user wants decimal value adjustments
            String value;
            if (mValueAdjustment == 1) {
                value = String.valueOf((int) Math.floor(mValue));
            } else {
                value = String.valueOf(mValue);
            }

            // this will save dimensions to mTextDimens
            measureText(value);
            int x = mWidth / 2 - mTextDimens.x / 2;
            int y = mHeight / 2 + mTextDimens.y / 2;
            canvas.drawText(value, x, y, mTextPaint);

            // update bounds to re-use later
            mTextBounds.set(x, y, x + mTextBounds.width(), y + mTextBounds.height());
        }

        if (mShowBars) {
            // draw all bars, but draw one more in the end with '<=' instead of '<' (to be symmetric)
            int opacity, barH;
            float linearX, insideX, x, y;
            int maxBarH = (int) Math.floor(0.5f * mHeight);
            float minBarHeightRatio = (mMinBarHeight.equals("large") ? 0.95f : 0.55f);
            int minBarH = (int) Math.floor(maxBarH * minBarHeightRatio);
            int minOpacity = 50;
            for (int i = 0; i <= mBarCount; i++) {
                // calculate bar X coordinate
                linearX = mDelta + (float) i / (float) mBarCount * (float) mWidth;
                insideX = repositionInside(linearX, mWidth);
                x = (float) Math.floor(easeInOut(insideX, 0f, 1f, mWidth) * mWidth);
                // calculate bar height
                barH = calculateBarHeight(minBarH, maxBarH, x, mWidth);
                // calculate Y coordinate
                y = mHeight / 2 - barH / 2;
                // don't draw if it overlaps the text
                mBarBounds.set(x - mBarWidth / 2f, y, x + mBarWidth, y + barH);
                if (!textOverlapsBar(mTextBounds, mBarBounds) && !controlsOverlapBar(mControlIcons, mBarBounds)) {
                    opacity = calculateBarOpacity(minOpacity, 255, x, mWidth);
                    mBarPaint.setAlpha(opacity);
                    canvas.drawRoundRect(mBarBounds, mBarBounds.width() / 3f, mBarBounds.width() / 3f, mBarPaint);
                }
            }
        }

        if (mShowControls) {
            mControlsBacks.get(ARR_LEFT).draw(canvas);
            mControlsBacks.get(ARR_RIGHT).draw(canvas);

            if (mShowHighlight) {
                int radius = mControlIcons.get(ARR_LEFT).getBounds().width() / 2;
                int leftCX = mControlIcons.get(ARR_LEFT).getBounds().centerX();
                int leftCY = mControlIcons.get(ARR_LEFT).getBounds().centerY();
                canvas.drawCircle(leftCX, leftCY, radius, mHighlightPaint);

                int rightCX = mControlIcons.get(ARR_RIGHT).getBounds().centerX();
                int rightCY = mControlIcons.get(ARR_RIGHT).getBounds().centerY();
                canvas.drawCircle(rightCX, rightCY, radius, mHighlightPaint);
            }

            mControlIcons.get(ARR_LEFT).draw(canvas);
            mControlIcons.get(ARR_RIGHT).draw(canvas);
        }

        if (mShowFastControls) {
            mControlsBacks.get(FAST_ARR_LEFT).draw(canvas);
            mControlsBacks.get(FAST_ARR_RIGHT).draw(canvas);

            if (mShowHighlight) {
                int radius = mControlIcons.get(FAST_ARR_LEFT).getBounds().width() / 2;
                int leftCX = mControlIcons.get(FAST_ARR_LEFT).getBounds().centerX();
                int leftCY = mControlIcons.get(FAST_ARR_LEFT).getBounds().centerY();
                canvas.drawCircle(leftCX, leftCY, radius, mHighlightPaint);

                int rightCX = mControlIcons.get(FAST_ARR_RIGHT).getBounds().centerX();
                int rightCY = mControlIcons.get(FAST_ARR_RIGHT).getBounds().centerY();
                canvas.drawCircle(rightCX, rightCY, radius, mHighlightPaint);
            }

            mControlIcons.get(FAST_ARR_LEFT).draw(canvas);
            mControlIcons.get(FAST_ARR_RIGHT).draw(canvas);
        }

    }

}
