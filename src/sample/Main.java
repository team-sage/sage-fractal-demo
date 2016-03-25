package sample;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {
    private final int WINWIDTH = 1920;
    private final int WINHEIGHT = 1080;
    private final int MAXDEPTH = 10000;

    @Override public void start(Stage stage) {
        fadeTest(stage);
    }

    private void fadeTest(Stage stage){
        final Canvas canvas = new Canvas(WINWIDTH, WINHEIGHT);
        AnimationTimer timer = new AnimationTimer() {
            int row = 0;
            long then = 0;
            @Override
            public void handle(long now) {
                GraphicsContext gc = canvas.getGraphicsContext2D();
                int width = WINWIDTH, height = WINHEIGHT, max = MAXDEPTH;

                Color[] colors = new Color[max];
                for (int i = 0; i<max; i++) {
                    colors[i] = Color.hsb(i/256f, 1, i/(i+8f));
                }

                //for (int row = 0; row < height; row++) {
                if(Math.abs(now - then) > 0.0 && row < WINHEIGHT) {
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

                        PixelWriter pw = gc.getPixelWriter();
                        if (iteration < max) pw.setColor(col, row, colors[iteration]);
                        else pw.setColor(col, row, Color.BLACK);
                    }
                    row++;
                    then = now;
                }
                //}

            }
        };

        stage.setScene(new Scene(new Group(canvas)));
        stage.show();

        timer.start();
    }

    public static void main(String[] args) { launch(args); }
}
