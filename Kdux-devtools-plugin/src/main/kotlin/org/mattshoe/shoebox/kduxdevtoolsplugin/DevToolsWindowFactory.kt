package org.mattshoe.shoebox.kduxdevtoolsplugin

import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import java.awt.BorderLayout
import javax.swing.JPanel

class DevToolsWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val composePanel = ComposePanel()
        composePanel.setContent {
            DevToolsScreen()
        }

        val panel = JPanel().apply {
            layout = BorderLayout()
            add(composePanel, BorderLayout.CENTER)
        }

        val content = toolWindow.contentManager.factory.createContent(panel, "Kdux DevTools", false)
        toolWindow.contentManager.addContent(content)
    }
}