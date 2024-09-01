package kdux

import kdux.tools.PerformanceData

internal object KduxGlobal {

    val loggers = mutableListOf<(Any) -> Unit>()
    val performanceMonitors = mutableListOf<(PerformanceData<*>) -> Unit>()


    fun globalLogger(logger: (Any) -> Unit) {
        loggers.add(logger)
    }

    fun globalPerformanceMonitor(monitor: (PerformanceData<*>) -> Unit) {
        performanceMonitors.add(monitor)
    }
}

