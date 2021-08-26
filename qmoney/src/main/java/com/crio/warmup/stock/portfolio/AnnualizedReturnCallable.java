package com.crio.warmup.stock.portfolio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;

public class AnnualizedReturnCallable implements Callable<AnnualizedReturn> {
    private StockQuotesService stockQuotesService;
    private PortfolioTrade trade;
    private LocalDate endDate;

    AnnualizedReturnCallable(StockQuotesService stockQuotesService,
            PortfolioTrade trade, LocalDate endDate) {
        this.stockQuotesService = stockQuotesService;
        this.trade = trade;
        this.endDate = endDate; 
        
    }

    @Override
    public AnnualizedReturn call() throws JsonProcessingException
    , StockQuoteServiceException, Exception {

        List<Candle> candles = stockQuotesService.getStockQuote(
            trade.getSymbol(), trade.getPurchaseDate(), endDate);

        if (candles != null && candles.size() > 0) {
            return PortfolioManagerImpl.calculateAnnualizedReturnForSingleStock(endDate, trade,
                candles.get(0).getOpen(), candles.get(candles.size() - 1).getClose());
        } else {
            return null;
        }
        
    }
}
