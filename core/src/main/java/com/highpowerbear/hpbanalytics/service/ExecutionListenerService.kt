package com.highpowerbear.hpbanalytics.service

import com.hazelcast.core.HazelcastInstanceNotActiveException
import com.highpowerbear.hpbanalytics.common.ExecutionMapper
import com.highpowerbear.hpbanalytics.common.HanUtil
import com.highpowerbear.hpbanalytics.config.HanSettings
import com.highpowerbear.shared.ExecutionDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by robertk on 10/23/2020.
 */
@Service
class ExecutionListenerService @Autowired constructor(
    private val executionMapper: ExecutionMapper,
    private val analyticsService: AnalyticsService,
    private val statisticsService: StatisticsService,
    private val executionQueue: BlockingQueue<ExecutionDTO>,
    private val executorService: ScheduledExecutorService) : InitializingService {

    private val hazelcastConsumerRunning = AtomicBoolean(true)
    init {
        Runtime.getRuntime().addShutdownHook(Thread { shutdown() }) // shutdown hook
    }

    override fun initialize() {
        log.info("initializing HazelcastService")
        executorService.schedule({ startHazelcastConsumer() }, HanSettings.HAZELCAST_CONSUMER_START_DELAY_SECONDS.toLong(), TimeUnit.SECONDS)
    }

    private fun startHazelcastConsumer() {
        log.info("starting hazelcast consumer")
        while (hazelcastConsumerRunning.get()) {
            try {
                val dto = executionQueue.take()
                val execution = executionMapper.dtoToEntity(dto)
                execution.symbol = HanUtil.removeWhiteSpaces(execution.symbol)
                log.info("consumed execution from the hazelcast queue $execution")
                analyticsService.addExecution(execution)
                statisticsService.calculateCurrentStatisticsOnExecution(execution)
            } catch (he: HazelcastInstanceNotActiveException) {
                log.error(he.message + " ... stopping hazelcast consumer task")
                hazelcastConsumerRunning.set(false)
            } catch (e: Exception) {
                log.error("hazelcast consumer task exception caught: " + e.message)
            }
        }
        log.info("hazelcast consumer task exit")
    }

    private fun shutdown() {
        executorService.shutdown()
        hazelcastConsumerRunning.set(false)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExecutionListenerService::class.java)
    }
}