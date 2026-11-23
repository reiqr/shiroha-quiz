package com.yiqiu.shirohaquiz.ui.screens

import com.yiqiu.shirohaquiz.BuildConfig
import com.yiqiu.shirohaquiz.state.QuizRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LatexComparisonBankTest {
    @Test
    fun opensBothBuiltInBanksInOrderWithoutReimporting() {
        assumeTrue(BuildConfig.LATEX_COMPARISON_ENABLED)
        val context = RuntimeEnvironment.getApplication()
        QuizRepository.init(context)
        val originalCount = QuizRepository.banks.size
        for (sample in LatexComparisonBank.entries) {
            val expected = context.assets.open("latex-comparison/${sample.asset}")
                .bufferedReader().use { it.readText() }
                .let(QuizRepository::parseImportJsonBank) ?: error("Invalid fixture: ${sample.asset}")
            repeat(2) {
                assertTrue(startLatexComparison(context, sample))
                assertEquals(sample.count, QuizRepository.practiceQuestions.size)
                assertEquals(0, QuizRepository.practiceIndex)
                assertEquals(expected.questions.map { it.question }, QuizRepository.practiceQuestions.map { it.question })
            }
            assertEquals(sample.ordinal + 1 + originalCount, QuizRepository.banks.size)
        }
    }
}
