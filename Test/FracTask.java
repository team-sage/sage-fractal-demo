import com.sage.task.SageTask;

/**
 * Created by root on 3/29/16.
 */
public class FracTask implements SageTask{

    /**
     * Send a job to create a fractal row
     * @param width Screen width
     * @param height Screen height
     * @param max Maximum fractal depth
     */
    public FracTask(){

    }

    public static byte[] int2byte(int[]src) {
        int srcLength = src.length;
        byte[]dst = new byte[srcLength << 2];

        for (int i=0; i<srcLength; i++) {
            int x = src[i];
            int j = i << 2;
            dst[j++] = (byte) ((x >>> 0) & 0xff);
            dst[j++] = (byte) ((x >>> 8) & 0xff);
            dst[j++] = (byte) ((x >>> 16) & 0xff);
            dst[j++] = (byte) ((x >>> 24) & 0xff);
        }
        return dst;
    }

    public static int[] byte2int(byte[]src) {
        int dstLength = src.length >>> 2;
        int[]dst = new int[dstLength];

        for (int i=0; i<dstLength; i++) {
            int j = i << 2;
            int x = 0;
            x += (src[j++] & 0xff) << 0;
            x += (src[j++] & 0xff) << 8;
            x += (src[j++] & 0xff) << 16;
            x += (src[j++] & 0xff) << 24;
            dst[i] = x;
        }
        return dst;
    }

    /**
     * Fractal method to get a completed row
     * @param row current row number (y position on screen)
     * @return array of "iterations" which can be quickly drawn to the screen
     */
    public int[] getIterations(int width, int height, int max, int row){
        int[] iterationNums = new int[width];

        for (int col = 0; col < width; col++) {
            double a = (col - width  / 2) * 4.0 / width;
            double b = (row - height / 2) * 4.0 / width;
            double x = 0, y = 0;

            int iteration = 0;
            while (x*x + y*y < 4 && iteration < max) {
                double xTemp = x*x - y*y + a;
                y = 2 * x * y + b;
                x = xTemp;
                iteration++;
            }
            iterationNums[col] = iteration;
        }
        return iterationNums;
    }

    public byte[] runTask(long taskNum, byte[] data){
        int[] inputData = byte2int(data);

        int iterations[];
        if(inputData.length >= 4) {
            iterations = getIterations(inputData[0], inputData[1], inputData[2], inputData[3]);
            byte[] dataToReturn = int2byte(iterations);

            return dataToReturn;
        }

        return null;
    }
}
