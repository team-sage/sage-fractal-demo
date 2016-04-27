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
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.sage.api.client.SageClient;
import org.apache.commons.lang.ArrayUtils;

public class Main extends Application {
    public static final int DIVIDESIZE = 6;
    public static final double WINWIDTH  =   1920/DIVIDESIZE;    //Window width
    public static final double WINHEIGHT =   1080/DIVIDESIZE;//1080/DIVIDESIZE;    //Window height
    public static  int MAXDEPTH  =   100;   //Maximum recursive depth of fractal
    public static final BigDecimal BOUNTY = new BigDecimal(2.0);      //Bounty per job
    public static final int TIMEOUT   =   3600000;    //Job timeout
    public static final int FRAMES = 70;
    private double zoomFactor = 4.0;
    private double startx = 0.3585614710926859372;
    private double starty = 0.3229491840959411351;

    public static final int BRANCH_FACTOR = 2;//DIVIDESIZE;

    public static final double CRITICAL_BRANCHING = (int) Math.pow(2, (int) (Math.log(WINHEIGHT)/Math.log(2)));

    public static int totalJobsCompleted  =   0;  //Keep track of how many jobs have been completed

    //public static final Semaphore sema = new Semaphore(1); //Semaphore for adding to the ids map

    protected final ExecutorService pool =
            new ScheduledThreadPoolExecutor(4 * Runtime.getRuntime().availableProcessors());

    public static final int size = findBranchSize();

    //HashMap of kv-pairs consisting of (Job ID, Fractal Row Number)
    protected static Map<Integer, Integer> ids = new HashMap<Integer, Integer>();
    public static boolean[] completion = new boolean[size];
    public static int[] depths = new int[size];

    public static byte[][] resultSets = new byte[size][(int)(WINWIDTH * WINHEIGHT)];


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

            // a branching factor to determine how many chunks to split the frame into
            int branchFactor = BRANCH_FACTOR;

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
                for (int i = 0; i < MAXDEPTH; i++) {
                    colors[i] = Color.hsb(i/64 + 256f, 1, i/(i+8f));
                    /*Hue, Saturation, brightness:
                    *The hue is i/scale, changing the scale value to smaller numbers makes the gradient more intense
                    * plus (+) some offset. Hue is basically a color wheel so this value can range from 0 to 360 (red to red).
                    */
                }



                //Send out all the jobs
                if (init) {
                    System.out.println("size: " + size);
                    //Int array of all necessary data.
                    //Width, height, depth, and current job (row number) are used by the java file
                    while (currentJobToSend < size) {

                        for (int i = 0; i < branchFactor; i++) {
                            int start = (int) (i * Math.floor(Main.WINHEIGHT / branchFactor));
                            int end = (int) (start + Math.floor(Main.WINHEIGHT / branchFactor));

                            if (i == branchFactor - 1 && end < WINHEIGHT && WINHEIGHT % branchFactor != 0) {
                                // make up the difference
                                end += WINHEIGHT - end;
                                if (end > WINHEIGHT) {
                                    end = (int) WINHEIGHT;
                                }
                                System.out.println("correctedEnd: " + end);
                            }



                            System.out.println("start: " + start + " end: " + end);
//                            System.out.println("**********************************");
                            String data = (int) Main.WINWIDTH+" "+ (int) Main.WINHEIGHT+" "+
                                    Main.MAXDEPTH+" "+zoomFactor+" "+startx+" "+starty+" "+start+" "+end;
                            //Convert to byte array
                            byte[] dataToSend = data.getBytes();
                            batch.add(dataToSend);
                            //depths[currentJobToSend] = MAXDEPTH;
                            //Iterate to the next job to send
                            depths[currentJobToSend] = MAXDEPTH;
                            currentJobToSend++;
                        }
                        // increase the zoom factor
                        zoomFactor*=0.7;
                        MAXDEPTH*=1.05;
                        // increase branching factor if critical branching has not yet been reached
                        if (branchFactor < CRITICAL_BRANCHING) {
                            branchFactor *= BRANCH_FACTOR;
                        } else if (branchFactor == CRITICAL_BRANCHING){
                            //System.out.println("branchFactor: " + branchFactor + " winheight: " + WINHEIGHT);
                            branchFactor = (int) WINHEIGHT;
                        }


                    }
                    init = false;
                    // reset branchFactor
                    branchFactor = BRANCH_FACTOR;
                    try {
                        File javaFile = new File("src/sample/FracTask.java");
                        ids = sc.placeBatchOrder(javaFile, BOUNTY, TIMEOUT, batch);
                        System.out.println("Jobs sent, getting job ids");
                    } catch (Exception e) {
                        System.err.println("An error has occurred while attempting to place the batch order");
                        e.printStackTrace();
                    }


                }

                System.out.println("branchFactor: " + branchFactor);
                if(!drawState) {
                    ArrayList<Future<?>> polls = new ArrayList<Future<?>>();
                    for (int i = 0; i < ids.size(); i++) {
                        // make sure the job isn't completed
                        if (!completion[i]) {
                            ConcurrentPoll tempPoll = new ConcurrentPoll(i, ids, sc);
                            FutureTask<Integer> tempTask = new FutureTask<Integer>(tempPoll);
                            polls.add(pool.submit(tempTask));
                        }
                    }

                    try {
                        // wait until they're all done
                        for (int i = 0; i < polls.size(); i++) {
                            polls.get(i).get();
                        }

                    } catch (Exception e) {
                        System.err.println("An error occurred while attempting to collect polls: ");
                        e.printStackTrace();
                    }


                }
                else if(currentJobToProcess + branchFactor < size){
                    try {

                        int[] databack = byte2int(resultSets[currentJobToProcess]);
                        //System.out.println("Branch Factor: " + branchFactor);
                        for (int i = 1; i < branchFactor; i++) {
                            byte[] r = resultSets[currentJobToProcess + i];
                            databack = ArrayUtils.addAll(databack, byte2int(r));
                        }

                        System.out.println("currentJobToProcess: " + currentJobToProcess);

                        int row = 0;
                        int col = 0;

                        if (databack != null && databack.length > 0) {
                            for (int i = 0; i < databack.length; i++) {
                                int iteration = databack[i];
                                //System.out.println(iteration);
                                PixelWriter pw = gc.getPixelWriter();
                                if (iteration < depths[currentJobToProcess]) {
                                    pw.setColor(col, row, colors[iteration]);
                                }
                                else {
                                    pw.setColor(col, row, Color.BLACK);
                                }

                                col++;
                                if (col == WINWIDTH) {
                                    col = 0;
                                    row++;
                                    System.out.println("row: " + row);
                                    System.out.println("WINHEIGHT / branchFactor: " + Math.floor(WINHEIGHT / branchFactor));
                                    System.out.println("row % (WINHEIGHT / branchFactor): " + row % Math.floor(WINHEIGHT / branchFactor));
                                    if (row % (Math.floor(WINHEIGHT / branchFactor)) == 0) {
                                        currentJobToProcess++;
                                        if (row == WINHEIGHT) {
                                            System.out.println("We've hit the max!!!");
                                            break;
                                        }
                                    }

                                }
                            }
                        }

                    } catch(Exception e){
                        e.printStackTrace();
                    }
                    //System.out.println("current Job to process: " + currentJobToProcess);
                    //currentJobToProcess += branchFactor;
                    if (branchFactor < CRITICAL_BRANCHING) {
                        branchFactor *= BRANCH_FACTOR;
                    } else {
                        System.out.println("branchFactor: " + branchFactor + " winheight: " + WINHEIGHT);
                        System.out.println("critical branching detected at: "+ branchFactor);
                        branchFactor = (int) WINHEIGHT;
                    }
                }
                else {
                    // reset branchFactor
                    branchFactor = BRANCH_FACTOR;
                    currentJobToProcess = 0;
                }

                if(!init) {
                    if(!drawState && timeToDraw()){
                        drawState=true;
                        pool.shutdownNow();
                    }
                    try{
                        Thread.sleep((int)WINHEIGHT);
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

    public static int findBranchSize() {
        int size = 0;
        double branchFactor = BRANCH_FACTOR;

        System.out.println("critical branching: " + CRITICAL_BRANCHING);
        for (int i = 0; i < FRAMES; i++) {
            size += branchFactor;
            // only increase branching factor if critical branching has not yet been reached
            if (branchFactor < CRITICAL_BRANCHING) {
                branchFactor *= BRANCH_FACTOR;
            } else if (branchFactor == CRITICAL_BRANCHING){
                //System.out.println("branchFactor: " + branchFactor + " winheight: " + WINHEIGHT);
                System.out.println("critical branching detected at: "+ branchFactor);
                branchFactor = WINHEIGHT;
            }
        }
        return size;
    }

}