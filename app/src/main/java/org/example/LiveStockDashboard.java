/*
 * Neither Yahoo Finance nor Alpha Vantage can fulfill the strict 5-second live update 
 * requirement due to the rate limits on their free tiers.
 * However, Alpha Vantage offers superior API stability and easier integration for this project.
 * 
 * Replace "MY_ALPHA_VANTAGE_API" below with an actual Alpha Vantage API key to run this app.
 * 
 * To run the application, navigate to the project directory in the terminal and type:
 * 
 *     gradle run
 * 
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

public class LiveStockDashboard extends Application {

    // API key for Alpha Vantage service - replace with your valid key
    private static final String ALPHA_VANTAGE_API_KEY = "UL385WUWKVPWPGE";

    // Symbol for the Dow Jones Industrial Average ETF (DIA) used to fetch price data
    private static final String SYMBOL = "DIA";

    // Formatter to display time on the X-axis as hours:minutes:seconds
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Maximum number of data points to display on the chart at any one time
    private static final int MAX_DATA_POINTS = 30;

    // Scheduled executor for running periodic price fetch tasks on a background thread
    private ScheduledExecutorService scheduledExecutorService;

    // Data series for the JavaFX line chart to hold time-price pairs
    private XYChart.Series<String, Number> series;

    // In-memory storage of all fetched data points to display on refresh
    private List<XYChart.Data<String, Number>> storedDataPoints = new ArrayList<>();

    /**
     * JavaFX application entry point – sets up the UI and starts data updates.
     */
    @Override
    public void start(Stage stage) {
        stage.setTitle("Live Dow Jones Dashboard");

        // X-axis: category axis for time labels (formatted HH:mm:ss)
        final CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time (HH:mm:ss)");
        xAxis.setAnimated(true);     // Enables smooth axis updates

        // Y-axis: numeric axis for stock price values in USD
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Stock Price (USD)");

        // Create the LineChart with the specified axes
        final LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Live Dow Jones Monitoring");
        lineChart.setAnimated(true);

        // Initialize the series to hold (time, price) data points
        series = new XYChart.Series<>();
        series.setName("Dow Jones Industrial Average");

        // Add the series to the chart's data list
        lineChart.getData().add(series);

        // Load stored data points into the series so chart shows all previous points on refresh
        loadStoredData();

        // Setup and show the JavaFX scene containing the chart
        Scene scene = new Scene(lineChart, 800, 600);
        stage.setScene(scene);
        stage.show();

        // Start the periodic task to fetch live price data and update chart
        startDataUpdates();
    }

    /**
     * Fetches the latest stock price for the SYMBOL from the Alpha Vantage API.
     * 
     * @return latest price as BigDecimal
     * @throws Exception on network or parsing errors
     */
    private BigDecimal fetchLatestPrice() throws Exception {
        // Build the Alpha Vantage API URL for the Global Quote function
        String url = String.format(
            "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
            SYMBOL, ALPHA_VANTAGE_API_KEY);

        // Create a new HttpClient for sending the HTTP request
        HttpClient client = HttpClient.newHttpClient();

        // Build the HTTP GET request to fetch the JSON quote
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        // Send the request and get the response body as a string
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check for successful HTTP response code
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch data: HTTP " + response.statusCode());
        }

        // Parse the JSON response using Jackson ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.body());

        // Navigate to the "Global Quote" node, which contains the price info
        JsonNode globalQuote = rootNode.path("Global Quote");

        // Validate the response contains the expected data
        if (globalQuote.isMissingNode() || globalQuote.size() == 0) {
            throw new RuntimeException("Invalid data received from Alpha Vantage");
        }

        // Extract the price string from the "05. price" field
        String priceStr = globalQuote.path("05. price").asText();

        // Ensure price is present and non-empty
        if (priceStr == null || priceStr.isEmpty()) {
            throw new RuntimeException("Price data missing in Alpha Vantage response");
        }

        // Convert the price string to a BigDecimal and return it
        return new BigDecimal(priceStr);
    }

    /**
     * Starts a scheduled task that fetches stock data every 60 seconds and updates the chart.
     */
    private void startDataUpdates() {
        // Create a single-threaded executor for scheduled fetching
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        // Schedule the fetch task at fixed-rate intervals (initial delay 0, repeat every 60 seconds)
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                // Fetch the latest price from the Alpha Vantage API
                BigDecimal price = fetchLatestPrice();

                // Format the current local time for the X-axis label
                String timestamp = LocalDateTime.now().format(TIME_FORMATTER);

                // Update the UI on the JavaFX Application Thread
                Platform.runLater(() -> {
                    // Create new data point with current timestamp and price
                    XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(timestamp, price);

                    // Store data point in memory for persistence during app session
                    storedDataPoints.add(dataPoint);

                    // Add data point to the chart series to update the graph
                    series.getData().add(dataPoint);

                    // Maintain a sliding window by removing oldest points beyond MAX_DATA_POINTS
                    if (series.getData().size() > MAX_DATA_POINTS) {
                        series.getData().remove(0);              // Remove oldest from chart
                        storedDataPoints.remove(0);               // Remove oldest from storage
                    }
                });

            } catch (Exception e) {
                // Print any error during fetch or update to console
                System.err.println("Error fetching stock data: " + e.getMessage());
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    /**
     * Load all stored data points into the chart series. Useful on refresh/start
     * so the chart displays all previously fetched data.
     */
    private void loadStoredData() {
        // Clear any existing data in the series
        series.getData().clear();

        // Add all stored data points into the series to show historical data
        series.getData().addAll(storedDataPoints);
    }

    /**
     * Called when the application is stopped; shuts down the scheduled executor cleanly.
     */
    @Override
    public void stop() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            System.out.println("Dashboard closing. Shutting down data fetcher.");
            scheduledExecutorService.shutdown();
        }
    }

    /**
     * Standard JavaFX launcher method.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
