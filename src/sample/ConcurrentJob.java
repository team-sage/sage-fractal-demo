package sample;

import com.sage.api.client.SageClient;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * Created by root on 4/4/16.
 */
public class ConcurrentJob implements Callable<Integer>{
    private SageClient sc;
    private int jobNumber;
    private Map<Integer, Integer> ids;

    private static final Semaphore sema = new Semaphore(1); //Semaphore for adding to the ids map

    public ConcurrentJob(int jobNumber, SageClient sageClient, Map map){
        this.jobNumber = jobNumber;
        sc = sageClient;
        ids = map;
    }

    @Override
    public Integer call() throws Exception {
        //Get the java file to be sent and computed on android devices
        File javaFile = new File("/home/wert/Documents/Test/FracTask.java");
        //Set jobid as -1 for checking later
        int jobid = -1;
        try {
            //Int array of all necessary data.
            //Width, height, depth, and current job (row number) are used by the java file
            int[] data = {Main.WINWIDTH, Main.WINHEIGHT, Main.MAXDEPTH, jobNumber};
            //Convert to byte array
            byte[] dataToSend = Main.int2byte(data);
            //Use the SageClient sc to place the job order
            sema.acquire();
            jobid = sc.placeJobOrder(Main.BOUNTY, Main.TIMEOUT, dataToSend, javaFile);
            //Check for success, and att the jobid with its associated row number to the HashMap
            if(jobid != -1) ids.put(jobid, jobNumber);
            else System.out.println("Something happened :(");
            sema.release();
            System.out.println("Job posted, ID is: " + jobid); //TODO: remove debug print
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jobid;
    }
}
