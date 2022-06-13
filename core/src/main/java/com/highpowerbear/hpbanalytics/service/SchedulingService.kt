package com.highpowerbear.hpbanalytics.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.function.Consumer

/**
 * Created by robertk on 11/1/2020.
 */
@Service
class SchedulingService @Autowired constructor(
    private val scheduledTaskPerformers: List<ScheduledTaskPerformer>) {

    @Scheduled(cron = "0 0 1 * * *")
    private fun performStartOfDayTasks() {
        scheduledTaskPerformers.forEach(Consumer { obj: ScheduledTaskPerformer -> obj.performStartOfDayTasks() })
    }
}