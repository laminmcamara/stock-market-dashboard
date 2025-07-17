/*
     *
     * Neither Yahoo Finance nor Alpha Vantage can fulfill the strict 5-second live update
     * requirement due to the rate limits on their free tiers.
     *
     * However, Alpha Vantage offers superior API stability and easier integration for this project.
     *
     * Replace "MY_ALPHA_VANTAGE_API" below with an actual Alpha Vantage API key to run this app.
     *
     * To run the application, navigate to the project directory in the terminal and type:
     * gradle run
     * Then press Enter.
*/

package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JavaFX app showing live Dow Jones price using Alpha Vantage API.
 */
public class App extends Application {

    private static final String ALPHA_VANTAGE_API_KEY = "MY_ALPHA_VANTAGE_API";
    private static final String SYMBOL = "DIA";

    private XYChart.Series<String, Number> series = new XYChart.Series<>();
    private List<XYChart.Data<String, Number>> storedDataPoints = new ArrayList<>();
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * JavaFX entry point - called when the application launches.
     * Sets up and shows the primary stage (main window).
     */

    @Override
    public void start(Stage stage) {
        
        // Set window title
        stage.setTitle("Live Dow Jones Dashboard");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price ($)");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("DIA Live Price Update");
        lineChart.getData().add(series);

        Scene scene = new Scene(lineChart, 800, 600);
        stage.setScene(scene);

        // Show the window
        stage.show();

        loadStoredData();
        startDataFetcher();
    }

    private void startDataFetcher() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                String uri = String.format(
                        "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                        SYMBOL, ALPHA_VANTAGE_API_KEY);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uri))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                JsonNode globalQuote = root.path("Global Quote");

                if (!globalQuote.isMissingNode()) {
                    String priceStr = globalQuote.path("05. price").asText();
                    BigDecimal price = new BigDecimal(priceStr);

                    String timeFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                    Platform.runLater(() -> {
                        XYChart.Data<String, Number> data = new XYChart.Data<>(timeFormatted, price);
                        series.getData().add(data);
                        storedDataPoints.add(data);

                        if (series.getData().size() > 20) {
                            series.getData().remove(0);
                            storedDataPoints.remove(0);
                        }
                    });
                } else {
                    System.err.println("No data received from Alpha Vantage API.");
                }

            } catch (Exception e) {
                System.err.println("Error fetching stock data: " + e.getMessage());
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    private void loadStoredData() {
        series.getData().clear();
        series.getData().addAll(storedDataPoints);
    }
    
    @Override
    public void stop() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            System.out.println("Dashboard closing. Shutting down data fetcher.");
            scheduledExecutorService.shutdown();
        }
    }


    /**
     * Main method to launch the JavaFX application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
