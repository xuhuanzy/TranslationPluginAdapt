package cn.yiiguxing.plugin.translate.provider.ignore;

import cn.yiiguxing.plugin.translate.provider.IgnoredDocumentationElementProvider
import org.jsoup.nodes.Element

class Kotlin : IgnoredDocumentationElementProvider {

    override fun ignoreElements(body: Element): List<Element> {
        val ignoredElement = body.select( """.member-signature""")
        ignoredElement.forEachIndexed { index, element ->
            element.replaceWith(
                Element("memberSignatureIgnore").attr("id", index.toString())
            )
        }
        return ignoredElement
    }

    override fun restoreIgnoredElements(body: Element, ignoredElements: List<Element>) {
        ignoredElements.forEachIndexed { index, element ->
            body.selectFirst("""memberSignatureIgnore[id="$index"]""")?.replaceWith(element)
        }
    }

}