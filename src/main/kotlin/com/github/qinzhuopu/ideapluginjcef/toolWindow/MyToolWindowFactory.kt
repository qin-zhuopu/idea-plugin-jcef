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

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val url = "https://example.com/"
        val browser = JBCefBrowser.createBuilder().setUrl(url).setOffScreenRendering(false).build()

        val jsQuery = createJsQuery(browser)

        browser.jbCefClient.addLoadHandler(object : CefLoadHandler {
            override fun onLoadingStateChange(p0: CefBrowser?, p1: Boolean, p2: Boolean, p3: Boolean) {
                thisLogger().warn("onLoadingStateChange")
            }

            override fun onLoadStart(p0: CefBrowser?, p1: CefFrame?, p2: CefRequest.TransitionType?) {
                thisLogger().warn("onLoadStart")
            }

            override fun onLoadError(p0: CefBrowser?, p1: CefFrame?, p2: CefLoadHandler.ErrorCode?, p3: String?, p4: String?) {
                thisLogger().warn("onLoadError")
            }

            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                injectJavaScript(browser, jsQuery)
            }
        }, browser.cefBrowser)

        panel.add(browser.component)
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, null, false))
    }

    private fun createJsQuery(browser: JBCefBrowser): JBCefJSQuery {
        return JBCefJSQuery.create(browser as JBCefBrowserBase).apply {
            addHandler { message ->
                try {
                    val data = parseJson<Map<String, Any>>(message)
                    val methodName = data["methodName"] as? String ?: return@addHandler JBCefJSQuery.Response("Missing methodName", 400, message)
                    val params = data["params"] as? List<*> ?: emptyList<Any>()

                    val result = invokeMethod(methodName, params)
                    JBCefJSQuery.Response(result.toString())
                } catch (e: Exception) {
                    thisLogger().error("Error in JSQuery handler", e)
                    JBCefJSQuery.Response(null, 500, "Internal error: ${e.message}")
                }
            }
        }
    }

    private fun injectJavaScript(browser: CefBrowser?, jsQuery: JBCefJSQuery) {
        val script = """
    ide = new Proxy({}, {    get: (_, methodName) => (...params) => {
        const payload = JSON.stringify({ methodName, params });
        ${
            jsQuery.inject(
                "payload", "response => console.log('Success:', response)", "(errCode, errMsg) => console.error('Error:', errCode, errMsg)"
            )
        };
    }});
        """
        browser?.executeJavaScript(script, browser.url, 0)
    }

    private fun invokeMethod(methodName: String, params: List<*>): Any {
        return when (methodName) {
            "sayHello" -> sayHello(params.getOrNull(0) as? String ?: "Guest")
            "addNumbers" -> addNumbers(params.getOrNull(0) as? Double ?: 0.0, params.getOrNull(1) as? Double ?: 0.0)
            "divideNumbers" -> divideNumbers(params.getOrNull(0) as? Double ?: 0.0, params.getOrNull(1) as? Double ?: 1.0)
            else -> throw NoSuchMethodException("Method $methodName not found")
        }
    }

    private fun sayHello(name: String) = "Hello, $name!"

    private fun addNumbers(a: Double, b: Double) = a + b

    private fun divideNumbers(numerator: Double, denominator: Double): Double {
        if (denominator == 0.0) throw IllegalArgumentException("Division by zero")
        return numerator / denominator
    }

    private fun <T> parseJson(json: String): T {
        return Gson().fromJson(json, object : TypeToken<T>() {}.type)
    }
}
