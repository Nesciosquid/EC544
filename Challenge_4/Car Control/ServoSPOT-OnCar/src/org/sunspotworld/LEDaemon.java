/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld;

import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;

/**
 *
 * @author Aaron Heuckroth
 */
public class LEDaemon {

    boolean reverse_LEDs = false;
    private ITriColorLEDArray LEDs;
    private double confidenceHighCutoff = .60;
    private double confidenceCutoffStep = .15;
    int ledIndex = 0;
    int leftLED = 0;
    int rightLED = 0;
    int leftRED = 0;
    int leftGREEN = 0;
    int leftBLUE = 0;
    int rightRED = 0;
    int rightGREEN = 0;
    int rightBLUE = 0;
    double maxAngle = 45;

    public LEDaemon(ITriColorLEDArray new_leds) {
        LEDs = new_leds;
    }

    public LEDaemon(ITriColorLEDArray new_leds, boolean reverse) {
        LEDs = new_leds;
        reverse_LEDs = reverse;
    }

    public LEDaemon(ITriColorLEDArray new_leds, boolean reverse, double c_max, double c_step, double max_angle) {
        LEDs = new_leds;
        confidenceHighCutoff = c_max;
        confidenceCutoffStep = c_step;
        reverse_LEDs = reverse;
        maxAngle = max_angle;
    }

    private int calculateIndex(double theta_rad, boolean reverse) {
        double theta = theta_rad * 57.2957795;
        double thetaRatio;
        double maxTheta = maxAngle + .01;
        int index;
        int thetaOffset;
        if (theta > maxTheta) {
            theta = maxTheta;
        } else if (theta < -(maxTheta)) {
            theta = -(maxTheta);
        }
        thetaRatio = theta / maxTheta;
        thetaOffset = (int) (thetaRatio * (LEDs.size() / 2));
        if (thetaRatio >= 0.0) {
            index = (4 + thetaOffset);
        } else {
            index = (3 + thetaOffset);
        }
        if (reverse) {
            return invertLEDIndex(index);
        } else {
            return index;
        }
    }

    private int invertLEDIndex(int LED_index) { // 0 - 7, 1 - 6,
        if (LED_index == 7) {
            return 0;
        } else {
            return (-(LED_index - 7));
        }
    }

    private void setLED_RED(int value, ITriColorLED led) {
        int red = value;
        int green = led.getGreen();
        int blue = led.getBlue();
        led.setRGB(red, green, blue);
    }

    private void setLED_BLUE(int value, ITriColorLED led) {
        int red = led.getRed();
        int green = led.getGreen();
        int blue = value;
        led.setRGB(red, green, blue);
    }

    private void setLED_GREEN(int value, ITriColorLED led) {
        int red = led.getRed();
        int green = value;
        int blue = led.getBlue();
        led.setRGB(red, green, blue);
    }
    
    public void setAllOff(){
        for (int i = 0; i < LEDs.size(); i++){
            LEDs.getLED(i).setOff();
        }
    }

    private int calcColor(double conf1, double conf2, int offset) {
        double minConfidence;
        double confSquared = conf1 * conf2;
        int intensity;
        if (conf1 < conf2) {
            minConfidence = conf1;
        } else {
            minConfidence = conf2;
        }

        if (minConfidence >= confidenceHighCutoff) {
            intensity = 255 - (int) (offset * confSquared * 256.0);
        } else {
            intensity = (int) (256.0 * confSquared); // >= .60
        }
        if (minConfidence < confidenceHighCutoff - confidenceCutoffStep * (offset)) { // (< .60)
            intensity = intensity / 2;
        }
        if (minConfidence < confidenceHighCutoff - confidenceCutoffStep * (offset + 1)) { //(< .45)
            intensity = intensity / 2;
        }
        if (minConfidence < confidenceHighCutoff - confidenceCutoffStep * (offset + 2)) { //(< .30)
            intensity = 0;
        }
        if (intensity > 255) {
            intensity = 255;
        } else if (intensity < 0) {
            intensity = 0;
        }
        return intensity;

    }

    public void changeColors(double thetaRight, double thetaLeft, double confidenceRF, double confidenceLF, double confidenceRR, double confidenceLR) {
        leftLED = calculateIndex(thetaLeft, reverse_LEDs);
        rightLED = calculateIndex(thetaRight, reverse_LEDs);
        leftRED = calcColor(confidenceLF, confidenceLR, 1);
        leftGREEN = calcColor(confidenceLF, confidenceLR, 0);
        leftBLUE = 0;
        rightBLUE = calcColor(confidenceRF, confidenceRR, 1);
        rightGREEN = calcColor(confidenceRF, confidenceRR, 0);
        rightRED = 0;

        updateLEDs();
    }

    private void updateLEDs() {
        ITriColorLED currentLED;

        for (int i = 0; i < LEDs.size(); i++) {

            int newRed;
            int newGreen;
            int newBlue;
            currentLED = LEDs.getLED(i);

            if (i == leftLED && i == rightLED) {
                newRed = (leftRED);
                if (leftGREEN > rightGREEN) {
                    newGreen = leftGREEN;
                } else {
                    newGreen = rightGREEN;
                }
                newBlue = (leftBLUE);
            } else if (i == leftLED) {
                newRed = leftRED;
                newGreen = leftGREEN;
                newBlue = leftBLUE;
            } else if (i == rightLED) {
                newRed = rightRED;
                newGreen = rightGREEN;
                newBlue = rightBLUE;
            } else {
                newRed = 0;
                newGreen = 0;
                newBlue = 0;
            }
            currentLED.setRGB(newRed, newGreen, newBlue);
            if ((newRed + newGreen + newBlue) == 0 && currentLED.isOn()) {
                currentLED.setOff();
            } else {
                if (!currentLED.isOn()) {
                    currentLED.setOn();
                }
            }

        }



    }
}
