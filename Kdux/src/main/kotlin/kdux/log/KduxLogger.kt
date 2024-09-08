package kdux.log

internal object Logger {
    private var _logger: KduxLogger = KduxLoggerImpl()
    fun get(): KduxLogger = _logger
    fun set(logger: KduxLogger) {
        _logger = logger
    }
}

interface KduxLogger {
    fun i(msg: String?)
    fun d(msg: String?)
    fun w(msg: String?)
    fun e(msg: String?, e: Throwable)
}

class KduxLoggerImpl: KduxLogger {
    override fun i(msg: String?) {
        println("KDUX::info  -- $msg")
    }

    override fun d(msg: String?) {
        println("KDUX::debug  -- $msg")
    }

    override fun w(msg: String?) {
        println("KDUX::warning  -- $msg")
    }

    override fun e(msg: String?, e: Throwable) {
        println("KDUX::error  -- $msg: \n\t${e.stackTrace.joinToString("\n\t") { it.toString() }}")
    }
}