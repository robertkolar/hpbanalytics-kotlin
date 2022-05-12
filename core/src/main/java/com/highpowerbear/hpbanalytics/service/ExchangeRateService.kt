package com.highpowerbear.hpbanalytics.service

import com.highpowerbear.hpbanalytics.common.ExchangeRateMapper
import com.highpowerbear.hpbanalytics.common.HanUtil
import com.highpowerbear.hpbanalytics.config.ApplicationProperties
import com.highpowerbear.hpbanalytics.config.HanSettings
import com.highpowerbear.hpbanalytics.database.ExchangeRate
import com.highpowerbear.hpbanalytics.database.ExchangeRateRepository
import com.highpowerbear.hpbanalytics.enums.Currency
import com.highpowerbear.shared.ExchangeRateDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.math.BigDecimal
import java.time.LocalDate
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * Created by robertk on 10/10/2016.
 */
@Service
class ExchangeRateService @Autowired constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val exchangeRateMapper: ExchangeRateMapper,
    private val applicationProperties: ApplicationProperties,
    private val exchangeRateMap: MutableMap<String, ExchangeRateDTO?>
) : ScheduledTaskPerformer {
    private val restTemplate = RestTemplate()

    init {
        retrieveExchangeRates()
        putLastExchangeRate() // in case of map empty and retrieve fails, to make sure we have at least one entry in the map
    }

    override fun performStartOfDayTasks() {
        retrieveExchangeRates()
    }

    private fun retrieveExchangeRates() {
        log.info("BEGIN ExchangeRateRetriever.retrieve")
        val url = applicationProperties.ecbExchangeRateUrl
        val response = restTemplate.getForEntity(url, String::class.java)
        val content = response.body
        if (content == null) {
            log.error("unable to retrieve exchange rates")
            return
        }
        val timeNode: Node
        timeNode = try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(content)))
            (XPathFactory.newInstance().newXPath().compile("//Cube[@time]")
                .evaluate(doc, XPathConstants.NODESET) as NodeList)
                .item(0)
        } catch (e: Exception) {
            log.error(e.toString())
            return
        }
        val exchangeRateDate = LocalDate.parse(timeNode.attributes.item(0).textContent)
        log.info("retrieved exchange rate for $exchangeRateDate")
        var exchangeRate = ExchangeRate().apply {
            date = exchangeRateDate.toString()
        }
        val rateNodes = timeNode.childNodes
        for (i in 0 until rateNodes.length) {
            val rateNode = rateNodes.item(i)
            if (rateNode.nodeType != Node.ELEMENT_NODE) {
                continue
            }
            val currency = Currency.findByValue(rateNode.attributes.item(0).textContent)
            val rate = rateNode.attributes.item(1).textContent.toDouble()
            if (currency == null) {
                continue
            }
            when (currency) {
                Currency.USD -> exchangeRate.eurUsd = rate
                Currency.AUD -> exchangeRate.eurAud = rate
                Currency.GBP -> exchangeRate.eurGbp = rate
                Currency.CHF -> exchangeRate.eurChf = rate
                Currency.JPY -> exchangeRate.eurJpy = rate
                Currency.KRW -> exchangeRate.eurKrw = rate
                Currency.HKD -> exchangeRate.eurHkd = rate
                Currency.SGD -> exchangeRate.eurSgd = rate
                Currency.EUR -> TODO()
            }
        }
        exchangeRate = exchangeRateRepository.save(exchangeRate)
        exchangeRateMap[exchangeRateDate.toString()] = exchangeRateMapper.entityToDto(exchangeRate)

        // create a few exchange rates with future dates to cover the weekends and holidays, will get overwritten for the working days
        for (d in 1..HanSettings.NUMBER_OF_FUTURE_EXCHANGE_RATES) {
            val futureDate = exchangeRateDate.plusDays(d.toLong())
            exchangeRate.date = futureDate.toString()
            exchangeRate = exchangeRateRepository.save(exchangeRate)
            exchangeRateMap[futureDate.toString()] = exchangeRateMapper.entityToDto(exchangeRate)
        }
        log.info("END ExchangeRateRetriever.retrieve")
    }

    fun getExchangeRate(localDate: LocalDate, currency: Currency): BigDecimal {
        val date = HanUtil.formatExchangeRateDate(localDate)
        var dto = getExchangeRateDTO(date)
        if (dto == null) {
            val previousDate = HanUtil.formatExchangeRateDate(localDate.minusDays(1))
            dto = getExchangeRateDTO(previousDate)
            checkNotNull(dto) { "exchange rate not available for $date or $previousDate" }
        }
        val rate = dto.getRate(HanSettings.PORTFOLIO_BASE_CURRENCY.name, currency.name)
        return BigDecimal.valueOf(rate)
    }

    private fun putLastExchangeRate() {
        val list = exchangeRateRepository.findFirstByOrderByDateDesc()
        if (list.isNotEmpty()) {
            val lastExchangeRate = list[0]
            exchangeRateMap[lastExchangeRate.date] = exchangeRateMapper.entityToDto(lastExchangeRate)
            log.info("last exchange rate found for " + lastExchangeRate.date)
        }
    }

    private fun getExchangeRateDTO(date: String): ExchangeRateDTO? {
        val map = exchangeRateMap
        if (map[date] == null) {
            exchangeRateRepository.findById(date)
                .ifPresent { entity: ExchangeRate -> map[date] = exchangeRateMapper.entityToDto(entity) }
        }
        return map[date]
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExchangeRateService::class.java)
    }
}