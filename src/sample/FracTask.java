package sample;

/**
 * Created by root on 3/29/16.
 */
public class FracTask {

    private int width, height, max;

    public FracTask(int width, int height, int max){
        this.width = width;
        this.height = height;
        this.max = max;
    }

    /**
     * Fractal method to get a completed row
     * @param row current row number (y position on screen)
     * @return array of "iterations" which can be quickly drawn to the screen
     */
    public int[] getIterations(int row){
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

                        /*PixelWriter pw = gc.getPixelWriter();
                        if (iteration < max) pw.setColor(col, row, colors[iteration]);
                        else pw.setColor(col, row, Color.BLACK);*/

        }

        return iterationNums;
    }
}
