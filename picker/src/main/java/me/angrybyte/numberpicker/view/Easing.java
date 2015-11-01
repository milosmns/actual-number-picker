
package me.angrybyte.numberpicker.view;

/**
 * A simple helper class used for storing Penner's easing functions.
 */
public class Easing {

    /**
     * Penner's ease in function. Gives values between {@code b} and {@code b + c} during iterations.<br>
     * Gives more values towards the start of {@code t} over {@code d} interval.
     *
     * @param t How far are you in the iteration process in comparison to {@code d} (may be in seconds, iterations or other)
     * @param b The beginning value of the metric you are easing
     * @param c Change in the value for the metric you are easing
     * @param d How many iterations are there (may be in seconds, iterations or other)
     * @return A calculated value between {@code b} and {@code b + c} for the given interation {@code t} over {@code d}
     */
    public static double easeIn(double t, double b, double c, double d) {
        return -c * Math.cos(t / d * (Math.PI / 2)) + c + b;
    }

    /**
     * Penner's ease out function. Gives values between {@code b} and {@code b + c} during iterations.<br>
     * Gives more values towards the end of {@code t} over {@code d} interval.
     *
     * @param t How far are you in the iteration process in comparison to {@code d} (may be in seconds, iterations or other)
     * @param b The beginning value of the metric you are easing
     * @param c Change in the value for the metric you are easing
     * @param d How many iterations are there (may be in seconds, iterations or other)
     * @return A calculated value between {@code b} and {@code b + c} for the given interation {@code t} over {@code d}
     */
    public static double easeOut(double t, double b, double c, double d) {
        return c * Math.sin(t / d * (Math.PI / 2)) + b;
    }

    /**
     * Penner's ease in & ease out function. Gives values between {@code b} and {@code b + c} during iterations.<br>
     * Gives more values towards the beginning and end of {@code t} to {@code d} interval.
     *
     * @param t How far are you in the iteration process in comparison to {@code d} (may be in seconds, iterations or other)
     * @param b The beginning value of the metric you are easing
     * @param c Change in the value for the metric you are easing
     * @param d How many iterations are there (may be in seconds, iterations or other)
     * @return A calculated value between {@code b} and {@code b + c} for the given interation {@code t} over {@code d}
     */
    public static double easeInOut(double t, double b, double c, double d) {
        return -c / 2 * (Math.cos(Math.PI * t / d) - 1) + b;
    }

}
