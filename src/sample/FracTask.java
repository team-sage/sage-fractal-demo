import com.sage.task.SageTask;

/**
 * Created by root on 3/29/16.
 */
public class FracTask implements SageTask{

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


    public int[] getIterations(int width, int height, int max, double zoomFactor,
                               double startx, double starty, int startRow, int endRow){
        int[] iterationNums = new int[width * (endRow - startRow)];
        int num = 0;
        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < width; col++) {
                double a = (col - width  / 2) * zoomFactor / width + startx;
                double b = (row - height / 2) * zoomFactor / width + starty;
                double x = 0, y = 0;

                int iteration = 0;
                while (x*x + y*y < 4 && iteration < max) {
                    double xTemp = x*x - y*y + a;
                    y = 2 * x * y + b;
                    x = xTemp;
                    iteration++;
                }
                iterationNums[num] = iteration;
                num++;
            }
        }
        return iterationNums;
    }

    public byte[] runTask(long taskNum, byte[] data){
        String inputData = new String(data);
        String[] dataPoints = inputData.split("\\s+");

        int[] iterations;
        if(dataPoints.length >= 7) {
            int winwidth = Integer.parseInt(dataPoints[0]);
            int winheight = Integer.parseInt(dataPoints[1]);
            int maxdepth = Integer.parseInt(dataPoints[2]);
            double zoomFactor = Double.parseDouble(dataPoints[3]);
            double startx = Double.parseDouble(dataPoints[4]);
            double starty = Double.parseDouble(dataPoints[5]);
            int startRow = Integer.parseInt(dataPoints[6]);
            int endRow = Integer.parseInt(dataPoints[7]);

            iterations = getIterations(winwidth, winheight, maxdepth, zoomFactor, startx, starty, startRow, endRow);
            byte[] dataToReturn = int2byte(iterations);

            return dataToReturn;
        }

        return null;
    }
}
