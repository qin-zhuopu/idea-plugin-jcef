package com.github.qinzhuopu.ideapluginjcef.toolWindow

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.network.CefRequest
import javax.swing.BoxLayout


class MyToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val url = "https://example.com/"
        val browser = JBCefBrowser.createBuilder().setUrl(url).setOffScreenRendering(false).build()
        browser.jbCefClient.addLoadHandler(object : CefLoadHandler {
            override fun onLoadEnd(cefBrowser: CefBrowser?, p1: CefFrame?, p2: Int) {
                thisLogger().warn("onLoadEnd")
                cefBrowser?.executeJavaScript(
                    """
                        let now = new Date();
                        setInterval(() => {
                          console.log(now);
                        }, 2000);
                    """, url, 0
                )
            }

            override fun onLoadingStateChange(p0: CefBrowser?, p1: Boolean, p2: Boolean, p3: Boolean) {
                thisLogger().warn("onLoadingStateChange")
            }

            override fun onLoadStart(p0: CefBrowser?, p1: CefFrame?, p2: CefRequest.TransitionType?) {
                thisLogger().warn("onLoadStart")
            }

            override fun onLoadError(p0: CefBrowser?, p1: CefFrame?, p2: CefLoadHandler.ErrorCode?, p3: String?, p4: String?) {
                thisLogger().warn("onLoadError")
            }
        }, browser.cefBrowser)
        myPanel.add(browser.component)

        val content = ContentFactory.getInstance().createContent(myPanel, null, false)
        toolWindow.contentManager.addContent(content)
    }

}
