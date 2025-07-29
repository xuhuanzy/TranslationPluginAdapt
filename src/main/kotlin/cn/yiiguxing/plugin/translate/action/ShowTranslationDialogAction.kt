package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.SelectionMode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager

/**
 * 显示翻译对话框动作
 */
class ShowTranslationDialogAction : TranslateAction(true) {

    override val priority: Int = ACTION_LOW_PRIORITY

    init {
        isEnabledInModalContext = true
        templatePresentation.text = adaptedMessage("action.ShowTranslationDialogAction.text")
        templatePresentation.description = message("action.ShowTranslationDialogAction.description")
    }

    override val selectionMode: SelectionMode
        get() = Settings.getInstance().autoSelectionMode

    // should be always available
    override fun update(e: AnActionEvent) {}

    override fun actionPerformed(e: AnActionEvent) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment) {
            return
        }

        TranslationUIManager.showDialog(e.project)
        if (Settings.getInstance().takeWordWhenDialogOpens) {
            super.actionPerformed(e)
        }
    }

}