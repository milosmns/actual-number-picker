
package me.angrybyte.numberpicker.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ViewGroup;

import me.angrybyte.numberpicker.listener.OnValueChangeListener;
import me.angrybyte.numberpicker.view.ActualNumberPicker;

public class DemoActivity extends AppCompatActivity implements OnValueChangeListener {

    private ActualNumberPicker mTestPicker;
    private ViewGroup mContentRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        mTestPicker = (ActualNumberPicker) findViewById(R.id.actual_picker2);
        mContentRoot = (ViewGroup) findViewById(android.R.id.content);
        mTestPicker.setListener(this);
        enableStrictMode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableStrictMode();
    }

    private void enableStrictMode() {
        // @formatter:off
        StrictMode.setThreadPolicy(
            new StrictMode.ThreadPolicy.
                Builder().
                detectAll().
                penaltyLog().
                build());
        StrictMode.setVmPolicy(
            new StrictMode.VmPolicy.
                Builder().
                detectAll().
                penaltyLog().
                penaltyDeath().
                build());
        // @formatter:on
    }

    private void disableStrictMode() {
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX);
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
    }

    private void updateBackgroundColor(float percent) {
        int[] colors = new int[] {
                Color.WHITE, Color.GRAY, Color.DKGRAY
        };
        int color = colors[((int) Math.floor(percent * (colors.length - 1)))];

        mContentRoot.setBackgroundColor(color);
        mContentRoot.getChildAt(0).setBackgroundColor(color);
    }

    @Override
    public void onValueChanged(double oldValue, double newValue) {
        float percent = (float) newValue / (float) (mTestPicker.getMaxValue() - mTestPicker.getMinValue());
        updateBackgroundColor(percent);
    }

}
