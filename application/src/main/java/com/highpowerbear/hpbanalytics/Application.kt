package com.highpowerbear.hpbanalytics

import org.springframework.boot.autoconfigure.SpringBootApplication
import kotlin.jvm.JvmStatic
import org.springframework.boot.builder.SpringApplicationBuilder

/**
 * Created by robertk on 12/24/2017.
 */
@SpringBootApplication
open class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder()
                .sources(Application::class.java, CoreApplication::class.java)
                .run(*args)
        }
    }
}