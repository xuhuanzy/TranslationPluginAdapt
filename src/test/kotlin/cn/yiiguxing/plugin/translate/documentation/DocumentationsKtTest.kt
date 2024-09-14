package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.provider.IgnoredDocumentationElementProvider
import cn.yiiguxing.plugin.translate.trans.DocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.trans.google.tk
import org.jsoup.nodes.Element
import org.junit.Assert
import org.junit.Test

class DocumentationsKtTest {

    @Test
    fun testDocumentation() {
        val document = Documentations.parseDocumentation(
            """<html><head></head><body><div class='definition'><pre>[<a class="ext" title="JetBrains.Annotations.PureAttribute" href="psi_element://cref://M:JetBrains.Annotations.PureAttribute.#ctor"><span style="color:#c191ff">Pure</span></a>]<br /><span style="color:#6c95eb">public</span> <span style="color:#6c95eb">static </span><a title="System.Int32" href="psi_element://cref://T:System.Int32"><span style="color:#6c95eb">int</span></a> <b><span style="color:#39cc9b">Max</span></b>(<a title="System.Int32" href="psi_element://cref://T:System.Int32"><span style="color:#6c95eb">int</span></a>&nbsp;val1, <a title="System.Int32" href="psi_element://cref://T:System.Int32"><span style="color:#6c95eb">int</span></a>&nbsp;val2)
 in class <a title="System.Math" href="psi_element://cref://T:System.Math"><span style="color:#c191ff">System</span>.<span style="color:#c191ff">Math</span></a></pre></div><div class='content'>Returns the larger of two 32-bit signed integers.</div><table class="sections"><tr><td class="section" valign="top"><p>Params:</td><td class="section_body" valign="top"><p/><p><b>val1</b> &mdash; The first of two 32-bit signed integers to compare.<p/><p><b>val2</b> &mdash; The second of two 32-bit signed integers to compare.</td><tr><td class="section" valign="top"><p>Returns:</td><td class="section_body" valign="top"><p/><p/>Parameter val1 or val2, whichever is larger.</td></table><div class='content'><a href="https://docs.microsoft.com/en-us/dotnet/api/System.Math.Max?view=net-9.0">`Math.Max` on docs.microsoft.com</a></div></body></html>"""
        )
        var language = null;
        val body = document.body()
        val definition = body.selectFirst(".definition")
        val definitions = definition
            ?.previousElementSiblings()
            ?.toMutableList()
            ?.apply {
                reverse()
                add(definition)
                forEach { it.remove() }
            }


        val preElements = body.select("pre")
        preElements.forEachIndexed { index, element ->
            element.replaceWith(Element("pre").attr("id", index.toString()))
        }

        val ignoredElementProvider = language?.let { IgnoredDocumentationElementProvider.forLanguage(it) }
        val ignoredElements = ignoredElementProvider?.ignoreElements(body)

        val translatedBody = document.body()

        preElements.forEachIndexed { index, element ->
            translatedBody.selectFirst("""${"pre"}[id="$index"]""")?.replaceWith(element)
        }
        ignoredElements?.let { ignoredElementProvider.restoreIgnoredElements(translatedBody, it) }
        definitions?.let { translatedBody.prependChildren(it) }
        println(translatedBody)
    }

}