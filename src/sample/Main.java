package sample;

import com.sage.api.models.Job;
import com.sun.corba.se.impl.orbutil.graph.Graph;
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
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.sage.api.client.SageClient;

public class Main extends Application {
    public static final int DIVIDESIZE = 5;
    public static final int WINWIDTH  =   1920/DIVIDESIZE;    //Window width
    public static final int WINHEIGHT =   1080/DIVIDESIZE;    //Window height
    public static  int MAXDEPTH  =   1000;   //Maximum recursive depth of fractal
    public static final int BOUNTY    =   8;      //Bounty per job
    public static final int TIMEOUT   =   100;    //Job timeout
    public static final int FRAMES = 25;
    private double zoomFactor = 4.0;
    private double startx = 0.3585614710926859372;
    private double starty = 0.3229491840959411351;

    public static int totalJobsCompleted  =   0;  //Keep track of how many jobs have been completed

    //public static final Semaphore sema = new Semaphore(1); //Semaphore for adding to the ids map
    protected final ExecutorService pool =
            new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());

    //HashMap of kv-pairs consisting of (Job ID, Fractal Row Number)
    protected static Map<Integer, Integer> ids = new HashMap<Integer, Integer>();
    public static boolean[] completion = new boolean[FRAMES];
    public static int[] depths = new int[FRAMES];
    public static byte[][] resultSets = new byte[FRAMES][WINWIDTH*WINHEIGHT];

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

            //Start a Sage API client
            SageClient sc = new SageClient();
            List<byte[]> batch = new ArrayList<byte[]>();

            //Flag to check if all the jobs are still being sent out. Set to false when done.
            boolean init = true;
            boolean drawState = false;

            boolean logout = true;
            //Handle "now" to be able to track progression. Content is called once per frame
            @Override
            public void handle(long now) {
                GraphicsContext gc = canvas.getGraphicsContext2D();
                Color[] colors = new Color[MAXDEPTH];
                for (int i = 0; i<MAXDEPTH; i++) {
                    colors[i] = Color.hsb(i/256f, 1, i/(i+8f));
                }
                /*if (logout) {
                    sc.logout();
                    logout = false;
                }*/

                //Send out all the jobs
                while (init) {
                    //Int array of all necessary data.
                    //Width, height, depth, and current job (row number) are used by the java file
                    String data = Main.WINWIDTH+" "+Main.WINHEIGHT+" "+Main.MAXDEPTH+" "+zoomFactor+" "+startx+" "+starty;
                    //Convert to byte array
                    byte[] dataToSend = data.getBytes();
                    batch.add(dataToSend);
                    depths[currentJobToSend] = MAXDEPTH;
                    zoomFactor*=0.7;
                    MAXDEPTH*=1.05;
                    //Iterate to the next job to send
                    currentJobToSend++;

                    //Stop coming to the init state when all jobs are sent out
                    if (currentJobToSend == FRAMES){
                        init = false;
                        try {
                            File javaFile = new File("/home/wert/Documents/Test/FracTask.java");
                            ids = sc.placeBatchOrder(javaFile, new BigDecimal(2.0), 1200000, batch);
                            System.out.println("Jobs sent, getting job ids");
                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
                if(!drawState) {
                    for (int i = 0; i < ids.size(); i++) {
                        ConcurrentPoll tempPoll = new ConcurrentPoll(i, ids, sc);
                        FutureTask<Integer> tempTask = new FutureTask<Integer>(tempPoll);
                        pool.submit(tempTask);
                    }
                }
                else if(currentJobToProcess < ids.size()){
                    try {
                        //Job job = sc.getJob(ids.get(currentJobToProcess));
                        //System.out.println("Status for job " + currentJobToProcess + ": " + sc.getJob(currentJobToProcess).getStatus()); //TODO: debug
                        //byte[] result = null;
                        //if (job != null) result = job.getResult();
                        byte[]result = resultSets[currentJobToProcess];
                        if (result != null) {
                            int[] dataBack = byte2int(result);
                            int col = 0;
                            int row = 0;
                            for(int i = 0; i < dataBack.length; i++){
                                if(i % WINWIDTH==0 && col > 0){
                                    col = 0;
                                    row++;
                                }
                                int iteration = dataBack[i];
                                PixelWriter pw = gc.getPixelWriter();
                                if (iteration < depths[currentJobToProcess]) pw.setColor(col, row, colors[iteration]);
                                else pw.setColor(col, row, Color.BLACK);
                                col++;
                            }
                            currentJobToProcess++;
                        }
                        else{
                            System.out.println("yeah");
                        }

                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
                else currentJobToProcess = 0;
                if(!init) {
                    if(!drawState && timeToDraw()){
                        drawState=true;
                        pool.shutdownNow();
                    }
                    try{
                        Thread.sleep(WINHEIGHT);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };

        //Start the scene
        stage.setScene(new Scene(new Group(canvas)));
        //Show the stage
        stage.show();
        //Start the timer
        timer.start();
    }

    public static boolean timeToDraw(){
        for(int j = 0; j < completion.length; j++){
            if(completion[j]==false)return false;
        }
        return true;
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

    public static byte[] double2byte(double[]src) {
        int srcLength = src.length;
        byte[]dst = new byte[srcLength << 3];

        for (int i=0; i<srcLength; i++) {
            double x = src[i];
            int j = i << 3;
            long l = Double.doubleToRawLongBits(x);
            dst[j++] = (byte)((l >> 0) & 0xff);
            dst[j++] = (byte)((l >> 8) & 0xff);
            dst[j++] = (byte)((l >> 16) & 0xff);
            dst[j++] = (byte)((l >> 24) & 0xff);
            dst[j++] = (byte)((l >> 32) & 0xff);
            dst[j++] = (byte)((l >> 40) & 0xff);
            dst[j++] = (byte)((l >> 48) & 0xff);
            dst[j++] = (byte)((l >> 56) & 0xff);
        }
        return dst;
    }

    public static double[] byte2double(byte[]src) {
        int dstLength = src.length >>> 3;
        double[]dst = new double[dstLength];

        for (int i=0; i<dstLength; i++) {
            int j = i << 3;
            long x = 0;
            x += (src[j++] & 0xff) << 0;
            x += (src[j++] & 0xff) << 8;
            x += (src[j++] & 0xff) << 16;
            x += (src[j++] & 0xff) << 24;
            x += (src[j++] & 0xff) << 32;
            x += (src[j++] & 0xff) << 40;
            x += (src[j++] & 0xff) << 48;
            x += (src[j++] & 0xff) << 56;
            dst[i] = Double.longBitsToDouble(x);
        }
        return dst;
    }

    public static void main(String[] args) { launch(args); }
}