package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class PinBalloonAction : DumbAwareAction(), ImportantTranslationAction {

    override val priority: Int = 1

    init {
        templatePresentation.text = message("action.PinBalloonAction.text")
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = TranslationUIManager.instance(e.project).currentBalloon() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        TranslationUIManager.instance(e.project).currentBalloon()?.pin()
    }
}