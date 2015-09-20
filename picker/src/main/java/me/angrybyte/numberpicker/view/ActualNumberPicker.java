
package me.angrybyte.numberpicker.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import me.angrybyte.numberpicker.R;

public class ActualNumberPicker extends View {

    private static final String TAG = ActualNumberPicker.class.getSimpleName();
    private static final int ARR_LEFT = 0xC1;
    private static final int ARR_RIGHT = 0xC2;
    private static final int FAST_ARR_LEFT = 0xF1;
    private static final int FAST_ARR_RIGHT = 0xF2;
    private static final int CONTROL_TEXT = 0x00;

    private Paint mBarsPaint;
    private Paint mControlsPaint;
    private Paint mFastControlsPaint;
    private TextPaint mTextPaint;

    private int mMinHeight;
    private int mMinBarWidth;
    private int mWidth;
    private int mHeight;

    private boolean mShowBars;
    private boolean mShowControls;
    private boolean mShowFastControls;

    private int mSelectedControl; // one of the constants on top

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
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mTextPaint.setHinting(Paint.HINTING_ON);
        }
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(textColor);

        // call this when you're done with the attributes
        attributes.recycle();

        mMinBarWidth = context.getResources().getDimensionPixelSize(R.dimen.min_bar_width);
        mMinHeight = context.getResources().getDimensionPixelSize(R.dimen.min_height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int height;

        if (heightMode == MeasureSpec.EXACTLY) {
            // respect min_height value
            height = Math.max(mMinHeight, heightSize);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            // whichever is smaller
            height = Math.min(mMinHeight, heightSize);
        } else {
            // doesn't matter
            height = Math.max(mMinHeight, heightSize);
        }

        mHeight = height;
        mWidth = mHeight * 5; // fast_controls x2, controls x2, text

        // MUST CALL THIS
        setMeasuredDimension(mWidth, mHeight);

        updateTextSize();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = Math.max(mMinHeight, h);
        mWidth = mHeight * 5; // fast_controls x2, controls x2, text
        updateTextSize();
    }

    private void updateTextSize() {
        float size = 14f; // 14px on LDPI
        Rect bounds = new Rect(0, 0, 0, 0);
        mTextPaint.setTextSize(size);
        mTextPaint.getTextBounds("00", 0, 1, bounds);

        // this loop exits when text size becomes too big
        while (bounds.height() < mHeight - mHeight * 0.2f) {
            size += 0.5f;
            mTextPaint.setTextSize(size);
            mTextPaint.getTextBounds("00", 0, 1, bounds);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText("w=" + mWidth + ",h=" + mHeight, mWidth * 0.5f, mHeight * 0.7f, mTextPaint);
    }

}
