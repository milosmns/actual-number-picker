
package me.angrybyte.numberpicker.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.NumberPicker;

import me.angrybyte.numberpicker.view.ActualNumberPicker;

public class DemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        ActualNumberPicker picker = (ActualNumberPicker) findViewById(R.id.picker);
        picker.setRange(1, 100);

        NumberPicker oldPicker = (NumberPicker) findViewById(R.id.picker_old);
        oldPicker.setMinValue(0);
        oldPicker.setMaxValue(100);
    }

}
