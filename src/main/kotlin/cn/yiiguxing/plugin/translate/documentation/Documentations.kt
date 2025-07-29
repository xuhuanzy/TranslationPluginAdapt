package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.provider.IgnoredDocumentationElementProvider
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.scaled
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.lang.Language
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.JBUI
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.awt.Color
import java.io.StringReader
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

// 枚举
enum class TranslateType {
    // 快速文档
    QuickDocumentation,

    // Rider专属的, 在代码建议时弹出的文Api单项
    RiderSummaryItem
}

/**
 * Help class that provide document operations.
 */
internal object Documentations {

    /**
     * Parses the specified [documentation] string.
     */
    fun parseDocumentation(documentation: String): Document = Jsoup.parse(documentation)

    /**
     * Returns the documentation string of the specified [documentation] object.
     *
     * @param prettyPrint Enable or disable pretty printing.
     */
    fun getDocumentationString(documentation: Document, prettyPrint: Boolean = false): String {
        documentation.outputSettings().prettyPrint(prettyPrint)
        return documentation.outerHtml().fixHtml()
    }

    /**
     * Adds the specified inline [message] to the [documentation].
     */
    fun addMessage(documentation: String, message: String, color: Color): String {
        return parseDocumentation(documentation)
            .addMessage(message, color)
            .documentationString
    }

}


private const val CSS_QUERY_DEFINITION = ".definition"
private const val CSS_QUERY_CONTENT = ".content"

private const val TAG_PRE = "pre"
private const val ATTR_TRANSLATED = "translated"

private const val FIX_HTML_CLASS_EXPRESSION_REPLACEMENT = "<${'$'}{tag} class='${'$'}{class}'>"
private val fixHtmlClassExpressionRegex = Regex("""<(?<tag>.+?) class="(?<class>.+?)">""")


/**
 * Documentation string of this [Document].
 */
internal val Document.documentationString: String
    get() = Documentations.getDocumentationString(this, false)


internal fun Translator.getTranslatedDocumentation(
    documentation: String,
    language: Language?,
    type: TranslateType
): String {
    val document: Document = Documentations.parseDocumentation(documentation)
    if (document.body().hasAttr(ATTR_TRANSLATED)) {
        return documentation
    }

    val translatedDocumentation = try {
        if (this is DocumentationTranslator) {
            if (type == TranslateType.RiderSummaryItem) {
                getRiderRiderSummaryTranslatedDocumentation(document)
            } else {
                getTranslatedDocumentation(document, language)
            }
        } else {
            getTranslatedDocumentation(document)
        }
    } catch (e: ContentLengthLimitException) {
        document.addLimitHint()
    } catch (e: TranslateException) {
        if (e.cause is ContentLengthLimitException) {
            document.addLimitHint()
        } else {
            throw e
        }
    }

    translatedDocumentation.body().attributes().put(ATTR_TRANSLATED, true)

    return translatedDocumentation.documentationString
}

private fun Document.addLimitHint(): Document {
    val hintColor = JBUI.CurrentTheme.Label.disabledForeground()
    return addMessage(message("translate.documentation.limitHint"), hintColor)
}

private fun Document.addMessage(message: String, color: Color): Document = apply {
    val colorHex = ColorUtil.toHex(color)
    val contentEl = body().selectFirst(CSS_QUERY_CONTENT) ?: return@apply
    val messageEl = contentEl.prependElement("div")
        .attr("style", "color: $colorHex; margin: ${3.scaled}px 0px;")
    messageEl.appendElement("icon")
        .attr("src", "AllIcons.General.Information")
    messageEl.append("&nbsp;").appendText(message)
}

/**
 * 修复HTML格式。[DocumentationComponent]识别不了 `class="class"` 的表达形式，
 * 只识别 `class='class'`，导致样式显示异常。
 */
private fun String.fixHtml(): String = replace(fixHtmlClassExpressionRegex, FIX_HTML_CLASS_EXPRESSION_REPLACEMENT)

private fun Element.isEmptyParagraph(): Boolean = "p".equals(tagName(), true) && html().isBlank()

private fun DocumentationTranslator.getTranslatedDocumentation(document: Document, language: Language?): Document {
    val body = document.body()
    val definitionElements = body.select(".definition")
    definitionElements.forEachIndexed { index, element ->
        element.replaceWith(
            Element("definitionTranslate").attr("id", index.toString())
        )
    }

    // <pre> 元素表示预定义格式文本
    val preElements = body.select("pre")
    preElements.forEachIndexed { index, element ->
        element.replaceWith(Element("pre").attr("id", index.toString()))
    }
    // 节点说明
    val sectionElements = body.select("td.section > p")
    sectionElements.forEachIndexed { index, element ->
        // 添加 style="white-space: nowrap;"
        element.attr("style", "white-space: nowrap;")
        element.replaceWith(
            Element("sectionTranslate").attr("id", index.toString())
        )
    }
    // 各个语言的忽略内容
    val ignoredElementProvider = language?.let { IgnoredDocumentationElementProvider.forLanguage(it) }
    val ignoredElements = ignoredElementProvider?.ignoreElements(body)

    // 调用翻译
    val translatedDocument = translateDocumentation(document, Lang.AUTO, (this as Translator).primaryLanguage)
    val translatedBody = translatedDocument.body()

    // 恢复
    preElements.forEachIndexed { index, element ->
        translatedBody.selectFirst("""pre[id="$index"]""")?.replaceWith(element)
    }
    sectionElements.forEachIndexed { index, element ->
        translatedBody.selectFirst("""sectionTranslate[id="$index"]""")?.replaceWith(element)
    }
    ignoredElements?.let { ignoredElementProvider.restoreIgnoredElements(translatedBody, it) }
    definitionElements.forEachIndexed { index, element ->
        translatedBody.selectFirst("""definitionTranslate[id="$index"]""")?.replaceWith(element)
    }
    translatedDocument.body().appendChild(Element("XuhuanzyTranslateCompleted"))

    return translatedDocument
}

private fun DocumentationTranslator.getRiderRiderSummaryTranslatedDocumentation(document: Document): Document {
    val body = document.body() ?: return document

    // 寻找分割线, Rider 的文档通常由 `签名<br>类型<br>描述` 构成。
    // 我们认为最后一个 <br> 之后的内容是需要翻译的描述。
    val lastSeparator = body.select("br").lastOrNull()

    // 如果没有 <br> 分割线，我们尝试寻找 <p> 标签，这通常是另一种提示格式。
    if (lastSeparator == null) {
        val p = body.selectFirst("p") ?: return document

        // 获取 <p> 之前的所有节点
        val nodesToKeep = mutableListOf<org.jsoup.nodes.Node>()
        var currentNode: org.jsoup.nodes.Node? = p.previousSibling()
        while (currentNode != null) {
            nodesToKeep.add(currentNode)
            currentNode = currentNode.previousSibling()
        }
        nodesToKeep.reverse()

        // 从父节点移除它们
        nodesToKeep.forEach { it.remove() }

        // 翻译剩下的 (主要是 <p> 和之后的内容)
        translateDocumentation(document, Lang.AUTO, (this as Translator).primaryLanguage)

        // 加回来
        body.prependChildren(nodesToKeep)
        return document
    }

    // 获取最后一个 <br> 标签和它之前的所有兄弟节点
    val nodesToKeep = mutableListOf<org.jsoup.nodes.Node>()
    var currentNode: org.jsoup.nodes.Node? = lastSeparator
    while (currentNode != null) {
        nodesToKeep.add(currentNode)
        currentNode = currentNode.previousSibling()
    }
    nodesToKeep.reverse() // 将它们按原顺序放回

    // 从文档中将这些要保留的节点移除，此时 body 中只剩下需要翻译的内容。
    nodesToKeep.forEach { it.remove() }

    // 对剩余的、只包含描述部分的 document 进行翻译。
    translateDocumentation(document, Lang.AUTO, (this as Translator).primaryLanguage)

    // 翻译完成后，将之前保留的节点重新插入到 body 的最前面。
    body.prependChildren(nodesToKeep)

    return document
}

private fun Translator.getTranslatedDocumentation(document: Document): Document {
    val body = document.body()

    val definition = body.selectFirst(CSS_QUERY_DEFINITION)?.apply { remove() }

    val htmlDocument = HTMLDocument().also { HTMLEditorKit().read(StringReader(body.html()), it, 0) }
    val formatted = try {
        val content = htmlDocument.getText(0, htmlDocument.length).trim()
        checkContentLength(content, contentLengthLimit)
    } catch (e: ContentLengthLimitException) {
        definition?.let { body.insertChildren(0, it) }
        throw e
    }
    val translation =
        if (formatted.isEmpty()) ""
        else translate(formatted, Lang.AUTO, primaryLanguage).translation ?: ""

    val newBody = Element("body")
    definition?.let { newBody.appendChild(it) }

    val contentEl = Element("div").addClass("content")
    translation.lines().forEach { contentEl.appendElement("p").appendText(it) }
    newBody.appendChild(contentEl)

    body.replaceWith(newBody)

    return document
}
