
package me.angrybyte.numberpicker.demo;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;

public class DemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
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

}
