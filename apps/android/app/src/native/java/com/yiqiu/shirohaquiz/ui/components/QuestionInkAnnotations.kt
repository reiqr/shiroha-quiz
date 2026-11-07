package com.yiqiu.shirohaquiz.ui.components

import android.os.Build
import android.view.MotionEvent
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.yiqiu.shirohaquiz.importer.model.InkGeometry
import com.yiqiu.shirohaquiz.importer.model.InkPoint
import com.yiqiu.shirohaquiz.importer.model.InkStroke
import com.yiqiu.shirohaquiz.importer.model.Question
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt

internal enum class InkTool { PEN, MARKER, ERASER }

/** Session-level tool choices survive question switches; writing mode begins off on a fresh launch. */
private object InkSessionOptions {
    var enabled = false
    var tool = InkTool.PEN
    var color = 0xFF2855B1L
    var drawWithFinger = false
    var scratchHeight = 160
}

/** In-memory edits with debounced persistence; page exit/background enqueues pending changes. */
class QuestionInkController internal constructor(
    initial: List<InkStroke>,
    private val persist: (List<InkStroke>) -> Boolean
) {
    var enabled by mutableStateOf(InkSessionOptions.enabled)
        private set
    var drawWithFinger by mutableStateOf(InkSessionOptions.drawWithFinger)
        private set
    var colorArgb by mutableStateOf(InkSessionOptions.color)
        private set
    fun changeEnabled(value: Boolean) { enabled = value; InkSessionOptions.enabled = value; if (value) visible = true }
    fun changeFingerMode(value: Boolean) { drawWithFinger = value; InkSessionOptions.drawWithFinger = value }
    fun chooseColor(value: Long) { colorArgb = value; InkSessionOptions.color = value }
    internal var tool by mutableStateOf(InkSessionOptions.tool)
        private set
    var visible by mutableStateOf(true)
    var scratchHeightDp by mutableStateOf(InkSessionOptions.scratchHeight)
        private set
    var strokes by mutableStateOf(initial)
        private set
    var pending by mutableStateOf<InkStroke?>(null)
        private set
    var warning by mutableStateOf("")
        private set
    var dirty by mutableStateOf(false)
        private set
    var stylusDown by mutableStateOf(false)
        private set
    private var undoStack = listOf<List<InkStroke>>()
    private var redoStack = listOf<List<InkStroke>>()
    private var gestureStart: List<InkStroke>? = null
    private var gestureEraser = false
    private var activeAnchor = ""
    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    fun setTool(value: String) {
        tool = when (value) { "marker" -> InkTool.MARKER; "eraser" -> InkTool.ERASER; else -> InkTool.PEN }
        InkSessionOptions.tool = tool
    }
    fun cycleScratchHeight() {
        scratchHeightDp = when (scratchHeightDp) { 160 -> 240; 240 -> 320; else -> 160 }
        InkSessionOptions.scratchHeight = scratchHeightDp
    }
    internal fun setStylusDown(value: Boolean) { stylusDown = value }

    internal fun begin(position: Offset, widthPx: Int, heightPx: Int, density: Float,
                       hardwareEraser: Boolean, anchor: String): Boolean {
        if (!enabled || !InkGeometry.validAnchor(anchor) || gestureStart != null) return false
        gestureStart = strokes
        gestureEraser = hardwareEraser || tool == InkTool.ERASER
        activeAnchor = anchor
        if (gestureEraser) eraseAt(position, widthPx, heightPx, density)
        else if (strokes.size >= InkGeometry.MAX_STROKES ||
            strokes.sumOf { it.points.size } >= InkGeometry.MAX_NEW_DRAW_POINTS) {
            warning = "本题笔迹已达容量上限，请擦除部分内容后再写。"
        } else {
            pending = InkStroke(
                tool = if (tool == InkTool.MARKER) "marker" else "pen",
                argb = colorArgb,
                widthDp = if (tool == InkTool.MARKER) 14f else 2.6f,
                points = listOf(toPoint(position, widthPx, heightPx, density)),
                anchor = anchor,
                referenceHeightDp = (heightPx / density).coerceIn(1f, 20000f)
            )
        }
        return true
    }

    internal fun move(position: Offset, widthPx: Int, heightPx: Int, density: Float, anchor: String) {
        if (gestureStart == null || activeAnchor != anchor) return
        if (gestureEraser) {
            eraseAt(position, widthPx, heightPx, density)
        } else {
            val current = pending ?: return
            if (current.points.size >= InkGeometry.MAX_POINTS_PER_STROKE) {
                warning = "单笔已达点数上限，请抬笔后继续。"
                return
            }
            val point = toPoint(position, widthPx, heightPx, density)
            val previous = current.points.last()
            val widthDp = widthPx / density
            if (hypot((point.x - previous.x) * widthDp, point.yDp - previous.yDp) >= 1.0f) {
                pending = current.copy(points = current.points + point)
            }
        }
    }

    internal fun finish(anchor: String) {
        if (activeAnchor != anchor) return
        val before = gestureStart ?: return
        val completed = pending
        if (completed != null && InkGeometry.isValid(completed) &&
            strokes.sumOf { it.points.size } + completed.points.size <= InkGeometry.MAX_NEW_DRAW_POINTS) {
            strokes = strokes + completed
        } else if (completed != null) {
            warning = "本题笔迹已达容量上限，请擦除部分内容后再写。"
        }
        pending = null
        gestureStart = null
        activeAnchor = ""
        stylusDown = false
        if (before != strokes) commit(before)
    }

    /** System cancellation is not pen-up: never commit the partial path or eraser gesture. */
    internal fun cancelGesture(anchor: String? = null) {
        if (anchor != null && activeAnchor != anchor) return
        gestureStart?.let { strokes = it }
        gestureStart = null
        pending = null
        activeAnchor = ""
        stylusDown = false
    }

    private fun eraseAt(position: Offset, widthPx: Int, heightPx: Int, density: Float) {
        val widthDp = widthPx / density
        val heightDp = heightPx / density
        val x = position.x / density
        val y = position.y / density
        strokes = strokes.filterNot { stroke ->
            stroke.anchor == activeAnchor &&
                InkGeometry.distanceToStrokeDp(x, y, InkGeometry.scaledForHeight(stroke, heightDp), widthDp) <=
                14f + stroke.widthDp / 2f
        }
    }

    private fun toPoint(position: Offset, widthPx: Int, heightPx: Int, density: Float): InkPoint = InkPoint(
        x = ((position.x / widthPx.coerceAtLeast(1)).coerceIn(0f, 1f) * 1000).roundToInt() / 1000f,
        yDp = (position.y / density).coerceIn(0f, (heightPx / density).coerceAtMost(20000f))
            .let { (it * 4).roundToInt() / 4f }
    )

    private fun commit(previous: List<InkStroke>) {
        undoStack = (undoStack + listOf(previous)).takeLast(15)
        redoStack = emptyList()
        dirty = true
    }

    fun flush(): Boolean {
        if (!dirty) return true
        if (gestureStart != null) return false
        val success = runCatching { persist(strokes) }.getOrDefault(false)
        if (success) {
            dirty = false
            warning = ""
        } else {
            warning = "笔迹尚未保存，请检查存储空间或题库状态，然后重试。"
        }
        return success
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.last()
        undoStack = undoStack.dropLast(1)
        redoStack = (redoStack + listOf(strokes)).takeLast(15)
        pending = null
        strokes = previous
        dirty = true
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack = (undoStack + listOf(strokes)).takeLast(15)
        strokes = redoStack.last()
        redoStack = redoStack.dropLast(1)
        dirty = true
    }

    fun clear() {
        if (strokes.isEmpty()) return
        val before = strokes
        strokes = emptyList()
        pending = null
        commit(before)
    }
}

@Composable
fun rememberQuestionInkController(question: Question, onSave: (List<InkStroke>) -> Boolean): QuestionInkController {
    val latestSave by rememberUpdatedState(onSave)
    val controller = remember(question.id) { QuestionInkController(question.inkStrokes) { latestSave(it) } }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(controller, controller.strokes, controller.dirty) {
        if (controller.dirty) {
            delay(800)
            if (!controller.flush()) {
                delay(1600)
                controller.flush()
            }
        }
    }
    DisposableEffect(controller, lifecycleOwner) {
        val listener = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                controller.cancelGesture()
                controller.flush()
            }
        }
        lifecycleOwner.lifecycle.addObserver(listener)
        onDispose {
            controller.cancelGesture()
            controller.flush()
            lifecycleOwner.lifecycle.removeObserver(listener)
        }
    }
    return controller
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionInkToolbar(controller: QuestionInkController, showForDevice: Boolean = true) {
    if (!showForDevice) return
    var confirmClear by remember(controller) { mutableStateOf(false) }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { controller.changeEnabled(!controller.enabled) }) {
                Text(if (controller.enabled) "关闭书写" else "✎ 触控笔批注")
            }
            if (controller.strokes.isNotEmpty()) {
                TextButton(onClick = { controller.visible = !controller.visible }) {
                    Text(if (controller.visible) "隐藏笔迹" else "显示笔迹")
                }
                Text("${controller.strokes.size} 笔", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (controller.enabled) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("pen" to "钢笔", "marker" to "荧光笔", "eraser" to "橡皮擦").forEach { (id, title) ->
                    TextButton(onClick = { controller.setTool(id) }) {
                        Text(if (controller.tool.name.equals(id, ignoreCase = true)) "●$title" else title)
                    }
                }
                TextButton(onClick = { controller.chooseColor(0xFF2855B1L) }) { Text("蓝") }
                TextButton(onClick = { controller.chooseColor(0xFFDA4343L) }) { Text("红") }
                TextButton(onClick = { controller.chooseColor(0xFF202020L) }) { Text("黑") }
                TextButton(onClick = { controller.chooseColor(0xFFE4E4E4L) }) { Text("白") }
                TextButton(onClick = { controller.chooseColor(0xFFDBB124L) }) { Text("黄") }
                TextButton(onClick = controller::undo, enabled = controller.canUndo) { Text("撤销") }
                TextButton(onClick = controller::redo, enabled = controller.canRedo) { Text("重做") }
                TextButton(onClick = { confirmClear = true }, enabled = controller.strokes.isNotEmpty()) { Text("清空") }
            }
            TextButton(onClick = { controller.changeFingerMode(!controller.drawWithFinger) }) {
                Text(if (controller.drawWithFinger) "草稿区指绘：开" else "草稿区指绘：关")
            }
            Text(
                "笔尖轻点选项可答题，拖动才写画；手指正常滚动和答题。指绘仅在草稿区生效；书写时左右滑动切题暂停。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (controller.strokes.any { it.anchor.isEmpty() }) {
            Text("旧版批注仍按整张题卡显示；字号或排版变化可能使旧笔迹偏移。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (controller.warning.isNotBlank()) {
            Row {
                Text(controller.warning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f))
                if (controller.dirty) TextButton(onClick = { controller.flush() }) { Text("重试保存") }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空本题批注？") },
            text = { Text("离开本题后无法撤销清空。") },
            confirmButton = { TextButton(onClick = { controller.clear(); confirmClear = false }) { Text("清空") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } }
        )
    }
}

/** One independent drawing region: stem, image, each original option, cloze block or scratch pad. */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.questionInkLayer(controller: QuestionInkController, anchor: String): Modifier {
    val density = LocalDensity.current.density
    val slopPx = with(LocalDensity.current) { 6.dp.toPx() }
    return this
        .motionEventSpy { event ->
            val action = event.actionMasked
            val rejectedStylus = Build.VERSION.SDK_INT >= 33 &&
                action in listOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP) &&
                (event.flags and MotionEvent.FLAG_CANCELED) != 0 &&
                event.getToolType(event.actionIndex) in
                    listOf(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_ERASER)
            if (action == MotionEvent.ACTION_CANCEL || rejectedStylus) controller.cancelGesture()
            // This is observation only: never seize the touch stream from answer buttons.
        }
        .pointerInput(controller, controller.enabled, controller.drawWithFinger, anchor, density) {
            awaitPointerEventScope {
                var active: PointerId? = null
                var starting = Offset.Zero
                var drawing = false
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (!controller.enabled) continue
                        if (active == null) {
                            val down = event.changes.firstOrNull { change ->
                                change.pressed && !change.previousPressed && !change.isConsumed &&
                                    (change.type == PointerType.Stylus || change.type == PointerType.Eraser ||
                                        (anchor == "scratch" && controller.drawWithFinger &&
                                            !controller.stylusDown &&
                                            event.changes.none {
                                                it.pressed && (it.type == PointerType.Stylus || it.type == PointerType.Eraser)
                                            } &&
                                            change.type == PointerType.Touch))
                            }
                            if (down != null) {
                                active = down.id
                                starting = down.position
                                drawing = false
                                if (down.type == PointerType.Stylus || down.type == PointerType.Eraser) {
                                    controller.setStylusDown(true)
                                }
                                // Deliberately don't consume DOWN: a short pen tap can select an answer.
                            }
                        } else {
                            val change = event.changes.firstOrNull { it.id == active }
                            if (change == null || !change.pressed) {
                                if (drawing && change != null && event.type == PointerEventType.Release) {
                                    controller.finish(anchor)
                                    change.consume()
                                } else if (drawing) {
                                    controller.cancelGesture(anchor)
                                }
                                if (change?.type == PointerType.Stylus || change?.type == PointerType.Eraser) {
                                    controller.setStylusDown(false)
                                }
                                active = null
                                drawing = false
                            } else {
                                if (!drawing && !change.isConsumed &&
                                    (change.position - starting).getDistance() > slopPx) {
                                    drawing = controller.begin(starting, size.width, size.height, density,
                                        change.type == PointerType.Eraser, anchor)
                                }
                                if (drawing) {
                                    controller.move(change.position, size.width, size.height, density, anchor)
                                    change.consume()
                                }
                            }
                        }
                    }
                } finally {
                    if (drawing) controller.cancelGesture(anchor)
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (!controller.visible) return@drawWithContent
            val heightDp = size.height / density
            clipRect {
                drawInkStrokes((controller.strokes + listOfNotNull(controller.pending))
                    .filter { it.anchor == anchor }.map { InkGeometry.scaledForHeight(it, heightDp) }, density)
            }
        }
}

/** Old releases used whole-card coordinates; draw them unchanged, without capturing new gestures. */
@Composable
fun Modifier.questionLegacyInkLayer(controller: QuestionInkController): Modifier {
    val density = LocalDensity.current.density
    return this.pointerInput(controller) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (controller.stylusDown) {
                    event.changes.filter { it.type == PointerType.Touch && it.pressed }.forEach { it.consume() }
                }
            }
        }
    }.drawWithContent {
        drawContent()
        if (controller.visible) drawInkStrokes(controller.strokes.filter { it.anchor.isEmpty() }, density)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInkStrokes(strokes: List<InkStroke>, density: Float) {
    strokes.forEach { stroke ->
        val points = stroke.points
        if (points.isEmpty()) return@forEach
        val color = Color(stroke.argb.toInt()).copy(alpha = if (stroke.tool == "marker") 0.32f else 1f)
        val width = stroke.widthDp * density
        if (points.size == 1) {
            drawCircle(color, width / 2f, Offset(points[0].x * size.width, points[0].yDp * density))
        } else {
            val path = Path().apply {
                moveTo(points.first().x * size.width, points.first().yDp * density)
                points.drop(1).forEach { p -> lineTo(p.x * size.width, p.yDp * density) }
            }
            drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
