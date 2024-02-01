package com.github.boyuanh.fisher.StatusBar

import com.intellij.openapi.wm.*
import kotlinx.coroutines.CoroutineScope

class FisherStatusBarWidgetFactory:StatusBarWidgetFactory,WidgetPresentationFactory {
    override fun getId(): String {
        return "FisherStatusBar"
    }

    override fun getDisplayName(): String {
        return "FishClicker"
    }

    override fun createPresentation(context: WidgetPresentationDataContext, scope: CoroutineScope): WidgetPresentation {
        return FisherStatusBar(dataContext = context, scope = scope)
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return super.canBeEnabledOn(statusBar)
    }
}