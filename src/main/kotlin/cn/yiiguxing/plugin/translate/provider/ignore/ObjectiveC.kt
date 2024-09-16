package cn.yiiguxing.plugin.translate.provider.ignore;

import cn.yiiguxing.plugin.translate.provider.IgnoredDocumentationElementProvider
import org.jsoup.nodes.Element

class ObjectiveC : IgnoredDocumentationElementProvider {

    override fun ignoreElements(body: Element): List<Element> {
        val ignoredElement = body.select( """tr > td > b""")
        ignoredElement.forEachIndexed { index, element ->
            element.replaceWith(
                Element("ObjectiveCIgnore").attr("id", index.toString())
            )
        }
        return ignoredElement
    }

    override fun restoreIgnoredElements(body: Element, ignoredElements: List<Element>) {
        ignoredElements.forEachIndexed { index, element ->
            body.selectFirst("""ObjectiveCIgnore[id="$index"]""")?.replaceWith(element)
        }
    }

}