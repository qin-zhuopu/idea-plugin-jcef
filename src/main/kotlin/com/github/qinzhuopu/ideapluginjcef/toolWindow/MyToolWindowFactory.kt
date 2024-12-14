package com.github.qinzhuopu.ideapluginjcef.toolWindow

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
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

        val divideJsQuery = JBCefJSQuery.create(browser as JBCefBrowserBase) // 1
        divideJsQuery.addHandler { message ->
            thisLogger().warn("Received message: $message")
            try {
                val gson = Gson()
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val data: Map<String, Any> = gson.fromJson(message, type)

                val numerator = (data["numerator"] as? Number)?.toDouble() ?: throw IllegalArgumentException("Missing or invalid numerator")
                val denominator = (data["denominator"] as? Number)?.toDouble() ?: throw IllegalArgumentException("Missing or invalid denominator")

                if (denominator == 0.0) {
                    return@addHandler JBCefJSQuery.Response(null, 400, "Division by zero is not allowed")
                }

                val result = numerator / denominator
                return@addHandler JBCefJSQuery.Response(result.toString())

            } catch (e: Exception) {
                thisLogger().error("Error handling division: ${e.message}", e)
                return@addHandler JBCefJSQuery.Response(null, 500, "Internal error: ${e.message}")
            }
        }

        browser.jbCefClient.addLoadHandler(object : CefLoadHandler {
            override fun onLoadEnd(cefBrowser: CefBrowser?, p1: CefFrame?, p2: Int) {
                thisLogger().warn("onLoadEnd")
                cefBrowser?.executeJavaScript(
                    """
                window.divide = function(numerator, denominator) {
                    const json = JSON.stringify({ numerator: numerator, denominator: denominator });
                    ${
                        divideJsQuery.inject(
                            "json",
                            "function(response) { console.log('Success:', response); }",
                            "function(errCode, errMsg) { console.error('Error:', errCode, errMsg); }"
                        )
                    };
                };
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
