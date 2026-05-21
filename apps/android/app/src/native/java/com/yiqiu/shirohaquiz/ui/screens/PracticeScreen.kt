package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaMotion
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.QuestionCheckResult
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.IllustrationHeroCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuizOptionCard
import com.yiqiu.shirohaquiz.ui.components.QuizOptionResultStyle
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeScreen(
    onGoExam: () -> Unit = {},
    onOpenRecords: () -> Unit = {},
    onOpenQuickEdit: () -> Unit = {}
) {
    val context = LocalContext.current
    val bank = QuizRepository.activeBank()
    val autoNextScope = rememberCoroutineScope()
    val practiceQuestions = QuizRepository.activePracticeQuestions()
    val question = QuizRepository.currentPracticeQuestion()
    val result = QuizRepository.practiceLastResult
    val isReciteMode = QuizRepository.practiceReciteModeEnabled
    val slashedVersion = QuizRepository.slashedQuestions.joinToString("|") { "${it.bankId}:${it.questionKey}" }
    val practiceCandidateQuestions = remember(bank?.id, bank?.questions, slashedVersion) {
        QuizRepository.activePracticePoolQuestions(bank)
    }
    val availableCounts = remember(practiceCandidateQuestions) {
        QuizRepository.questionTypeCounts(practiceCandidateQuestions)
    }
    val availableTypes = practiceTypeOrder.filter { (availableCounts[it] ?: 0) > 0 }
    val defaultPracticeTypes = remember(availableTypes) {
        availableTypes
            .filter { it in QuizRepository.objectiveQuestionTypes() }
            .toSet()
            .ifEmpty { availableTypes.toSet() }
            .ifEmpty { QuizRepository.objectiveQuestionTypes() }
    }
    val rememberedPracticeTypes = remember(
        bank?.id,
        availableTypes,
        QuizRepository.rememberPracticeSettingsEnabled
    ) {
        if (QuizRepository.rememberPracticeSettingsEnabled) {
            QuizRepository.preferredPracticeTypes().filter { it in availableTypes }.toSet()
        } else {
            emptySet()
        }
    }
    val initialPracticeTypes = rememberedPracticeTypes.ifEmpty { defaultPracticeTypes }
    val initialAvailableCount = remember(bank?.id, practiceCandidateQuestions, initialPracticeTypes) {
        practiceCandidateQuestions.count { it.type in initialPracticeTypes }
    }
    val initialQuestionCountMode = remember(
        bank?.id,
        initialAvailableCount,
        QuizRepository.rememberPracticeSettingsEnabled,
        QuizRepository.preferredPracticeQuestionCountMode
    ) {
        if (QuizRepository.rememberPracticeSettingsEnabled) {
            normalizeVisiblePracticeCountMode(QuizRepository.preferredPracticeQuestionCountMode, initialAvailableCount)
        } else {
            "custom"
        }
    }
    var selectedQuestionCount by remember(bank?.id, initialAvailableCount, initialQuestionCountMode) {
        mutableIntStateOf(
            resolvePracticeQuestionCount(
                mode = initialQuestionCountMode,
                customCount = QuizRepository.preferredPracticeCustomQuestionCount,
                availableCount = initialAvailableCount
            )
        )
    }
    var selectedQuestionCountMode by remember(bank?.id, initialQuestionCountMode) { mutableStateOf(initialQuestionCountMode) }
    var selectedTypes by remember(bank?.id, initialPracticeTypes) { mutableStateOf(initialPracticeTypes) }
    var practiceOrderMode by rememberSaveable(bank?.id) {
        mutableStateOf(
            if (QuizRepository.rememberPracticeSettingsEnabled) {
                QuizRepository.preferredPracticeOrderMode
            } else {
                "random"
            }
        )
    }
    var selectedPracticeMode by rememberSaveable(bank?.id, QuizRepository.preferredPracticeMode) {
        mutableStateOf(QuizRepository.preferredPracticeMode)
    }
    var selectedBatchGroupSizeMode by rememberSaveable(bank?.id, QuizRepository.preferredPracticeBatchSizeMode) {
        mutableStateOf(QuizRepository.preferredPracticeBatchSizeMode)
    }
    var selectedBatchGroupSize by remember(bank?.id, selectedBatchGroupSizeMode, selectedQuestionCount) {
        mutableIntStateOf(
            QuizRepository.resolvePracticeBatchGroupSize(
                mode = selectedBatchGroupSizeMode,
                customSize = QuizRepository.preferredPracticeBatchCustomSize
            ).coerceIn(1, selectedQuestionCount.coerceAtLeast(1))
        )
    }

    val selectedAvailable = remember(availableCounts, selectedTypes) {
        availableCounts.entries.sumOf { (type, count) -> if (type in selectedTypes) count else 0 }
    }
    val startPracticeWithSettings = {
        val safeTypes = selectedTypes.ifEmpty { QuizRepository.objectiveQuestionTypes() }
        val available = QuizRepository.activePracticePoolQuestions(bank).count { it.type in safeTypes }
        if (available > 0) {
            val count = selectedQuestionCount.coerceIn(1, available)
            QuizRepository.rememberPracticeSettings(
                context = context,
                questionCountMode = selectedQuestionCountMode,
                customQuestionCount = if (selectedQuestionCountMode == "custom") count else null,
                orderMode = practiceOrderMode,
                types = safeTypes,
                practiceMode = selectedPracticeMode,
                batchSizeMode = selectedBatchGroupSizeMode,
                customBatchSize = if (selectedBatchGroupSizeMode == "custom") selectedBatchGroupSize.coerceIn(1, count) else null
            )
            QuizRepository.startPracticeSession(
                questionCount = count,
                allowedTypes = safeTypes,
                sourceLabel = "当前题库",
                randomize = practiceOrderMode == "random",
                practiceMode = if (QuizRepository.practiceReciteModeEnabled) QuizRepository.PRACTICE_MODE_INSTANT else selectedPracticeMode,
                batchGroupSize = selectedBatchGroupSize.coerceIn(1, count)
            )
        }
    }

    val isPracticeRunning = QuizRepository.practiceQuestions.isNotEmpty()
    var isPracticeProgressExpanded by rememberSaveable(practiceQuestions.size) { mutableStateOf(true) }
    val practiceAnsweredCount = QuizRepository.practiceAnsweredCount()
    val practiceCorrectCount = QuizRepository.practiceCorrectCount()
    val practiceAccuracy = if (practiceAnsweredCount == 0) 0 else practiceCorrectCount * 100 / practiceAnsweredCount

    val screenScrollState = rememberScrollState()
    LaunchedEffect(isPracticeRunning, bank?.id) {
        if (!isPracticeRunning) {
            screenScrollState.scrollTo(0)
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(screenScrollState)
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Sm)) {
            Text(
                text = "Practice",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "练习模式",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (isPracticeRunning && !isPracticeProgressExpanded) {
                        PracticeAccuracyCapsule(
                            accuracy = practiceAccuracy,
                            modifier = Modifier.height(34.dp),
                            onClick = { isPracticeProgressExpanded = true }
                        )
                    }
                    ActionPillButton(
                        icon = Icons.Rounded.Timer,
                        text = "切换考试",
                        primary = false,
                        modifier = Modifier.height(44.dp),
                        onClick = onGoExam
                    )
                }
            }
        }

        if (!isPracticeRunning) {
            CompactPracticeSetupHero()
        }

        if (bank == null || bank.questions.isEmpty()) {
            GlassCard {
                NoticeCard("还没有可练习题目。请先在导入页创建题库。")
            }
            return
        }

        if (QuizRepository.practiceQuestions.isEmpty()) {
            PracticeSetupPanel(
                bankName = bank.name,
                totalQuestions = bank.questions.size,
                availableCounts = availableCounts,
                selectedTypes = selectedTypes,
                selectedQuestionCount = selectedQuestionCount.coerceAtMost(selectedAvailable.coerceAtLeast(1)),
                selectedQuestionCountMode = selectedQuestionCountMode,
                practiceOrderMode = practiceOrderMode,
                selectedPracticeMode = selectedPracticeMode,
                selectedBatchGroupSize = selectedBatchGroupSize.coerceIn(1, selectedQuestionCount.coerceAtLeast(1)),
                selectedBatchGroupSizeMode = selectedBatchGroupSizeMode,
                showInlineAnswerSettings = QuizRepository.practiceInlineAnswerSettingsEnabled,
                onSelectPracticeMode = { mode ->
                    selectedPracticeMode = mode
                    QuizRepository.rememberPracticeSettings(context, practiceMode = mode)
                },
                onSelectPracticeOrderMode = { mode ->
                    practiceOrderMode = mode
                    QuizRepository.rememberPracticeSettings(context, orderMode = mode)
                },
                onToggleType = { type ->
                    val updated = if (selectedTypes.contains(type)) selectedTypes - type else selectedTypes + type
                    selectedTypes = updated
                    val newAvailable = availableCounts.entries.sumOf { (itemType, count) -> if (itemType in updated) count else 0 }
                    var savedCountMode = selectedQuestionCountMode
                    var savedCustomCount: Int? = null
                    if (newAvailable > 0) {
                        val boundedCount = selectedQuestionCount.coerceAtMost(newAvailable)
                        if (boundedCount != selectedQuestionCount) {
                            selectedQuestionCount = boundedCount
                            selectedQuestionCountMode = "custom"
                            savedCountMode = "custom"
                            savedCustomCount = boundedCount
                        }
                    }
                    QuizRepository.rememberPracticeSettings(
                        context = context,
                        questionCountMode = savedCountMode,
                        customQuestionCount = savedCustomCount,
                        types = updated
                    )
                },
                onSelectQuestionCount = { count, mode ->
                    selectedQuestionCount = count
                    selectedQuestionCountMode = mode
                    if (selectedBatchGroupSize > count) {
                        selectedBatchGroupSize = count.coerceAtLeast(1)
                        selectedBatchGroupSizeMode = "custom"
                    }
                    QuizRepository.rememberPracticeSettings(
                        context = context,
                        questionCountMode = mode,
                        customQuestionCount = if (mode == "custom") count else null,
                        batchSizeMode = selectedBatchGroupSizeMode,
                        customBatchSize = if (selectedBatchGroupSizeMode == "custom") selectedBatchGroupSize else null
                    )
                },
                onSelectBatchGroupSize = { count, mode ->
                    selectedBatchGroupSize = count.coerceIn(1, selectedQuestionCount.coerceAtLeast(1))
                    selectedBatchGroupSizeMode = mode
                    QuizRepository.rememberPracticeSettings(
                        context = context,
                        batchSizeMode = mode,
                        customBatchSize = if (mode == "custom") selectedBatchGroupSize else null
                    )
                },
                onStartPractice = startPracticeWithSettings
            )
            return
        }

        if (question == null) {
            GlassCard { NoticeCard("当前练习没有可显示的题目，请重新开始练习。") }
            return
        }

        val savedResult = QuizRepository.practiceAnswerResults[question.id]
        val effectiveResult = result ?: savedResult?.let { saved ->
            QuestionCheckResult(
                question = question,
                userAnswer = saved.userAnswer,
                correct = saved.correct,
                answerText = saved.answerText
            )
        }
        val isSubmitted = effectiveResult != null
        val isBatchPractice = QuizRepository.practiceMode == QuizRepository.PRACTICE_MODE_BATCH
        val isBatchSubmitted = isBatchPractice && QuizRepository.practiceBatchSubmitted
        val isBatchBeforeSubmit = isBatchPractice && !QuizRepository.practiceBatchSubmitted
        val batchGroupStart = if (isBatchPractice) QuizRepository.practiceCurrentBatchStartIndex() else 0
        val batchGroupEnd = if (isBatchPractice) QuizRepository.practiceCurrentBatchEndIndex() else practiceQuestions.lastIndex
        val batchGroupIndexes = if (isBatchPractice) QuizRepository.practiceCurrentBatchIndexes() else practiceQuestions.indices.toList()
        val batchGroupTotal = if (isBatchPractice) QuizRepository.practiceCurrentBatchTotal() else practiceQuestions.size
        val batchGroupNumber = if (isBatchPractice) QuizRepository.practiceBatchGroupNumber() else 0
        val batchGroupCount = if (isBatchPractice) QuizRepository.practiceBatchGroupCount() else 0
        val canGoNext = if (isReciteMode) {
            if (isBatchPractice) QuizRepository.practiceIndex < batchGroupEnd else QuizRepository.practiceIndex < practiceQuestions.lastIndex
        } else if (isBatchPractice) {
            QuizRepository.practiceIndex < batchGroupEnd
        } else {
            isBatchBeforeSubmit || !QuizRepository.practiceNextRequiresResult || isSubmitted
        }
        val displayedSelection = if (isReciteMode) emptyList() else effectiveResult?.userAnswer ?: QuizRepository.selectedAnswer
        val isCurrentQuestionFavorited = QuizRepository.isCurrentPracticeQuestionFavorited()
        val batchDraftAnsweredCount = QuizRepository.practiceDraftAnsweredCount()
        var showBatchSubmitConfirm by rememberSaveable(practiceQuestions.size, QuizRepository.practiceBatchSubmitted, batchGroupStart) { mutableStateOf(false) }
        var showExitPracticeConfirm by rememberSaveable(practiceQuestions.size) { mutableStateOf(false) }
        var showBatchAnswerSheet by rememberSaveable(practiceQuestions.size, QuizRepository.practiceBatchSubmitted, batchGroupStart) { mutableStateOf(false) }
        var batchReviewWrongOnly by rememberSaveable(practiceQuestions.size, QuizRepository.practiceBatchSubmitted, batchGroupStart) { mutableStateOf(false) }
        val batchWrongIndexes = if (isBatchSubmitted) QuizRepository.practiceWrongQuestionIndexes() else emptyList()
        if (!isBatchSubmitted && batchReviewWrongOnly) batchReviewWrongOnly = false
        if (batchReviewWrongOnly && batchWrongIndexes.isEmpty()) batchReviewWrongOnly = false
        val batchReviewIndexes = if (isBatchSubmitted && batchReviewWrongOnly) batchWrongIndexes else if (isBatchPractice) batchGroupIndexes else practiceQuestions.indices.toList()
        val currentReviewPosition = batchReviewIndexes.indexOf(QuizRepository.practiceIndex)
        val goPreviousPractice = {
            if (isBatchSubmitted && batchReviewWrongOnly) {
                if (currentReviewPosition > 0) QuizRepository.goToPracticeQuestion(batchReviewIndexes[currentReviewPosition - 1])
            } else {
                QuizRepository.previousQuestion()
            }
        }
        val goNextPractice = {
            if (isBatchSubmitted && batchReviewWrongOnly) {
                if (currentReviewPosition >= 0 && currentReviewPosition < batchReviewIndexes.lastIndex) {
                    QuizRepository.goToPracticeQuestion(batchReviewIndexes[currentReviewPosition + 1])
                }
            } else {
                QuizRepository.nextQuestion()
            }
        }
        val canGoPreviousPractice = if (isBatchSubmitted && batchReviewWrongOnly) currentReviewPosition > 0 else if (isBatchPractice) QuizRepository.practiceIndex > batchGroupStart else QuizRepository.practiceIndex > 0
        val canGoNextPractice = if (isBatchSubmitted && batchReviewWrongOnly) {
            currentReviewPosition >= 0 && currentReviewPosition < batchReviewIndexes.lastIndex
        } else {
            canGoNext
        }
        val canStartNextBatchGroup = isBatchSubmitted && QuizRepository.canStartNextPracticeBatchGroup()
        val scheduleInstantAutoNextAfterSubmit: (String, Int) -> Unit = { autoNextQuestionId, autoNextIndex ->
            if (!isBatchPractice &&
                !isReciteMode &&
                QuizRepository.practiceAutoNextEnabled &&
                autoNextIndex < practiceQuestions.lastIndex
            ) {
                autoNextScope.launch {
                    delay(320)
                    if (QuizRepository.practiceIndex == autoNextIndex &&
                        QuizRepository.currentPracticeQuestion()?.id == autoNextQuestionId
                    ) {
                        QuizRepository.nextQuestion()
                    }
                }
            }
        }
        val isPracticeComplete = !isReciteMode &&
            practiceQuestions.isNotEmpty() &&
            if (isBatchPractice) QuizRepository.isAllPracticeBatchGroupsSubmitted() else QuizRepository.practiceAnsweredCount() >= practiceQuestions.size

        val questionCardModifier = if (QuizRepository.swipeNavigationEnabled) {
            Modifier.questionSwipeNavigation(
                onSwipeLeft = { if (canGoNextPractice) goNextPractice() },
                onSwipeRight = { if (canGoPreviousPractice) goPreviousPractice() }
            )
        } else {
            Modifier
        }

        Column(verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)) {
            if (isPracticeProgressExpanded) {
                PracticeProgressCard(
                    total = if (isBatchPractice) batchGroupTotal else practiceQuestions.size,
                    answered = if (isBatchBeforeSubmit) batchDraftAnsweredCount else if (isBatchSubmitted) QuizRepository.practiceCurrentBatchSubmittedCount() else practiceAnsweredCount,
                    correct = if (isBatchSubmitted) QuizRepository.practiceCurrentBatchCorrectCount() else practiceCorrectCount,
                    batchBeforeSubmit = isBatchBeforeSubmit,
                    batchSubmitted = isBatchSubmitted,
                    batchGroupNumber = batchGroupNumber,
                    batchGroupCount = batchGroupCount,
                    wrongCount = batchWrongIndexes.size,
                    wrongOnly = batchReviewWrongOnly,
                    expanded = true,
                    reciteMode = isReciteMode,
                    reciteIndex = QuizRepository.practiceIndex + 1,
                    onOpenAnswerSheet = if (isBatchPractice && !isReciteMode) { { showBatchAnswerSheet = true } } else null,
                    onToggleWrongOnly = if (isBatchSubmitted && !isReciteMode) {
                        {
                            if (batchReviewWrongOnly) {
                                batchReviewWrongOnly = false
                            } else if (batchWrongIndexes.isNotEmpty()) {
                                batchReviewWrongOnly = true
                                if (QuizRepository.practiceIndex !in batchWrongIndexes) {
                                    QuizRepository.goToPracticeQuestion(batchWrongIndexes.first())
                                }
                            }
                        }
                    } else null,
                    onToggle = { isPracticeProgressExpanded = false }
                )
            }

            if (isPracticeComplete) {
                PracticeCompletionCard(
                    total = practiceQuestions.size,
                    answered = QuizRepository.practiceAnsweredCount(),
                    correct = QuizRepository.practiceCorrectCount(),
                    onRestart = {
                        QuizRepository.endPracticeSession()
                        startPracticeWithSettings()
                    },
                    onOpenRecords = {
                        QuizRepository.endPracticeSession()
                        onOpenRecords()
                    },
                    onExit = { QuizRepository.endPracticeSession() }
                )
            }

            GlassCard(modifier = questionCardModifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactPracticeChip(
                        if (isBatchPractice) "第 $batchGroupNumber 组 · ${QuizRepository.practiceIndex - batchGroupStart + 1} / $batchGroupTotal 题" else "第 ${QuizRepository.practiceIndex + 1} / ${practiceQuestions.size} 题",
                        selected = true
                    )
                    CompactPracticeChip(typeLabel(question.type))
                    if (isReciteMode) CompactPracticeChip("背题模式", selected = true)
                    if (isBatchSubmitted && batchReviewWrongOnly) CompactPracticeChip("只看错题", selected = true)
                }
                FavoriteQuestionIconButton(
                    favorited = isCurrentQuestionFavorited,
                    onClick = { QuizRepository.toggleCurrentPracticeFavorite(context) }
                )
                if (QuizRepository.practiceQuickEditEnabled) {
                    QuickEditQuestionIconButton(onClick = onOpenQuickEdit)
                }
                if (QuizRepository.practiceSlashEnabled && QuizRepository.practiceSourceLabel == "当前题库") {
                    SlashQuestionRoundButton(
                        onClick = { QuizRepository.slashCurrentPracticeQuestion(context) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = QuizRepository.questionFontSizeSp().sp,
                    lineHeight = QuizRepository.questionLineHeightSp().sp
                ),
                fontWeight = FontWeight.SemiBold
            )
            if (question.images.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                QuestionImagesBlock(question.images, maxPreviewHeight = 360.dp, showMeta = true)
            }
            Spacer(Modifier.height(14.dp))

            when (question.type) {
                QuestionType.SINGLE,
                QuestionType.MULTIPLE,
                QuestionType.JUDGE -> {
                    question.options.forEach { option ->
                        QuizOptionCard(
                            label = option.key,
                            text = option.text,
                            selected = displayedSelection.contains(option.key),
                            resultStyle = practiceOptionResultStyle(
                                optionKey = option.key,
                                correctAnswers = question.answer,
                                result = effectiveResult,
                                revealAnswer = isReciteMode
                            ),
                            onClick = {
                                if (!isSubmitted && !isReciteMode) {
                                    val isInstantAutoSubmitQuestion = question.type == QuestionType.SINGLE || question.type == QuestionType.JUDGE
                                    val shouldAutoSubmitInstant = !isBatchPractice &&
                                        QuizRepository.practiceAutoSubmitEnabled &&
                                        isInstantAutoSubmitQuestion
                                    QuizRepository.toggleAnswer(
                                        key = option.key,
                                        multiple = question.type == QuestionType.MULTIPLE
                                    )
                                    if (shouldAutoSubmitInstant) {
                                        val autoNextQuestionId = question.id
                                        val autoNextIndex = QuizRepository.practiceIndex
                                        val submitted = QuizRepository.submitPracticeQuestion()
                                        if (submitted != null) {
                                            scheduleInstantAutoNextAfterSubmit(autoNextQuestionId, autoNextIndex)
                                        }
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(if (QuizRepository.compactOptionsEnabled) 5.dp else 10.dp))
                    }
                }

                QuestionType.BLANK,
                QuestionType.SHORT -> {
                    NoticeCard("这道题属于 ${typeLabel(question.type)}，当前先展示参考答案和解析。")
                }
            }

            Spacer(Modifier.height(10.dp))
            if (!isReciteMode && isBatchBeforeSubmit) {
                ActionPillButton(
                    Icons.Rounded.CheckCircle,
                    "提交本组",
                    primary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (batchDraftAnsweredCount < batchGroupTotal) {
                            showBatchSubmitConfirm = true
                        } else {
                            QuizRepository.submitPracticeBatch()
                        }
                    }
                )
            } else if (!isReciteMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionPillButton(
                        Icons.AutoMirrored.Rounded.TextSnippet,
                        "查看解析",
                        primary = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        fillWidthContent = true,
                        onClick = {
                            if (!isSubmitted) {
                                val autoNextQuestionId = question.id
                                val autoNextIndex = QuizRepository.practiceIndex
                                val submitted = QuizRepository.submitPracticeQuestion()
                                if (submitted != null) {
                                    scheduleInstantAutoNextAfterSubmit(autoNextQuestionId, autoNextIndex)
                                }
                            }
                        }
                    )
                    ActionPillButton(
                        Icons.Rounded.CheckCircle,
                        if (isSubmitted) "已提交" else "提交答案",
                        primary = !isSubmitted,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        fillWidthContent = true,
                        onClick = {
                            if (!isSubmitted) {
                                val autoNextQuestionId = question.id
                                val autoNextIndex = QuizRepository.practiceIndex
                                val submitted = QuizRepository.submitPracticeQuestion()
                                if (submitted != null) {
                                    scheduleInstantAutoNextAfterSubmit(autoNextQuestionId, autoNextIndex)
                                }
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    "上一题",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    enabled = canGoPreviousPractice,
                    onClick = { goPreviousPractice() }
                )
                ActionPillButton(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    "下一题",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    enabled = canGoNextPractice,
                    onClick = { goNextPractice() }
                )
            }

            if (isBatchSubmitted) {
                Spacer(Modifier.height(10.dp))
                ActionPillButton(
                    Icons.Rounded.PlayArrow,
                    if (canStartNextBatchGroup) "进入下一组" else "完成练习",
                    primary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (canStartNextBatchGroup) {
                            QuizRepository.startNextPracticeBatchGroup()
                            batchReviewWrongOnly = false
                            isPracticeProgressExpanded = true
                        } else {
                            QuizRepository.endPracticeSession()
                        }
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                ActionPillButton(
                    Icons.Rounded.Alarm,
                    "退出练习",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = { showExitPracticeConfirm = true }
                )
            }



            if (showExitPracticeConfirm) {
                PracticeExitConfirmDialog(
                    onDismiss = { showExitPracticeConfirm = false },
                    onConfirm = {
                        showExitPracticeConfirm = false
                        QuizRepository.endPracticeSession()
                    }
                )
            }

            if (isReciteMode || effectiveResult != null) {
                val answerText = effectiveResult?.answerText ?: question.answer.joinToString(" / ").ifBlank { "未识别答案" }
                Spacer(Modifier.height(16.dp))
                if (!isReciteMode && effectiveResult != null) {
                    AnswerResultCapsule(correct = effectiveResult.correct)
                    Spacer(Modifier.height(8.dp))
                }
                NoticeCard("正确答案：$answerText", warning = false)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "解析",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = question.analysis.takeIf { it.isNotBlank() }?.let(::formatAnalysisForDisplay) ?: "暂无解析",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showBatchSubmitConfirm) {
                BatchSubmitConfirmDialog(
                    unansweredCount = (batchGroupTotal - batchDraftAnsweredCount).coerceAtLeast(0),
                    onDismiss = { showBatchSubmitConfirm = false },
                    onConfirm = {
                        QuizRepository.submitPracticeBatch()
                        showBatchSubmitConfirm = false
                    }
                )
            }

            if (showBatchAnswerSheet) {
                BatchPracticeAnswerSheetDialog(
                    groupNumber = batchGroupNumber,
                    groupCount = batchGroupCount,
                    indexes = batchGroupIndexes,
                    currentIndex = QuizRepository.practiceIndex,
                    submitted = isBatchSubmitted,
                    isAnswered = { index -> QuizRepository.practiceDraftAnswers[practiceQuestions[index].id]?.isNotEmpty() == true },
                    isCorrect = { index -> QuizRepository.practiceAnswerResults[practiceQuestions[index].id]?.correct },
                    onJump = { index ->
                        QuizRepository.goToPracticeQuestion(index)
                        showBatchAnswerSheet = false
                    },
                    onDismiss = { showBatchAnswerSheet = false }
                )
            }
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeQuickEditScreen(
    onBack: () -> Unit
) {
    val question = QuizRepository.currentPracticeQuestion()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Quick Edit",
            title = "快速编辑题目",
            subtitle = "修正当前练习题后，可直接返回继续刷题。"
        )

        if (question == null) {
            GlassCard { NoticeCard("当前没有可编辑的练习题。") }
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                text = "返回练习",
                primary = false,
                modifier = Modifier.height(44.dp),
                onClick = onBack
            )
            return
        }

        var questionText by remember(question.id) { mutableStateOf(question.question) }
        var answerText by remember(question.id) { mutableStateOf(question.answer.joinToString(" / ")) }
        var analysisText by remember(question.id) { mutableStateOf(question.analysis) }
        var optionDrafts by remember(question.id) { mutableStateOf(initialQuickEditOptions(question)) }
        var savedNotice by remember(question.id) { mutableStateOf("") }
        val isObjective = question.type == QuestionType.SINGLE ||
            question.type == QuestionType.MULTIPLE ||
            question.type == QuestionType.JUDGE

        GlassCard {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip("第 ${question.number.ifBlank { "-" }} 题")
                StatusChip(typeLabel(question.type))
                StatusChip("保留题型")
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = questionText,
                onValueChange = {
                    questionText = it
                    savedNotice = ""
                },
                label = { Text("题干") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            if (question.images.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                NoticeCard("图片题图暂不在快速编辑中修改，保存后会继续保留原题图片。")
            }

            if (isObjective) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "选项",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                optionDrafts.forEachIndexed { index, option ->
                    OutlinedTextField(
                        value = option.text,
                        onValueChange = { value ->
                            optionDrafts = optionDrafts.toMutableList().also { drafts ->
                                drafts[index] = option.copy(text = value)
                            }
                            savedNotice = ""
                        },
                        label = { Text("选项 ${option.key}") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionPillButton(
                        icon = Icons.Rounded.Add,
                        text = "新增选项",
                        primary = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        fillWidthContent = true,
                        onClick = {
                            nextQuickEditOptionKey(optionDrafts)?.let { key ->
                                optionDrafts = optionDrafts + Option(key = key, text = "")
                                savedNotice = ""
                            }
                        }
                    )
                    ActionPillButton(
                        icon = Icons.Rounded.DeleteOutline,
                        text = "删除最后",
                        primary = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        fillWidthContent = true,
                        enabled = optionDrafts.size > minimumOptionCount(question.type),
                        onClick = {
                            if (optionDrafts.size > minimumOptionCount(question.type)) {
                                optionDrafts = optionDrafts.dropLast(1)
                                savedNotice = ""
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = answerText,
                onValueChange = {
                    answerText = it
                    savedNotice = ""
                },
                label = { Text(if (isObjective) "答案，例如 A 或 A/B" else "参考答案") },
                modifier = Modifier.fillMaxWidth(),
                minLines = if (isObjective) 1 else 2,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = analysisText,
                onValueChange = {
                    analysisText = it
                    savedNotice = ""
                },
                label = { Text("解析") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            if (savedNotice.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                NoticeCard(savedNotice)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                text = "取消",
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                fillWidthContent = true,
                onClick = onBack
            )
            ActionPillButton(
                icon = Icons.Rounded.CheckCircle,
                text = "保存修改",
                primary = questionText.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                fillWidthContent = true,
                enabled = questionText.isNotBlank(),
                onClick = {
                    val updatedQuestion = question.copy(
                        question = questionText.trim(),
                        options = if (isObjective) optionDrafts.map { it.copy(text = it.text.trim()) } else emptyList(),
                        answer = parseQuickEditAnswer(answerText, question.type, optionDrafts),
                        analysis = analysisText.trim()
                    )
                    if (QuizRepository.updateCurrentPracticeQuestion(updatedQuestion)) {
                        savedNotice = "已保存修改，当前练习题已刷新。"
                        onBack()
                    } else {
                        savedNotice = "保存失败：未找到这道题的源题库。"
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeSetupPanel(
    bankName: String,
    totalQuestions: Int,
    availableCounts: Map<QuestionType, Int>,
    selectedTypes: Set<QuestionType>,
    selectedQuestionCount: Int,
    selectedQuestionCountMode: String,
    practiceOrderMode: String,
    selectedPracticeMode: String,
    selectedBatchGroupSize: Int,
    selectedBatchGroupSizeMode: String,
    showInlineAnswerSettings: Boolean,
    onSelectPracticeMode: (String) -> Unit,
    onSelectPracticeOrderMode: (String) -> Unit,
    onToggleType: (QuestionType) -> Unit,
    onSelectQuestionCount: (Int, String) -> Unit,
    onSelectBatchGroupSize: (Int, String) -> Unit,
    onStartPractice: () -> Unit
) {
    val selectedAvailable = availableCounts.entries.sumOf { (type, count) -> if (type in selectedTypes) count else 0 }
    var showCustomCountDialog by remember { mutableStateOf(false) }
    var customQuestionCountText by remember(selectedAvailable) {
        mutableStateOf(selectedQuestionCount.coerceIn(1, selectedAvailable.coerceAtLeast(1)).toString())
    }
    var showCustomBatchGroupDialog by remember { mutableStateOf(false) }
    var customBatchGroupText by remember(selectedQuestionCount) {
        mutableStateOf(selectedBatchGroupSize.coerceIn(1, selectedQuestionCount.coerceAtLeast(1)).toString())
    }

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bankName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "共 $totalQuestions 题 · 选范围后开始",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ActionPillButton(
                icon = Icons.Rounded.PlayArrow,
                text = "开始练习",
                primary = selectedAvailable > 0,
                modifier = Modifier.height(46.dp),
                onClick = { if (selectedAvailable > 0) onStartPractice() }
            )
        }
        Spacer(Modifier.height(12.dp))

        if (showInlineAnswerSettings) {
            Text("答题方式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = "即时反馈",
                    primary = selectedPracticeMode == QuizRepository.PRACTICE_MODE_INSTANT,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    fillWidthContent = true,
                    onClick = { onSelectPracticeMode(QuizRepository.PRACTICE_MODE_INSTANT) }
                )
                ActionPillButton(
                    icon = Icons.AutoMirrored.Rounded.TextSnippet,
                    text = "批量做题",
                    primary = selectedPracticeMode == QuizRepository.PRACTICE_MODE_BATCH,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    fillWidthContent = true,
                    onClick = { onSelectPracticeMode(QuizRepository.PRACTICE_MODE_BATCH) }
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (selectedPracticeMode == QuizRepository.PRACTICE_MODE_BATCH) "按每组题数连续做题，提交本组后统一看解析。" else "每题提交后立即查看结果和解析。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (selectedPracticeMode == QuizRepository.PRACTICE_MODE_BATCH) {
                Spacer(Modifier.height(10.dp))
                Text("每组题数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    val safeMaxGroupSize = selectedQuestionCount.coerceAtLeast(1)
                    ActionPillButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = "10题",
                        primary = selectedBatchGroupSizeMode == "10",
                        modifier = Modifier.height(44.dp),
                        enabled = safeMaxGroupSize >= 10,
                        onClick = { onSelectBatchGroupSize(10.coerceAtMost(safeMaxGroupSize), "10") }
                    )
                    ActionPillButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = "20题",
                        primary = selectedBatchGroupSizeMode == "20",
                        modifier = Modifier.height(44.dp),
                        enabled = safeMaxGroupSize >= 20,
                        onClick = { onSelectBatchGroupSize(20.coerceAtMost(safeMaxGroupSize), "20") }
                    )
                    ActionPillButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = "自定义",
                        primary = selectedBatchGroupSizeMode == "custom",
                        modifier = Modifier.height(44.dp),
                        onClick = {
                            customBatchGroupText = selectedBatchGroupSize.coerceIn(1, safeMaxGroupSize).toString()
                            showCustomBatchGroupDialog = true
                        }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "当前每组 ${selectedBatchGroupSize.coerceIn(1, selectedQuestionCount.coerceAtLeast(1))} 题，提交本组后再进入下一组。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))
        }
        Text("组题方式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionPillButton(
                icon = Icons.Rounded.PlayArrow,
                text = "随机刷题",
                primary = practiceOrderMode == "random",
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                fillWidthContent = true,
                onClick = { onSelectPracticeOrderMode("random") }
            )
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.TextSnippet,
                text = "顺序刷题",
                primary = practiceOrderMode == "ordered",
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                fillWidthContent = true,
                onClick = { onSelectPracticeOrderMode("ordered") }
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (practiceOrderMode == "random") "从已选题型中随机抽题。" else "按题库原顺序练习。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(10.dp))
        Text("题型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            practiceTypeOrder.filter { (availableCounts[it] ?: 0) > 0 }.forEach { type ->
                val note = if (type in QuizRepository.objectiveQuestionTypes()) "" else " · 主观"
                ActionPillButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = "${typeLabel(type)} ${availableCounts[type] ?: 0}$note",
                    primary = type in selectedTypes,
                    modifier = Modifier.height(44.dp),
                    onClick = { onToggleType(type) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("题量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "可用 $selectedAvailable 题",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            val safeAvailable = selectedAvailable.coerceAtLeast(1)
            val halfCount = (safeAvailable / 2).coerceAtLeast(1)
            ActionPillButton(
                icon = Icons.Rounded.PlayArrow,
                text = "自定义",
                primary = selectedQuestionCountMode == "custom",
                modifier = Modifier.height(44.dp),
                onClick = {
                    customQuestionCountText = selectedQuestionCount.coerceIn(1, safeAvailable).toString()
                    showCustomCountDialog = true
                }
            )
            buildList {
                if (safeAvailable >= 50) add(Triple(50, "50 题", "50"))
                if (safeAvailable >= 100) add(Triple(100, "100 题", "100"))
                add(Triple(halfCount, "一半 $halfCount 题", "half"))
                add(Triple(safeAvailable, "全部 $safeAvailable 题", "all"))
            }
                .distinctBy { it.first }
                .forEach { (count, label, mode) ->
                    ActionPillButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = label,
                        primary = selectedQuestionCountMode == mode,
                        modifier = Modifier.height(44.dp),
                        onClick = { onSelectQuestionCount(count, mode) }
                    )
                }
        }
        if (selectedAvailable <= 0) {
            Spacer(Modifier.height(10.dp))
            val emptyTip = if (totalQuestions > 0) {
                "当前筛选范围内没有可练习题目。若题目已被斩题，可到题库详情的斩题本恢复后继续练习。"
            } else {
                "当前筛选没有可练习题目，请至少选择一种有题目的题型。"
            }
            NoticeCard(emptyTip, warning = true)
        }
    }

    if (showCustomCountDialog) {
        CustomQuestionCountDialog(
            title = "自定义练习题量",
            value = customQuestionCountText,
            maxCount = selectedAvailable.coerceAtLeast(1),
            onValueChange = { customQuestionCountText = it },
            onDismiss = { showCustomCountDialog = false },
            onConfirm = { count ->
                onSelectQuestionCount(count, "custom")
                showCustomCountDialog = false
            }
        )
    }
    if (showCustomBatchGroupDialog) {
        CustomQuestionCountDialog(
            title = "自定义每组题数",
            value = customBatchGroupText,
            maxCount = selectedQuestionCount.coerceAtLeast(1),
            onValueChange = { customBatchGroupText = it },
            onDismiss = { showCustomBatchGroupDialog = false },
            onConfirm = { count ->
                onSelectBatchGroupSize(count, "custom")
                showCustomBatchGroupDialog = false
            }
        )
    }
}

@Composable
private fun CompactPracticeSetupHero() {
    val density = LocalDensity.current
    val floatDistancePx = with(density) { ShirohaMotion.HeroFloatDistance.toPx() }
    val heroFloat = rememberInfiniteTransition(label = "practice_illustration_float")
    val imageOffsetY by heroFloat.animateFloat(
        initialValue = -floatDistancePx,
        targetValue = floatDistancePx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ShirohaMotion.HeroFloatMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "practice_illustration_float_y"
    )

    GlassCard(
        modifier = Modifier.height(ShirohaDimens.HeroCardHeight),
        contentPadding = ShirohaSpacing.Xl
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PracticeSetupStepCard(index = "1", text = "选好参数", selected = true)
                PracticeSetupStepCard(index = "2", text = "开始练习", selected = false)
                PracticeSetupStepCard(index = "3", text = "记录结果", selected = false)
            }
            if (QuizRepository.shirohaModeEnabled) {
                Box(
                    modifier = Modifier.size(ShirohaDimens.HeroImageFrameSize),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.illus_practice_hint_webp),
                        contentDescription = "练习提示",
                        modifier = Modifier
                            .size(ShirohaDimens.HeroImageFrameSize)
                            .graphicsLayer { translationY = imageOffsetY }
                            .alpha(ShirohaDimens.HeroImageAlpha),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeSetupStepCard(
    index: String,
    text: String,
    selected: Boolean
) {
    Surface(
        modifier = Modifier
            .width(ShirohaDimens.StepPillWidth)
            .defaultMinSize(minHeight = ShirohaDimens.StepPillMinHeight),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (selected) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardMuted,
        border = BorderStroke(ShirohaDimens.Hairline, if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ShirohaDimens.StepPillHorizontalPadding, vertical = ShirohaDimens.StepPillVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$index  $text",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else ShirohaColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}




@Composable
private fun FavoriteQuestionIconButton(
    favorited: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (favorited) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = if (favorited) "取消收藏当前题目" else "收藏当前题目",
            tint = if (favorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun QuickEditQuestionIconButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.EditNote,
            contentDescription = "快速编辑当前题目",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SlashQuestionRoundButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = ShirohaColors.BrandPrimarySoft,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineSelected)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "斩",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompactPracticeChip(
    text: String,
    selected: Boolean = false
) {
    Surface(
        modifier = Modifier.defaultMinSize(minHeight = 32.dp),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (selected) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardMuted,
        border = BorderStroke(ShirohaDimens.Hairline, if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else ShirohaColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Modifier.practiceNoRipplePillClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val shape = RoundedCornerShape(ShirohaRadius.Pill)
    return this
        .clip(shape)
        .clickable(
            enabled = enabled,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
}

@Composable
private fun CompactPracticeActionChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minHeight = 32.dp)
            .practiceNoRipplePillClick(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = ShirohaColors.CardWhite86,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineStrong)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun CompactExitPracticeButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minHeight = 34.dp)
            .practiceNoRipplePillClick(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = ShirohaColors.CardWhite86,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineStrong)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "退出练习",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "退出",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun PracticeCompletionCard(
    total: Int,
    answered: Int,
    correct: Int,
    onRestart: () -> Unit,
    onOpenRecords: () -> Unit,
    onExit: () -> Unit
) {
    val accuracy = if (answered == 0) 0 else correct * 100 / answered
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "本轮练习完成",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "共 $total 题 · 已提交 $answered 题 · 正确 $correct 题 · 正确率 $accuracy%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionPillButton(
                icon = Icons.Rounded.PlayArrow,
                text = "再练一组",
                primary = true,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                fillWidthContent = true,
                onClick = onRestart
            )
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.TextSnippet,
                text = "查看记录",
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                fillWidthContent = true,
                onClick = onOpenRecords
            )
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                text = "返回设置",
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                fillWidthContent = true,
                onClick = onExit
            )
        }
    }
}


private fun practiceOptionResultStyle(
    optionKey: String,
    correctAnswers: List<String>,
    result: QuestionCheckResult?,
    revealAnswer: Boolean = false
): QuizOptionResultStyle {
    if (result == null && !revealAnswer) return QuizOptionResultStyle.Neutral
    val normalizedKey = optionKey.trim().uppercase()
    val isCorrectAnswer = correctAnswers.any { it.trim().uppercase() == normalizedKey }
    val isUserSelected = result?.userAnswer.orEmpty().any { it.trim().uppercase() == normalizedKey }
    return when {
        isCorrectAnswer -> QuizOptionResultStyle.Correct
        isUserSelected -> QuizOptionResultStyle.Wrong
        else -> QuizOptionResultStyle.Neutral
    }
}

@Composable
private fun AnswerResultCapsule(correct: Boolean) {
    val accent = if (correct) ShirohaColors.StateSuccess else ShirohaColors.StateDanger
    val background = if (correct) ShirohaColors.StateSuccessSoft else ShirohaColors.StateDangerSoft
    val text = if (correct) "回答正确" else "回答错误"
    Surface(
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = background.copy(alpha = if (ShirohaColors.isDarkMode) 0.9f else 0.76f),
        border = BorderStroke(ShirohaDimens.Hairline, accent.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (correct) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = text,
                    modifier = Modifier.size(15.dp),
                    tint = accent
                )
            } else {
                Text(
                    text = "×",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    }
}

@Composable
private fun PracticeProgressCard(
    total: Int,
    answered: Int,
    correct: Int,
    batchBeforeSubmit: Boolean,
    batchSubmitted: Boolean,
    batchGroupNumber: Int,
    batchGroupCount: Int,
    wrongCount: Int,
    wrongOnly: Boolean,
    expanded: Boolean,
    reciteMode: Boolean = false,
    reciteIndex: Int = 0,
    onOpenAnswerSheet: (() -> Unit)?,
    onToggleWrongOnly: (() -> Unit)?,
    onToggle: () -> Unit
) {
    val accuracy = if (answered == 0) 0 else correct * 100 / answered
    if (!expanded) return

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (reciteMode) {
                    Text(
                        text = "背题模式",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "浏览 ${reciteIndex.coerceIn(1, total.coerceAtLeast(1))} / $total 题 · 不计入正确率",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (batchSubmitted) {
                    Text(
                        text = "批量复盘",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "第 $batchGroupNumber / $batchGroupCount 组",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = if (batchBeforeSubmit) "批量做题 · 第 $batchGroupNumber / $batchGroupCount 组" else "正确率",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!batchBeforeSubmit) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "已提交 $answered / $total 题 · 正确率 $accuracy%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenAnswerSheet != null) {
                    PracticePanelCapsule(text = "答题卡", onClick = onOpenAnswerSheet)
                }
                if (onToggleWrongOnly != null) {
                    PracticePanelCapsule(
                        text = if (wrongOnly) "看全部" else "只看错题",
                        enabled = wrongCount > 0,
                        onClick = onToggleWrongOnly
                    )
                }
                PracticePanelCapsule(
                    text = "收起",
                    onClick = onToggle
                )
            }
        }
    }
}

@Composable
private fun PracticePanelCapsule(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minHeight = 32.dp)
            .practiceNoRipplePillClick(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (enabled) ShirohaColors.CardWhite86 else ShirohaColors.CardMuted,
        border = BorderStroke(ShirohaDimens.Hairline, if (enabled) ShirohaColors.LineStrong else ShirohaColors.LineSoft)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else ShirohaColors.TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun PracticeAccuracyCapsule(
    accuracy: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .width(38.dp)
            .practiceNoRipplePillClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$accuracy%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Modifier.questionSwipeNavigation(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier {
    val thresholdPx = with(LocalDensity.current) { 62.dp.toPx() }
    val maxOffsetPx = with(LocalDensity.current) { 34.dp.toPx() }
    val swipeOffset = remember { Animatable(0f) }
    val swipeScope = rememberCoroutineScope()
    var dragAmount by remember { mutableStateOf(0f) }
    val dragState = rememberDraggableState { dragDelta ->
        dragAmount += dragDelta
        val visualOffset = (dragAmount * 0.42f).coerceIn(-maxOffsetPx, maxOffsetPx)
        swipeScope.launch { swipeOffset.snapTo(visualOffset) }
    }

    fun resetSwipeOffset() {
        swipeScope.launch { swipeOffset.animateTo(0f, animationSpec = tween(durationMillis = 140)) }
    }

    val offsetFraction = if (maxOffsetPx > 0f) {
        (abs(swipeOffset.value) / maxOffsetPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    return this
        .graphicsLayer {
            translationX = swipeOffset.value
            alpha = 1f - offsetFraction * 0.05f
        }
        .draggable(
            state = dragState,
            orientation = Orientation.Horizontal,
            onDragStarted = {
                dragAmount = 0f
                swipeScope.launch { swipeOffset.stop() }
            },
            onDragStopped = {
                when {
                    dragAmount <= -thresholdPx -> onSwipeLeft()
                    dragAmount >= thresholdPx -> onSwipeRight()
                }
                dragAmount = 0f
                resetSwipeOffset()
            }
        )
}


@Composable
private fun BatchPracticeAnswerSheetDialog(
    groupNumber: Int,
    groupCount: Int,
    indexes: List<Int>,
    currentIndex: Int,
    submitted: Boolean,
    isAnswered: (Int) -> Boolean,
    isCorrect: (Int) -> Boolean?,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (submitted) "第 $groupNumber / $groupCount 组复盘答题卡" else "第 $groupNumber / $groupCount 组答题卡") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BatchLegendChip("当前", selected = true)
                    if (submitted) {
                        BatchLegendChip("正确", correct = true)
                        BatchLegendChip("错误", correct = false)
                    } else {
                        BatchLegendChip("已答")
                        BatchLegendChip("未答", muted = true)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    indexes.chunked(5).forEach { rowIndexes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            rowIndexes.forEach { index ->
                                val current = index == currentIndex
                                val correct = if (submitted) isCorrect(index) else null
                                val answered = isAnswered(index)
                                BatchAnswerNumberChip(
                                    number = index - indexes.first() + 1,
                                    current = current,
                                    answered = answered,
                                    submitted = submitted,
                                    correct = correct,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onJump(index) }
                                )
                            }
                            repeat(5 - rowIndexes.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun BatchLegendChip(
    text: String,
    selected: Boolean = false,
    correct: Boolean? = null,
    muted: Boolean = false
) {
    val background = when {
        selected -> ShirohaColors.BrandPrimarySoft
        correct == true -> ShirohaColors.StateSuccessSoft
        correct == false -> ShirohaColors.StateDangerSoft
        muted -> ShirohaColors.CardMuted
        else -> ShirohaColors.CardWhite86
    }
    val borderColor = when {
        selected -> ShirohaColors.LineSelected
        correct == true -> ShirohaColors.StateSuccess
        correct == false -> ShirohaColors.StateDanger
        else -> ShirohaColors.LineSoft
    }
    val textColor = when {
        selected -> MaterialTheme.colorScheme.primary
        correct == true -> ShirohaColors.StateSuccess
        correct == false -> ShirohaColors.StateDanger
        else -> ShirohaColors.TextSecondary
    }
    Surface(
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = background,
        border = BorderStroke(ShirohaDimens.Hairline, borderColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun BatchAnswerNumberChip(
    number: Int,
    current: Boolean,
    answered: Boolean,
    submitted: Boolean,
    correct: Boolean?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = when {
        current -> ShirohaColors.BrandPrimarySoft
        submitted && correct == true -> ShirohaColors.StateSuccessSoft
        submitted && correct == false -> ShirohaColors.StateDangerSoft
        answered -> ShirohaColors.CardWhite86
        else -> ShirohaColors.CardMuted
    }
    val borderColor = when {
        current -> ShirohaColors.LineSelected
        submitted && correct == true -> ShirohaColors.StateSuccess
        submitted && correct == false -> ShirohaColors.StateDanger
        answered -> ShirohaColors.LineStrong
        else -> ShirohaColors.LineSoft
    }
    val textColor = when {
        current -> MaterialTheme.colorScheme.primary
        submitted && correct == true -> ShirohaColors.StateSuccess
        submitted && correct == false -> ShirohaColors.StateDanger
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier
            .height(36.dp)
            .practiceNoRipplePillClick(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = background,
        border = BorderStroke(ShirohaDimens.Hairline, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PracticeExitConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确定要退出练习吗？") },
        text = {
            Text(
                text = "退出后将结束当前练习进度。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("退出") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun BatchSubmitConfirmDialog(
    unansweredCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提交本组？") },
        text = {
            Text(
                text = if (unansweredCount > 0) {
                    "还有 $unansweredCount 题未作答，提交后会按错误处理。确定仍然提交吗？"
                } else {
                    "提交后将统一判分，并进入解析复盘。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("提交") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}


@Composable
private fun CustomQuestionCountDialog(
    title: String,
    value: String,
    maxCount: Int,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "请输入 1～$maxCount 之间的题数。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() }.take(4)) },
                    singleLine = true,
                    label = { Text("题量") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val count = value.toIntOrNull()?.coerceIn(1, maxCount) ?: 1
                    onConfirm(count)
                }
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun normalizeVisiblePracticeCountMode(mode: String, availableCount: Int): String {
    return when {
        mode == "50" && availableCount >= 50 -> "50"
        mode == "100" && availableCount >= 100 -> "100"
        mode == "half" -> "half"
        mode == "all" -> "all"
        else -> "custom"
    }
}

private fun resolvePracticeQuestionCount(
    mode: String,
    customCount: Int,
    availableCount: Int
): Int {
    val safeAvailable = availableCount.coerceAtLeast(1)
    return when (mode) {
        "50" -> 50.coerceAtMost(safeAvailable)
        "100" -> 100.coerceAtMost(safeAvailable)
        "half" -> (safeAvailable / 2).coerceAtLeast(1)
        "all" -> safeAvailable
        else -> customCount.coerceIn(1, safeAvailable)
    }
}

private fun formatAnalysisForDisplay(analysis: String): String {
    return analysis.trim()
        .replace(Regex("\\s*(?=([A-GＡ-Ｇ])项[，,：:])"), "\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

private val practiceTypeOrder = listOf(
    QuestionType.SINGLE,
    QuestionType.MULTIPLE,
    QuestionType.JUDGE,
    QuestionType.BLANK,
    QuestionType.SHORT
)


private fun initialQuickEditOptions(question: Question): List<Option> {
    if (question.options.isNotEmpty()) return question.options
    return when (question.type) {
        QuestionType.JUDGE -> listOf(
            Option("A", "正确"),
            Option("B", "错误")
        )
        QuestionType.SINGLE,
        QuestionType.MULTIPLE -> listOf("A", "B", "C", "D").map { key -> Option(key, "") }
        else -> emptyList()
    }
}

private fun minimumOptionCount(type: QuestionType): Int = when (type) {
    QuestionType.JUDGE -> 2
    QuestionType.SINGLE,
    QuestionType.MULTIPLE -> 2
    else -> 0
}

private fun nextQuickEditOptionKey(options: List<Option>): String? {
    val used = options.map { it.key.uppercase() }.toSet()
    return ('A'..'Z').firstOrNull { it.toString() !in used }?.toString()
}

private fun parseQuickEditAnswer(raw: String, type: QuestionType, options: List<Option>): List<String> {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return emptyList()
    return when (type) {
        QuestionType.SINGLE,
        QuestionType.MULTIPLE,
        QuestionType.JUDGE -> {
            val upper = trimmed.uppercase()
            val normalized = when (upper) {
                "正确", "对", "是", "TRUE", "T", "√" -> "A"
                "错误", "错", "否", "FALSE", "F", "×", "X" -> "B"
                else -> upper
            }
            val optionKeys = options.map { it.key.uppercase() }.toSet()
            val tokens = normalized
                .split(Regex("""[\s,，、/|;；]+"""))
                .flatMap { token ->
                    if (token.length > 1 && token.all { it in 'A'..'Z' }) {
                        token.map { it.toString() }
                    } else {
                        listOf(token)
                    }
                }
                .map { it.trim().uppercase() }
                .filter { it.isNotBlank() && it in optionKeys }
                .distinct()
            tokens
        }
        QuestionType.BLANK,
        QuestionType.SHORT -> trimmed
            .split(Regex("""[\n/]+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(trimmed) }
    }
}


private fun typeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE -> "单选题"
    QuestionType.MULTIPLE -> "多选题"
    QuestionType.JUDGE -> "判断题"
    QuestionType.BLANK -> "填空题"
    QuestionType.SHORT -> "简答题"
}
