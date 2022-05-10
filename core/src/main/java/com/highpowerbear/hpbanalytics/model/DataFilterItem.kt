package com.highpowerbear.hpbanalytics.model

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.JsonNode
import java.util.ArrayList
import java.util.function.Consumer

/**
 * Created by robertk on 10/4/2020.
 */
class DataFilterItem {
    val property: String? = null
    val operator: String? = null
    var value: String? = null
    var doubleValue: Double? = null

    private val values: MutableList<String> = ArrayList()
    @JsonSetter("value")
    fun setValueNode(valueNode: JsonNode) {
        if (valueNode.isTextual) {
            value = valueNode.asText()
        } else if (valueNode.isNumber) {
            doubleValue = valueNode.asDouble()
        } else if (valueNode.isArray) {
            valueNode.forEach(Consumer { item: JsonNode -> values.add(item.asText()) })
        }
    }

    fun getValues(): List<String> {
        return values
    }
}