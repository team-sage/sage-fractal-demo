package sample;

import com.sage.api.client.SageClient;
import com.sage.api.models.Job;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * Created by root on 4/21/16.
 */
public class ConcurrentPoll implements Callable<Integer> {
    private Map<Integer, Integer> ids;
    private SageClient sc;
    private int currentJobToProcess;
    public static final Semaphore sema = new Semaphore(1); //Semaphore for adding to the ids map

    public ConcurrentPoll(int currentJobToProcess, Map ids, SageClient sc){
        this.ids = ids;
        this.sc = sc;
        this.currentJobToProcess = currentJobToProcess;
    }

    @Override
    public Integer call() throws Exception {

        //get current number of running jobs
        int numberOfJobs = ids.keySet().size();
        //get current job to process by accessing some key from the keySet.
        Object[] keys = ids.keySet().toArray();
        if (currentJobToProcess < keys.length) {
            int jobnum = (int) keys[currentJobToProcess];
            if (!Main.completion[jobnum]) {
                int jobid  = ids.get(jobnum);
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
                        if (result != null && jobnum < Main.size) {
                            sema.acquire();
                            Main.completion[jobnum] = true;
                            Main.resultSets[jobnum] = job.getResult();
                            sema.release();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
        return 0;
    }
}
