
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {
  
  private StockQuotesService stockQuotesService;
  private RestTemplate restTemplate;
  private static final String TOKEN = "3a6d46f83d277a3671f6e8e291ceaf417fb826ae";

  

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF

  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> trades,
      LocalDate endDate) throws StockQuoteServiceException {
    
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    try {
      for (PortfolioTrade trade: trades) {
        if (stockQuotesService != null) {
          List<Candle> candles = stockQuotesService.getStockQuote(
              trade.getSymbol(), trade.getPurchaseDate(), endDate);
    
          if (candles != null && candles.size() > 0) {
            annualizedReturns.add(calculateAnnualizedReturnForSingleStock(endDate, trade,
                candles.get(0).getOpen(), candles.get(candles.size() - 1).getClose()));
          }
        }
      }
      annualizedReturns.sort(getComparator());
    } catch (RuntimeException e) {
        throw new RuntimeException();
    } catch (JsonProcessingException e ) {
        e.printStackTrace();
    } finally {
        return annualizedReturns;
    }
  }



  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate startDate, LocalDate endDate)
      throws StockQuoteServiceException {
    
    if (startDate.isAfter(endDate)) {
      throw new RuntimeException("startDate is ahead of endDate");
    }
    try {
      String url = buildUri(symbol, startDate, endDate);
      return Arrays.asList(restTemplate.getForObject(url, TiingoCandle[].class));
    } catch (Exception e) {
      throw new StockQuoteServiceException(e.getMessage());
    }
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
       
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    return uriTemplate.replace("$SYMBOL", symbol).replace("$STARTDATE", startDate.toString())
        .replace("$ENDDATE", endDate.toString()).replace("$APIKEY", TOKEN);
  }

  public static AnnualizedReturn calculateAnnualizedReturnForSingleStock(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    
    LocalDate startDate = trade.getPurchaseDate();  
    double years = (ChronoUnit.DAYS.between(startDate, endDate)) / (double) 365;
    double totalReturn = (sellPrice - buyPrice) / buyPrice;
    double annualizedReturn = Math.pow((1 + totalReturn), ((double)1 / years)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }

  public RestTemplate getRestTemplate() {
    return restTemplate;
  }

  public void setRestTemplate(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public StockQuotesService getStockQuotesService() {
    return stockQuotesService;
  }

  public void setStockQuotesService(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> trades,
      LocalDate endDate, int numThreads) throws StockQuoteServiceException {

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<Future<AnnualizedReturn>> resultList = new ArrayList<>();
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    try {
      for(PortfolioTrade trade: trades) {
        AnnualizedReturnCallable callable = new 
            AnnualizedReturnCallable(this.stockQuotesService, trade, endDate);
        Future<AnnualizedReturn> result = executor.submit(callable);
        resultList.add(result);
      }
  
      for(Future<AnnualizedReturn> result: resultList) {
        AnnualizedReturn annualizedReturn = result.get();
        if(annualizedReturn != null) {
          annualizedReturns.add(annualizedReturn);
        } 
      }
      annualizedReturns.sort(getComparator());
      return annualizedReturns;
    } catch (RuntimeException e) {
        throw new RuntimeException();
    } catch (InterruptedException | ExecutionException e) {
        throw new StockQuoteServiceException(e.getMessage());
    } finally {
      executor.shutdown();
    }
  }
}
