package request

import kotlinx.serialization.Serializable

enum class Tools(val tool: Tools?, val cmd: String) {
    C(null, "converts"),
    C_MD5(C, "md5"), C_NOW(C, "now"), C_HDATE(C, "hdate");

}

@Serializable
data class Params(val cmd: String, val input: String)

@Serializable
data class CommandItem(val id: String, val cmd: String, val tags: List<String>?)