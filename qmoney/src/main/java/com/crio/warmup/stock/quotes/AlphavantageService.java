
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement the StockQuoteService interface as per the contracts. Call Alphavantage service
  //  to fetch daily adjusted data for last 20 years.
  //  Refer to documentation here: https://www.alphavantage.co/documentation/
  //  --
  //  The implementation of this functions will be doing following tasks:
  //    1. Build the appropriate url to communicate with third-party.
  //       The url should consider startDate and endDate if it is supported by the provider.
  //    2. Perform third-party communication with the url prepared in step#1
  //    3. Map the response and convert the same to List<Candle>
  //    4. If the provider does not support startDate and endDate, then the implementation
  //       should also filter the dates based on startDate and endDate. Make sure that
  //       result contains the records for for startDate and endDate after filtering.
  //    5. Return a sorted List<Candle> sorted ascending based on Candle#getDate
  // Note:
  // 1. Make sure you use {RestTemplate#getForObject(URI, String)} else the test will fail.
  // 2. Run the tests using command below and make sure it passes:
  //    ./gradlew test --tests AlphavantageServiceTest
  //CHECKSTYLE:OFF
  //CHECKSTYLE:ON
  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  1. Write a method to create appropriate url to call Alphavantage service. The method should
  //     be using configurations provided in the {@link @application.properties}.
  //  2. Use this method in #getStockQuote.


  private RestTemplate restTemplate;
  private static final String TOKEN = "IKZSGWLV5XCN1J6I";
  private static final String URITEMPLATE = "https://www.alphavantage.co/query"
      + "?function=TIME_SERIES_DAILY_ADJUSTED&symbol=$SYMBOL&apikey=$APIKEY";

  public AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
  
  protected String buildUri(String symbol) {  
    return URITEMPLATE.replace("$SYMBOL", symbol).replace("$APIKEY", TOKEN);
  }

  private List<Candle> getCandlesListFromMap(LocalDate startDate, LocalDate endDate,
      Map<LocalDate, AlphavantageCandle> map) {

    List<Candle> candles = new ArrayList<>();
    AlphavantageCandle candle = null;
    LocalDate currentDate = startDate;
    try {
      while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
        if (map.containsKey(currentDate)) {
          candle = map.get(currentDate);
          candle.setDate(currentDate);
          candles.add(candle);
        }
        currentDate = currentDate.plusDays(1);
      }
      return candles;
    } catch (NullPointerException e) {
      throw new NullPointerException("map is null");
    }
  }

  private List<Candle> getCandles(String response, LocalDate startDate, LocalDate endDate)
      throws JsonProcessingException, StockQuoteServiceException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    try {
      AlphavantageDailyResponse alphavantageDailyResponse = mapper.readValue(response,
          AlphavantageDailyResponse.class);
      return getCandlesListFromMap(startDate, endDate, alphavantageDailyResponse.getCandles());
    } catch (NullPointerException | JsonProcessingException e) {
      throw new StockQuoteServiceException(response);
    }
  }


  public List<Candle> getStockQuote(String symbol, LocalDate startDate, LocalDate endDate)
      throws JsonProcessingException, StockQuoteServiceException {

    if (startDate.isAfter(endDate)) {
      throw new RuntimeException("startDate is ahead of endDate");
    }

    String url = buildUri(symbol);
    String response = restTemplate.getForObject(url, String.class);
    return getCandles(response, startDate, endDate);
  }

  public RestTemplate getRestTemplate() {
    return restTemplate;
  }

  public void setRestTemplate(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //   1. Update the method signature to match the signature change in the interface.
  //   2. Start throwing new StockQuoteServiceException when you get some invalid response from
  //      Alphavantage, or you encounter a runtime exception during Json parsing.
  //   3. Make sure that the exception propagates all the way from PortfolioManager, so that the
  //      external user's of our API are able to explicitly handle this exception upfront.
  //CHECKSTYLE:OFF

}

