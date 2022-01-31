package com.highpowerbear.hpbanalytics.service;

import com.highpowerbear.hpbanalytics.common.ExchangeRateMapper;
import com.highpowerbear.hpbanalytics.common.HanUtil;
import com.highpowerbear.hpbanalytics.config.ApplicationProperties;
import com.highpowerbear.hpbanalytics.config.HanSettings;
import com.highpowerbear.hpbanalytics.database.ExchangeRate;
import com.highpowerbear.hpbanalytics.database.ExchangeRateRepository;
import com.highpowerbear.hpbanalytics.enums.Currency;
import com.highpowerbear.shared.ExchangeRateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
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

        retrieveExchangeRates();
        putLastExchangeRate(); // in case of map empty and retrieve fails, to make sure we have at least one entry in the map
    }

    @Override
    public void performStartOfDayTasks() {
        retrieveExchangeRates();
    }

    private void retrieveExchangeRates() {
        log.info("BEGIN ExchangeRateRetriever.retrieve");

        String url = applicationProperties.getEcbExchangeRateUrl();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        String content = response.getBody();

        if (content == null) {
            log.error("unable to retrieve exchange rates");
            return;
        }
        Node timeNode;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));

            timeNode = ((NodeList) XPathFactory.newInstance().newXPath().compile("//Cube[@time]")
                    .evaluate(doc, XPathConstants.NODESET))
                    .item(0);

        } catch (Exception e) {
            log.error(e.toString());
            return;
        }

        LocalDate exchangeRateDate = LocalDate.parse(timeNode.getAttributes().item(0).getTextContent());
        log.info("retrieved exchange rate for " + exchangeRateDate);

        ExchangeRate exchangeRate = new ExchangeRate()
                .setDate(exchangeRateDate.toString());

        NodeList rateNodes = timeNode.getChildNodes();
        for (int i = 0; i < rateNodes.getLength(); i ++) {
            Node rateNode = rateNodes.item(i);

            if (rateNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Currency currency = Currency.findByValue(rateNode.getAttributes().item(0).getTextContent());
            Double rate = Double.parseDouble(rateNode.getAttributes().item(1).getTextContent());

            if (currency == null) {
                continue;
            }
            switch(currency) {
                case USD:
                    exchangeRate.setEurUsd(rate);
                    break;
                case AUD:
                    exchangeRate.setEurAud(rate);
                    break;
                case GBP:
                    exchangeRate.setEurGbp(rate);
                    break;
                case CHF:
                    exchangeRate.setEurChf(rate);
                    break;
                case JPY:
                    exchangeRate.setEurJpy(rate);
                    break;
                case KRW:
                    exchangeRate.setEurKrw(rate);
                    break;
                case HKD:
                    exchangeRate.setEurHkd(rate);
                    break;
                case SGD:
                    exchangeRate.setEurSgd(rate);
                    break;
            }
        }
        exchangeRate = exchangeRateRepository.save(exchangeRate);
        exchangeRateMap.put(exchangeRateDate.toString(), exchangeRateMapper.entityToDto(exchangeRate));

        // create a few exchange rates with future dates to cover the weekends and holidays, will get overwritten for the working days
        for (int d = 1; d <= HanSettings.NUMBER_OF_FUTURE_EXCHANGE_RATES; d++) {
            LocalDate futureDate = exchangeRateDate.plusDays(d);
            exchangeRate.setDate(futureDate.toString());

            exchangeRate = exchangeRateRepository.save(exchangeRate);
            exchangeRateMap.put(exchangeRateDate.toString(), exchangeRateMapper.entityToDto(exchangeRate));
        }
        log.info("END ExchangeRateRetriever.retrieve");
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
