package net.cofcool.toolbox.simplenote

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.jackson.DatabindCodec

object Config {
    const val PORT_KEY = "port"
    const val PORT_VAL = 8888

    const val PATH_KEY = "filepath"
    const val PATH_VAL = "/tmp"

    const val FILE_KEY = "filename"
    const val FILE_NAME = "notes.json"

    const val NOTE_SERVICE = "NOTE_SERVICE"

    const val STARTED = "STARTED"
    const val LIST = "LIST"
    const val SAVE = "SAVE"
    const val DELETE = "DELETE"
    const val DIRTY = "DIRTY"

    val objectMapper: ObjectMapper =  DatabindCodec.mapper()

    fun init() {
        objectMapper.registerModule(JavaTimeModule())
    }

    fun <T> toPojoList(buffer: Buffer, clazz: Class<T>): List<T> {
        return objectMapper.readValue(buffer.bytes, objectMapper.typeFactory.constructCollectionType(List::class.java, clazz))
    }
}