package com.highpowerbear.hpbanalytics.service;

import com.highpowerbear.hpbanalytics.common.ExchangeRateMapper;
import com.highpowerbear.hpbanalytics.common.HanUtil;
import com.highpowerbear.hpbanalytics.config.ApplicationProperties;
import com.highpowerbear.hpbanalytics.config.HanSettings;
import com.highpowerbear.hpbanalytics.database.ExchangeRate;
import com.highpowerbear.hpbanalytics.database.ExchangeRateRepository;
import com.highpowerbear.hpbanalytics.enums.Currency;
import com.highpowerbear.hpbanalytics.model.ExchangeRates;
import com.highpowerbear.shared.ExchangeRateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Created by robertk on 10/10/2016.
 */
@Service
public class ExchangeRateService implements ScheduledTaskPerformer {
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateMapper exchangeRateMapper;
    private final ApplicationProperties applicationProperties;
    private final Map<String, ExchangeRateDTO> exchangeRateMap;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository,
                               ExchangeRateMapper exchangeRateMapper,
                               ApplicationProperties applicationProperties,
                               Map<String, ExchangeRateDTO> exchangeRateMap) {

        this.exchangeRateRepository = exchangeRateRepository;
        this.exchangeRateMapper = exchangeRateMapper;
        this.applicationProperties = applicationProperties;
        this.exchangeRateMap = exchangeRateMap;

        putLastExchangeRate();
    }

    @Override
    public void performStartOfDayTasks() {
        retrieveExchangeRates();
    }

    private void retrieveExchangeRates() {
        log.info("BEGIN ExchangeRateRetriever.retrieve");
        final int daysBack = applicationProperties.getFixer().getDaysBack();

        for (int i = 0; i < daysBack; i++) {
            LocalDate localDate = LocalDate.now().plusDays(i - daysBack);
            String date = HanUtil.formatExchangeRateDate(localDate);

            ExchangeRates exchangeRates = retrieve(date);

            ExchangeRate exchangeRate = new ExchangeRate()
                    .setDate(date)
                    .setEurUsd(exchangeRates.getRate(Currency.USD))
                    .setEurGbp(exchangeRates.getRate(Currency.GBP))
                    .setEurChf(exchangeRates.getRate(Currency.CHF))
                    .setEurAud(exchangeRates.getRate(Currency.AUD))
                    .setEurJpy(exchangeRates.getRate(Currency.JPY))
                    .setEurKrw(exchangeRates.getRate(Currency.KRW))
                    .setEurHkd(exchangeRates.getRate(Currency.HKD))
                    .setEurSgd(exchangeRates.getRate(Currency.SGD));

            exchangeRateRepository.save(exchangeRate);
            exchangeRateMap.put(date, exchangeRateMapper.entityToDto(exchangeRate));
        }

        log.info("END ExchangeRateRetriever.retrieve");
    }

    private ExchangeRates retrieve(String date) {
        String fixerUrl = applicationProperties.getFixer().getUrl();
        String fixerAccessKey = applicationProperties.getFixer().getAccessKey();
        String fixerSymbols = applicationProperties.getFixer().getSymbols();

        String query = fixerUrl + "/" + date + "?access_key=" + fixerAccessKey + "&symbols=" + fixerSymbols;
        ExchangeRates exchangeRates = restTemplate.getForObject(query, ExchangeRates.class);

        log.info("retrieved exchange rates " + exchangeRates);
        return exchangeRates;
    }

    public BigDecimal getExchangeRate(LocalDate localDate, Currency currency) {

        String date = HanUtil.formatExchangeRateDate(localDate);
        ExchangeRateDTO dto = getExchangeRateDTO(date);

        if (dto == null) {
            String previousDate = HanUtil.formatExchangeRateDate(localDate.minusDays(1));

            dto = getExchangeRateDTO(previousDate);
            if (dto == null) {
                throw new IllegalStateException("exchange rate not available for " + date + " or " + previousDate);
            }
        }
        double rate = dto.getRate(HanSettings.PORTFOLIO_BASE_CURRENCY.name(), currency.name());
        return BigDecimal.valueOf(rate);
    }

    private void putLastExchangeRate() {
        List<ExchangeRate> list = exchangeRateRepository.findFirstByOrderByDateDesc();

        if (!list.isEmpty()) {
            ExchangeRate lastExchangeRate = list.get(0);
            exchangeRateMap.put(lastExchangeRate.getDate(), exchangeRateMapper.entityToDto(lastExchangeRate));
            log.info("last exchange rate found for " + lastExchangeRate.getDate());
        }
    }

    private ExchangeRateDTO getExchangeRateDTO(String date) {
        Map<String, ExchangeRateDTO> map = exchangeRateMap;

        if (map.get(date) == null) {
            exchangeRateRepository.findById(date).ifPresent(entity -> map.put(date, exchangeRateMapper.entityToDto(entity)));
        }
        return map.get(date);
    }
}
