package sample;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.sage.api.client.SageClient;

public class Main extends Application {
    public static final int WINWIDTH  =   1920/3;    //Window width
    public static final int WINHEIGHT =   1080/3;    //Window height
    public static final int MAXDEPTH  =   1000;   //Maximum recursive depth of fractal
    public static final int BOUNTY    =   8;      //Bounty per job
    public static final int TIMEOUT   =   100;    //Job timeout

    public static int totalJobsCompleted  =   0;  //Keep track of how many jobs have been completed

    //public static final Semaphore sema = new Semaphore(1); //Semaphore for adding to the ids map
    protected final ExecutorService pool =
            new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());

    @Override public void start(Stage stage) {
        fadeTest(stage);
    }

    private void fadeTest(Stage stage){

        //Create a JavaFX canvas with the above specified width and height
        final Canvas canvas = new Canvas(WINWIDTH, WINHEIGHT);

        //Content for the JavaFX timer
        AnimationTimer timer = new AnimationTimer() {
            int currentJobToSend    =   0;  //Keep track of which job is being sent out per frame
            int currentJobToProcess =   0;  //Keep track of which job is currently being processed


            //HashMap of kv-pairs consisting of (Job ID, Fractal Row Number)
            Map<Integer, Integer> ids = new HashMap<Integer, Integer>();

            //Start a Sage API client
            SageClient sc = new SageClient();

            //Flag to check if all the jobs are still being sent out. Set to false when done.
            boolean init = true;
            //Handle "now" to be able to track progression. Content is called once per frame
            @Override
            public void handle(long now) {

                //Send out all the jobs
                while (init) {
                    ConcurrentJob tempJob = new ConcurrentJob(currentJobToSend, sc, ids);
                    FutureTask<Integer> tempTask = new FutureTask<Integer>(tempJob);
                    pool.submit(tempTask);
                    //Iterate to the next job to send
                    currentJobToSend++;
                    //Stop coming to the init state when all jobs are sent out
                    if (currentJobToSend == WINHEIGHT){
                        System.out.println("All jobs sent");
                        init = false;
                    }
                }
                //if(!init) {
                int numberOfJobs = ids.keySet().size();
                for(int iJob = 0; iJob < numberOfJobs; iJob++) {
                    //System.out.println("entered loop");
                    //ConcurrentDraw tempDraw = new ConcurrentDraw(currentJobToProcess, ids, canvas, sc);
                    ConcurrentDraw tempDraw = new ConcurrentDraw(iJob, ids, canvas, sc);
                    FutureTask<Integer> tempTask = new FutureTask<Integer>(tempDraw);
                    pool.submit(tempTask);
                    //Cycle back to first job if we make it to the end.
                    //if (currentJobToProcess >= numberOfJobs - 1) currentJobToProcess = 0;
                    //If all jobs are finished, stop the timer. It is no longer needed.
                    if (totalJobsCompleted == WINHEIGHT) {
                        System.out.println("Finished drawing to screen!");
                        pool.shutdown();
                        this.stop();
                    }
                }
                //}

            }
        };

        //Start the scene
        stage.setScene(new Scene(new Group(canvas)));
        //Show the stage
        stage.show();
        //Start the timer
        timer.start();
    }

    /**
     * Convert an integer array to a byte array and return it
     * @param src input int array
     * @return byte array
     */
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

    /**
     * Convert a byte array to an integer array
     * @param src input byte array
     * @return integer array
     */
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