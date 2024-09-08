package org.mattshoe.shoebox.kduxdevtoolsplugin.ui

import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.DevToolsViewModel
import java.awt.BorderLayout
import javax.swing.JPanel

class DevToolsWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val composePanel = ComposePanel()
        val viewModel = DevToolsViewModel()
        composePanel.setContent {
            DevToolsScreen(viewModel)
        }

        val panel = JPanel().apply {
            layout = BorderLayout()
            add(composePanel, BorderLayout.CENTER)
        }

        val content = toolWindow.contentManager.factory.createContent(panel, "Kdux DevTools", false)
        toolWindow.contentManager.addContent(content)
    }
}