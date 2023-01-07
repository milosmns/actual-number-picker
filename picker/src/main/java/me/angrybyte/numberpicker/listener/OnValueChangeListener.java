
package me.angrybyte.numberpicker.listener;

import me.angrybyte.numberpicker.view.ActualNumberPicker;

/**
 * A listener for {@link ActualNumberPicker} View. Gives information about changes in the value (always between
 * {@link ActualNumberPicker#mMinValue} and {@link ActualNumberPicker#mMaxValue}.
 */
public interface OnValueChangeListener {

    /**
     * Invoked when the value changes on the {@link ActualNumberPicker}. Remember to set this listener to the View.
     *
     * @param oldValue The value picker had before the change
     * @param newValue Tha value picker has now, after the change
     */
    void onValueChanged(double oldValue, double newValue);

}
