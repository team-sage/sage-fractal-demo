package sample;

import com.sage.api.models.Job;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

import com.sage.api.client.SageClient;

public class Main extends Application {
    private final int WINWIDTH  =   192;    //Window width
    private final int WINHEIGHT =   108;    //Window height
    private final int MAXDEPTH  =   1000;   //Maximum recursive depth of fractal
    private final int BOUNTY    =   8;      //Bounty per job
    private final int TIMEOUT   =   100;    //Job timeout

    @Override public void start(Stage stage) {
        fadeTest(stage);
    }

    private void fadeTest(Stage stage){

        //Create a JavaFX canvas with the above specified width and height
        final Canvas canvas = new Canvas(WINWIDTH, WINHEIGHT);

        //Content for the JavaFX timer
        AnimationTimer timer = new AnimationTimer() {
            int currentJobToSend    =   0;  //Keep track of which job is being sent out per frame
            int numberOfJobs        =   0;  //Keep track of how many active jobs are out there
            int currentJobToProcess =   0;  //Keep track of which job is currently being processed
            int totalJobsCompleted  =   0;  //Keep track of how many jobs have been completed

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
                if(init) {
                    //Get the java file to be sent and computed on android devices
                    File javaFile = new File("/home/wert/Documents/Test/FracTask.java");
                    //Set jobid as -1 for checking later
                    int jobid = -1;
                    try {
                        //Int array of all necessary data.
                        //Width, height, depth, and current job (row number) are used by the java file
                        int[] data = {WINWIDTH, WINHEIGHT, MAXDEPTH, currentJobToSend};
                        //Convert to byte array
                        byte[] dataToSend = int2byte(data);
                        //Use the SageClient sc to place the job order
                        jobid = sc.placeJobOrder(BOUNTY, TIMEOUT, dataToSend, javaFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //Check for success, and att the jobid with its associated row number to the HashMap
                    if(jobid != -1) ids.put(jobid, currentJobToSend);
                    System.out.println(jobid); //TODO: remove debug print
                    //Iterate to the next job to send
                    currentJobToSend++;
                    //Stop coming to the init state when all jobs are sent out
                    if(currentJobToSend == WINHEIGHT)init = false;
                }

                //Prepare colors to be drawn one pixel at a time
                GraphicsContext gc = canvas.getGraphicsContext2D();
                Color[] colors = new Color[MAXDEPTH];
                for (int i = 0; i < MAXDEPTH; i++) {
                    colors[i] = Color.hsb(i/256f, 1, i/(i+8f));
                }

                //get current number of running jobs
                numberOfJobs = ids.keySet().size();
                //get current job to process by accessing some key from the keySet.
                Object[] keys = ids.keySet().toArray();
                int jobid = (int)keys[currentJobToProcess];

                //set result to null, assign value to it when result is received.
                byte[] result = null;
                try {
                    System.out.println("job id: " + jobid); //TODO: remove debug print
                    boolean done = sc.pollJob(jobid);
                    if(done) {
                        //get the job from SageClient sc with the current jobid.
                        Job job = sc.getJob(jobid);
                        //Jobs are sometimes coming back null with a done status. Extra check to ensure this doesn't happen.
                        if(job != null)result = job.getResult();

                        //If result is till null, then we didn't get a valid job back.
                        //If a job did come back, use its result to draw a fractal row to the screen.
                        if (result != null) {
                            int[] iterationNums = byte2int(result);
                            for (int col = 0; col < WINWIDTH; col++) {
                                int iteration = iterationNums[col];
                                PixelWriter pw = gc.getPixelWriter();
                                if (iteration < MAXDEPTH) pw.setColor(col, ids.get(jobid), colors[iteration]);
                                else pw.setColor(col, ids.get(jobid), Color.BLACK);
                            }
                        }
                        //Remove this ID because it is no longer needed.
                        ids.remove(jobid);
                        //Keep track of how many jobs have been finished so we know when to end.
                        totalJobsCompleted++;
                    }
                    //Use this else to use a different method of job selection.
                    //This will cycle through all the available jobs each frame.
                    //Without it, we just look for the first available one.
                    //else currentJobToProcess++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //Cycle back to first job if we make it to the end.
                if(currentJobToProcess >= numberOfJobs-1) currentJobToProcess = 0;
                //If all jobs are finished, stop the timer. It is no longer needed.
                if(totalJobsCompleted == WINHEIGHT) this.stop();
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
