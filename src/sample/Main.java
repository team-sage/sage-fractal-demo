package sample;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class Main extends Application {

    static int dx = 1;
    static int dy = 1;

    private final int WINWIDTH = 1920;
    private final int WINHEIGHT = 1080;
    private final int MAXDEPTH = 1000;

    @Override
    public void start(Stage primaryStage) throws Exception{
        //Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        Group root = new Group();
        primaryStage.setTitle("Hello World");

        Canvas canvas = new Canvas(WINWIDTH, WINHEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        drawBrot(gc);
        root.getChildren().add(canvas);
        primaryStage.setScene(new Scene(root, WINWIDTH, WINHEIGHT));
        primaryStage.show();
    }


    private void drawBrot(GraphicsContext gc){
        int width = WINWIDTH, height = WINHEIGHT, max = MAXDEPTH;

        Color[] colors = new Color[max];
        for (int i = 0; i<max; i++) {
            colors[i] = Color.hsb(i/256f, 1, i/(i+8f));
        }

        for (int row = 0; row < height; row++) {
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

                int red = 255;
                int green = 255;
                int blue = 255;

                PixelWriter pw = gc.getPixelWriter();
                if (iteration < max) pw.setColor(col, row, colors[iteration]);
                else pw.setColor(col, row, Color.BLACK);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
