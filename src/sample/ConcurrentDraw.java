package sample;

import com.sage.api.client.SageClient;
import com.sage.api.models.Job;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * Created by root on 4/5/16.
 */


public class ConcurrentDraw implements Callable<Integer> {
    private Map<Integer, Integer> ids;
    private Canvas canvas;
    private SageClient sc;
    private int currentJobToProcess;
    public static final Semaphore sema = new Semaphore(1); //Semaphore for adding to the ids map


    public ConcurrentDraw(int currentJobToProcess, Map ids, Canvas canvas, SageClient sc){
        this.ids = ids;
        this.canvas = canvas;
        this.sc = sc;
        this.currentJobToProcess = currentJobToProcess;
    }

    @Override
    public Integer call() throws Exception {
        //Prepare colors to be drawn one pixel at a time
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Color[] colors = new Color[Main.MAXDEPTH];
        for (int i = 0; i < Main.MAXDEPTH; i++) {
            colors[i] = Color.hsb(i / 256f, 1, i / (i + 8f));
        }

        //get current number of running jobs
        int numberOfJobs = ids.keySet().size();
        //System.out.println(numberOfJobs); //TODO: debug
        //get current job to process by accessing some key from the keySet.
        Object[] keys = ids.keySet().toArray();
        if (currentJobToProcess < keys.length) {
            int jobid = (int) keys[currentJobToProcess];
            //set result to null, assign value to it when result is received.
            byte[] result = null;
            try {
                boolean done = sc.pollJob(jobid);
                System.out.println("Status for job " + jobid + ": " + sc.getJob(jobid).getStatus()); //TODO: debug
                if (done) {
                    //get the job from SageClient sc with the current jobid.
                    Job job = sc.getJob(jobid);
                    //Jobs are sometimes coming back null with a done status.
                    //Extra check to ensure this doesn't happen.
                    if (job != null) result = job.getResult();

                    //If result is till null, then we didn't get a valid job back.
                    //If a job did come back, use its result to draw a fractal row to the screen.
                    if (result != null) {
                        //System.out.println(", and job is non-null"); //TODO: debug
                        int[] iterationNums = Main.byte2int(result);
                        for (int col = 0; col < Main.WINWIDTH; col++) {
                            int iteration = iterationNums[col];
                            sema.acquire();
                            PixelWriter pw = gc.getPixelWriter();
                            //System.out.println("null?: " + jobid);//TODO: debug
                            if(ids.get(jobid)!=null) {
                                if (iteration < Main.MAXDEPTH) pw.setColor(col, ids.get(jobid), colors[iteration]);
                                else pw.setColor(col, ids.get(jobid), Color.BLACK);
                            }
                            sema.release();
                        }
                        //Remove this ID because it is no longer needed.
                        ids.remove(jobid);
                        //Keep track of how many jobs have been finished so we know when to end.
                        Main.totalJobsCompleted++;
                    }
                    //else System.out.println(", and job is NULL (FIX THIS!!!)"); //TODO: debug
                }
                //Use this else to use a different method of job selection.
                //This will cycle through all the available jobs each frame.
                //Without it, we just look for the first available one.
                else{
                    //System.out.println(); //TODO: debug
                    //currentJobToProcess++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return 0;
    }
}
