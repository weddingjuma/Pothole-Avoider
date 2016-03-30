package rescuedroneapp.rescuedrone.labs.mediatek.plotandroidsensors;

import java.util.ArrayList;

/**
 * Created by oscar on 18/03/2016.
 */
public class Preprocessing implements RollingWindowChangesListener {

    private ArrayList<RollingWindowChangesListener> rollingWindowChangesListenerListeners;

    public Preprocessing(ArrayList<RollingWindowChangesListener> rollingWindowChangesListenerListeners) {
        this.rollingWindowChangesListenerListeners = rollingWindowChangesListenerListeners;
    }

    @Override
    public void newRollingWindowRawData(float[][][] snapshotOfAccelGyroMagnetoInRawWindows, int[] snapshotOfSpeedWindow) {
        new CalculatorThread(snapshotOfAccelGyroMagnetoInRawWindows[0],
                snapshotOfAccelGyroMagnetoInRawWindows[1],
                snapshotOfAccelGyroMagnetoInRawWindows[2]).start();
    }

    private class CalculatorThread extends Thread {

        private float[][] arrayAccelerometerValues;
        private float[][] arrayGyroscopeValues;
        private float[][] arrayMagnetometerValues;

        public CalculatorThread (float[][] arrayAccelerometerValues,
                                         float[][] arrayGyroscopeValues,
                                         float[][] arrayMagnetometerValues) {
            this.arrayAccelerometerValues = arrayAccelerometerValues;
            this.arrayGyroscopeValues = arrayGyroscopeValues;
            this.arrayMagnetometerValues = arrayMagnetometerValues;
        }

        @Override
        public void run() {
            float[][] calculus = makeCalculus();
            for(RollingWindowChangesListener rollingWindowChangesListenerListener : rollingWindowChangesListenerListeners) {
                rollingWindowChangesListenerListener.newRollingWindowDeviceWorldCalculus(calculus);
            }
        }

        public float[][] makeCalculus() {
            float[][] calculus = new float[3][9];
            System.arraycopy( getMeansVector3(arrayAccelerometerValues), 0, calculus[0], 0, 3);
            System.arraycopy(getMeansVector3(arrayGyroscopeValues), 0, calculus[0], 3, 3);
            System.arraycopy( getMeansVector3(arrayMagnetometerValues), 0, calculus[0], 6, 3);
            // TODO implement variances calculus[1]
            //System.arraycopy( getMeansVector3(arrayAccelerometerValues), 0, calculus[0], 0, 3);

            return calculus;
        }

        private float[] getMeansVector3(float[][] arrayVector3Values) {
            float xSum = 0.0f;
            float ySum = 0.0f;
            float zSum = 0.0f;
            for(float[] vector3Reading: arrayVector3Values) {
                xSum += vector3Reading[0];
                ySum += vector3Reading[1];
                zSum += vector3Reading[2];
            }
            float[] meansVector3 = new float[3];
            meansVector3[0] = xSum/arrayVector3Values.length;
            meansVector3[1] = ySum/arrayVector3Values.length;
            meansVector3[2] = zSum/arrayVector3Values.length;
            return meansVector3;
        }

        private float[] getVariancesVector3(float[] meansVector3,
                                            float[][] arrayVector3Values) {
            float xiMinusMeanXTwoSquaredSum = 0.0f;
            float yiMinusMeanYTwoSquaredSum = 0.0f;
            float ziMinusMeanZTwoSquaredSum = 0.0f;
            for(float[] vector3Reading: arrayVector3Values) {
                xiMinusMeanXTwoSquaredSum +=
                        Math.pow(vector3Reading[0] - meansVector3[0], 2);
                yiMinusMeanYTwoSquaredSum +=
                        Math.pow(vector3Reading[1] - meansVector3[1], 2);
                ziMinusMeanZTwoSquaredSum +=
                        Math.pow(vector3Reading[2] - meansVector3[2], 2);
            }
            float[] variancesVector3 = new float[3];
            variancesVector3[0] = xiMinusMeanXTwoSquaredSum/arrayVector3Values.length;
            variancesVector3[1] = yiMinusMeanYTwoSquaredSum/arrayVector3Values.length;
            variancesVector3[2] = ziMinusMeanZTwoSquaredSum/arrayVector3Values.length;
            return variancesVector3;
        }


        private boolean isVector3InRange (float[] vector3, float range) {
            boolean isInRange = false;
            if (vector3[0] < range
                    && vector3[0] > -range) {
                if (vector3[1] < range
                        && vector3[1] > -range) {
                    if (vector3[2] < range
                            && vector3[2] > -range) {
                        isInRange = true;
                    }
                }
            }
            return isInRange;
        }


        private float[] getSVMVector3(float[][] arrayVector3Values) {
            float xPow2, yPow2, zPow2;
            float[] svmVector = new float[arrayVector3Values.length];
            for (int i=0; i<arrayVector3Values.length; i++) {
                xPow2 = (float) Math.pow(arrayVector3Values[i][0], 2);
                yPow2 = (float) Math.pow(arrayVector3Values[i][1], 2);
                zPow2 = (float) Math.pow(arrayVector3Values[i][2], 2);
                svmVector[i] = xPow2 + yPow2 + zPow2;
            }
            return svmVector;
        }
    }

    @Override
    public void newRollingWindowTransformedToRealWorld(float[][][] snapshotAccelGyroMagnetoRealWorldWindows,
                                                       int[] snapshotOfSpeedWindow) {
        new RealWorldCalculatorThread(snapshotAccelGyroMagnetoRealWorldWindows[0],
                snapshotAccelGyroMagnetoRealWorldWindows[1],
                snapshotAccelGyroMagnetoRealWorldWindows[2]).start();
    }

    private class RealWorldCalculatorThread extends Thread{

        private float[][] arrayAccelerometerValues;
        private float[][] arrayGyroscopeValues;
        private float[][] arrayMagnetometerValues;

        public RealWorldCalculatorThread (float[][] arrayAccelerometerValues,
                                 float[][] arrayGyroscopeValues,
                                 float[][] arrayMagnetometerValues) {
            this.arrayAccelerometerValues = arrayAccelerometerValues;
            this.arrayGyroscopeValues = arrayGyroscopeValues;
            this.arrayMagnetometerValues = arrayMagnetometerValues;
        }

        @Override
        public void run() {
            float[][] calculus = makeRealWorldCalculus();
            for(RollingWindowChangesListener rollingWindowChangesListenerListener : rollingWindowChangesListenerListeners) {
                rollingWindowChangesListenerListener.newRollingWindowRealWorldCalculus(calculus);
            }
        }

        private float[][] makeRealWorldCalculus() {
            float[][] realWorldCalculus = new float[3][9];
            realWorldCalculus[0][2] = getMeanFromMatrixColumnVector(arrayAccelerometerValues, 2);
            realWorldCalculus[2][0] = getSVMVector3Difference(arrayAccelerometerValues);
            realWorldCalculus[2][3] = getSVMVector3Difference(arrayGyroscopeValues);
            return  realWorldCalculus;
        }

        private float getSVMVector3Difference(float[][] arrayVector3Values) {
            float xPow2, yPow2, zPow2;
            float maxSVM = -1;
            float minSVM = Float.MAX_VALUE;
            float currentSVMValue;
            for (int i=0; i<arrayVector3Values.length; i++) {
                xPow2 = (float) Math.pow(arrayVector3Values[i][0], 2);
                yPow2 = (float) Math.pow(arrayVector3Values[i][1], 2);
                zPow2 = (float) Math.pow(arrayVector3Values[i][2], 2);
                currentSVMValue = xPow2 + yPow2 + zPow2;
                if (maxSVM < currentSVMValue) {
                    maxSVM = currentSVMValue;
                } else if (minSVM > currentSVMValue) {
                    minSVM = currentSVMValue;
                }
            }
            return maxSVM-minSVM;
        }
        private float getMeanFromMatrixColumnVector(float[][] matrix, int columnId) {
            float sum = 0.0f;
            for(int i=0; i<matrix.length; i++) {
                sum += matrix[i][columnId];
            }
            return sum/matrix.length;
        }
    }

    @Override
    public void newRollingWindowDeviceWorldCalculus(float[][] calculusMatrix) {}

    @Override
    public void newRollingWindowRealWorldCalculus(float[][] calculusMatrix) {}

}
