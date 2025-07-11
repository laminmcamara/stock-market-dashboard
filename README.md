# Dow Jones Stock Price Fetcher

## Overview
This Java application fetches the Dow Jones Industrial Average (DJIA) stock price at regular intervals and stores the price along with timestamps in a queue. It demonstrates working with financial data APIs, handling rate limits, and basic queue management.

## Features
- Fetches DJIA price using the Alpha Vantage API.
- Stores fetched prices and timestamps in a queue.
- Respects API rate limits by fetching data every 60 seconds.
- Handles connection errors gracefully.
- Simple console output of fetched data.

## Requirements
- Java 8 or higher
- Gradle (for building and running)
- Alpha Vantage API key (free to obtain at [https://www.alphavantage.co/](https://www.alphavantage.co/))

## Setup Instructions

1. Clone the repository:

git clone https://github.com/laminmcamara/stock-market-dashboard.git
cd stock-market-dashboard

2. Set your Alpha Vantage API key as an environment variable:

- On Windows (Command Prompt):
  ```
  set ALPHA_VANTAGE_API_KEY=your_api_key_here
  ```
- On Windows (PowerShell):
  ```
  $env:ALPHA_VANTAGE_API_KEY="your_api_key_here"
  ```
- On Linux/macOS (bash):
  ```
  export ALPHA_VANTAGE_API_KEY=your_api_key_here
  ```

3. Build and run the application using Gradle:

gradle run

## Notes

- The application fetches data every 60 seconds to comply with Alpha Vantage's free tier rate limits (5 requests per minute).
- The assignment originally requested updates every 5 seconds; however, this interval exceeds the API limits and would cause errors.
- For real-time or more frequent updates, consider using a paid API plan or alternative data sources.
- The fetched data is stored in a queue with a maximum size of 100 entries.

## License
This project is for educational purposes.

## Contact
For questions or suggestions, please contact laminmasana@gmail.com.
