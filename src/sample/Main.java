package sample;

import com.sage.task.SageTask;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.sage.api.client.SageClient;

public class Main extends Application {
    private final int WINWIDTH = 1920;
    private final int WINHEIGHT = 1080;
    private final int MAXDEPTH = 1000;

    @Override public void start(Stage stage) {
        fadeTest(stage);
    }

    private void fadeTest(Stage stage){

        //send out all the sage tasks here
        //TODO: use API to send off all the jobs

        int[] data = {WINWIDTH, WINHEIGHT, MAXDEPTH, 0};
        byte[] dataToSend = int2byte(data);

        SageTask fracTask = new FracTask();
        byte[] dataReceived = fracTask.runTask(0, dataToSend);

        SageClient sc = new SageClient();
        File javaFile = new File("/home/wert/Documents/Test/FracTask.java");
        int jobid = -1;
        try{
            jobid = sc.placeJobOrder(8,100, dataToSend, javaFile);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(jobid);

        final Canvas canvas = new Canvas(WINWIDTH, WINHEIGHT);

        //Content for the JavaFX timer
        AnimationTimer timer = new AnimationTimer() {
            int row = 0; //TODO: remoe these once API is implemented
            long then = 0;



            //Handle "now" to be able to track progression. Content is called once per frame
            @Override
            public void handle(long now) {
                GraphicsContext gc = canvas.getGraphicsContext2D();
                int width = WINWIDTH, height = WINHEIGHT, max = MAXDEPTH;
                //FracTask frac = new FracTask(width,height,max);

                Color[] colors = new Color[max];
                for (int i = 0; i<max; i++) {
                    colors[i] = Color.hsb(i/256f, 1, i/(i+8f));
                }

                //get back any finished tasks here
                //draw them to the screen using the below inner for loop (outer "if loop" to be removed)
                /*if(Math.abs(now - then) > 0.0 && row < WINHEIGHT) { //TODO: replace this with checking for finished jobs
                    int[] iterationNums = frac.getIterations(row);

                    for (int col = 0; col < width; col++) {
                        int iteration = iterationNums[col];
                        PixelWriter pw = gc.getPixelWriter();
                        if (iteration < max) pw.setColor(col, row, colors[iteration]);
                        else pw.setColor(col, row, Color.BLACK);
                    }
                    row++;
                    then = now;
                }*/
            }
        };

        stage.setScene(new Scene(new Group(canvas)));
        stage.show();
        timer.start();
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

    public static void main(String[] args) { launch(args); }
}
