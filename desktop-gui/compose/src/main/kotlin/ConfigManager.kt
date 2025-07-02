import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import view.viewLogger
import java.io.File

object ConfigManager {
    private val defaultConfig = AppConfig(webAddr = "http://127.0.0.1", 38080)
    private var config: AppConfig
    private val configFile = File(System.getProperty("user.home"), ".mytool/gui-config.json")

    init {
        configFile.parentFile?.mkdirs()
        config = loadConfig()?:defaultConfig
    }

    fun saveConfig(c: AppConfig) {
        config = c
        configFile.writeText(globalJson.encodeToString(c))
    }

    fun requestUrl() = config.webAddr + ":" + config.webPort

    fun config() = config

    private fun loadConfig(): AppConfig? {
        return if (configFile.exists()) {
            try {
                val text = configFile.readText()
                globalJson.decodeFromString(text)
            } catch (e: Exception) {
                viewLogger.error("Load config file error", e)
                null
            }
        } else {
            null
        }
    }

    fun deleteConfig() {
        configFile.delete()
    }

    fun configPath(): String = configFile.absolutePath

    fun needLocalServer(): Boolean {
        return config == defaultConfig
    }
}

@Serializable
data class AppConfig(
    val webAddr: String,
    val webPort: Int
)