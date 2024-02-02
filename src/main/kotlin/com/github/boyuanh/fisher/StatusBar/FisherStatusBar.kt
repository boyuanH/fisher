package com.github.boyuanh.fisher.StatusBar

import com.intellij.ide.util.EditorGotoLineNumberDialog
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.TextWidgetPresentation
import com.intellij.openapi.wm.WidgetPresentationDataContext
import com.intellij.openapi.wm.impl.status.EditorBasedWidgetHelper
import com.intellij.ui.UIBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.job
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.Nls
import java.awt.Component
import java.awt.event.MouseEvent
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.streams.asSequence
import kotlin.time.Duration.Companion.milliseconds


@Internal
@OptIn(FlowPreview::class)
open class FisherStatusBar (private val dataContext: WidgetPresentationDataContext,
                            scope: CoroutineScope,
    protected val helper: EditorBasedWidgetHelper = EditorBasedWidgetHelper(dataContext.project)) : TextWidgetPresentation{

    private val updateTextRequests = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        .also { it.tryEmit(Unit) }
    private val charCountRequests = MutableSharedFlow<CodePointCountTask>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var Reader_index:Int = -1

    init {
        val disposable = Disposer.newDisposable()
        scope.coroutineContext.job.invokeOnCompletion { Disposer.dispose(disposable) }
        check(charCountRequests.tryEmit(CodePointCountTask(text = "", startOffset = 0, endOffset = 0)))
        val multicaster = EditorFactory.getInstance().allEditors
        val xx = multicaster.size
        val xde = 2
//        timer_exec.scheduleAtFixedRate({
//                                       check(updateTextRequests.tryEmit(Unit))
//
//        },10,1,TimeUnit.SECONDS)
        for (idx in multicaster.indices){
            val _edit = multicaster[idx]
            _edit.addEditorMouseListener(object:EditorMouseListener{
                override fun mouseClicked(event: EditorMouseEvent) {
                    super.mouseClicked(event)
                    check(updateTextRequests.tryEmit(Unit))
                }
            })

        }
    }

    companion object{
        @JvmField
        val DISABLE_FOR_EDITOR: Key<Any> = Key<Any>("positionPanel.disableForEditor")
        const val SPACE: String = "     "
        const val SEPARATOR: String = ":"
        private const val CHAR_COUNT_SYNC_LIMIT = 500_000
        private const val CHAR_COUNT_UNKNOWN = "..."

        public const val EMPTY_DISPLAY: String = "____________________"
        public var DISPLAY_CONTENT :String = EMPTY_DISPLAY
        public var ClickCount_Manu :Int = 0

        private var timer_exec:ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        private const val TargetSrcDict : String = "C:\\fishers\\"
        private const val FisherConfig : String = "cfg.json"
    }

    override val alignment: Float
        get() = Component.CENTER_ALIGNMENT


    @Suppress("HardCodedStringLiteral")
    private fun getCurrentDisplayText(): @NlsContexts.Label String{
        return "Dec_" + (ClickCount_Manu++).toString()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun text(): Flow<@NlsContexts.Label String?> {
        return combine(updateTextRequests, dataContext.currentFileEditor) { _, fileEditor -> (fileEditor as? TextEditor)?.editor }
            .debounce(100.milliseconds)
            .mapLatest { editor ->
                if (editor == null || DISABLE_FOR_EDITOR.isIn(editor)) null else readAction { getCurrentDisplayText() }
            }
            .combine(charCountRequests.mapLatest { task ->
                Character.codePointCount(task.text, task.startOffset, task.endOffset).toString()
            }) { text, charCount ->
                text?.replaceFirst(CHAR_COUNT_UNKNOWN, charCount)
            }
    }


}



private class CodePointCountTask(@JvmField val text: CharSequence, @JvmField val startOffset: Int, @JvmField val endOffset: Int)