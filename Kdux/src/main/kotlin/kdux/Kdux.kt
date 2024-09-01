package kdux

import kdux.tools.PerformanceData
import org.mattshoe.shoebox.kdux.Store

/**
 * Configures global settings for the Kdux library using the provided configuration block.
 *
 * This function allows you to set up global behaviors that will be applied across all Kdux stores in your application.
 *
 * @param configure A lambda function that configures global settings using the [KduxMenu].
 */
fun kdux(configure: KduxMenu.() -> Unit) {
    KduxMenu().apply(configure)
}

class KduxMenu {
    companion object {
        internal val loggers = mutableListOf<(Any) -> Unit>()
        internal val performanceMonitors = mutableListOf<(PerformanceData<*>) -> Unit>()
        internal val globalGuards = mutableListOf<suspend (Any) -> Boolean>()
    }

    /**
     * Adds a global guard that blocks any action across the app when [isAuthorized] returns false.
     *
     * This will be invoked every time a `dispatch` operation is invoked on any [Store] in the application. As such,
     * the logic inside [isAuthorized] should be fast, as it will be invoked very rapidly.
     *
     * @param isAuthorized A suspend function that returns `true` if the action is authorized, or `false` if it should
     *     be blocked. The lambda parameter is the Action being requested to dispatch.
     */
    fun globalGuard(isAuthorized: suspend (Any) -> Boolean) {
        globalGuards.add(isAuthorized)
    }

    /**
     * Adds a global logger to track actions dispatched across all stores.
     *
     * This will be invoked every time a `dispatch` operation is invoked on any [Store] in the application. As such,
     * the logic inside [logger] should be fast, as it will be invoked very rapidly.
     *
     * @param logger A function that logs each action dispatched.
     */
    fun globalLogger(logger: (Any) -> Unit) {
        loggers.add(logger)
    }

    /**
     * Adds a global performance monitor to measure and log the performance of action dispatching.
     *
     * This will be invoked every time a `dispatch` operation is invoked on any [Store] in the application. As such,
     * the logic inside [monitor] should be fast, as it will be invoked very rapidly.
     *
     * @param monitor A function that logs performance data for each dispatched action.
     */
    fun globalPerformanceMonitor(monitor: (PerformanceData<*>) -> Unit) {
        performanceMonitors.add(monitor)
    }

    /**
     * Clears all pre-defined global behaviors.
     */
    fun clearGlobals() {
        loggers.clear()
        performanceMonitors.clear()
        globalGuards.clear()
    }

}