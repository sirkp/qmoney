
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  private RestTemplate restTemplate;
  private static final String TOKEN = "3a6d46f83d277a3671f6e8e291ceaf417fb826ae";


  public TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest
  private List<Candle> getCandles(String responseText) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return Arrays.asList(mapper.readValue(responseText, TiingoCandle[].class));
  }

  public List<Candle> getStockQuote(String symbol, LocalDate startDate, LocalDate endDate)
      throws JsonProcessingException, StockQuoteServiceException {

    if (startDate.isAfter(endDate)) {
      throw new RuntimeException("startDate is ahead of endDate");
    }

    String url = buildUri(symbol, startDate, endDate);
    String response = restTemplate.getForObject(url, String.class);
    try {
      List<Candle> candles = getCandles(response);
      return candles;
    } catch (JsonProcessingException e) {
      throw new StockQuoteServiceException(response);
    }
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
       
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    return uriTemplate.replace("$SYMBOL", symbol).replace("$STARTDATE", startDate.toString())
        .replace("$ENDDATE", endDate.toString()).replace("$APIKEY", TOKEN);
  }

  public RestTemplate getRestTemplate() {
    return restTemplate;
  }

  public void setRestTemplate(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }





  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //  1. Update the method signature to match the signature change in the interface.
  //     Start throwing new StockQuoteServiceException when you get some invalid response from
  //     Tiingo, or if Tiingo returns empty results for whatever reason, or you encounter
  //     a runtime exception during Json parsing.
  //  2. Make sure that the exception propagates all the way from
  //     PortfolioManager#calculateAnnualisedReturns so that the external user's of our API
  //     are able to explicitly handle this exception upfront.

  //CHECKSTYLE:OFF


}
