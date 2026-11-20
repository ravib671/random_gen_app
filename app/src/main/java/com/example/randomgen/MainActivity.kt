package com.example.randomgen

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import com.example.randomgen.databinding.ActivityMainBinding
import com.example.randomgen.databinding.ViewRandomSectionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val preferences by lazy { getPreferences(MODE_PRIVATE) }
    private var partCount = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sectionA = binding.sectionA
        val sectionB = binding.sectionB

        SectionController(sectionA, getString(R.string.section_a_title)).bind()
        SectionController(sectionB, getString(R.string.section_b_title)).bind()

        restoreControls(sectionA, savedInstanceState?.getBundle("partA"))
        restoreControls(sectionB, savedInstanceState?.getBundle("partB"))
        partCount = preferences.getInt("partCount", 2).coerceIn(1, 2)
        updatePartLayout()
        binding.settingsButton.setOnClickListener { showSettings() }
    }

    private fun showSettings() {
        val choices = arrayOf(getString(R.string.one_part), getString(R.string.two_parts))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.number_of_parts)
            .setSingleChoiceItems(choices, partCount - 1) { dialog, selected ->
                partCount = selected + 1
                preferences.edit().putInt("partCount", partCount).apply()
                updatePartLayout()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updatePartLayout() {
        val showTwoParts = partCount == 2
        binding.sectionB.root.isVisible = showTwoParts
        binding.sectionA.tvSectionTitle.isVisible = showTwoParts
        val height = if (showTwoParts) R.dimen.result_height else R.dimen.single_result_height
        binding.sectionA.tvResult.minimumHeight = resources.getDimensionPixelSize(height)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // The included panels share child IDs, so save their inputs separately.
        outState.putBundle("partA", saveControls(binding.sectionA))
        outState.putBundle("partB", saveControls(binding.sectionB))
    }

    private fun saveControls(section: ViewRandomSectionBinding) = Bundle().apply {
        putString("maximum", section.etMaxValue.text.toString())
        putBoolean("repeat", section.switchRepeat.isChecked)
    }

    private fun restoreControls(section: ViewRandomSectionBinding, state: Bundle?) {
        if (state == null) return
        section.etMaxValue.setText(state.getString("maximum"))
        section.switchRepeat.isChecked = state.getBoolean("repeat")
    }

    private inner class SectionController(
        private val sectionBinding: ViewRandomSectionBinding,
        title: String
    ) {
        private var cycleGenerator: RandomCycleGenerator? = null

        init {
            sectionBinding.tvSectionTitle.text = title
        }

        fun bind() {
            sectionBinding.tvResult.setOnClickListener {
                val maxValue = sectionBinding.etMaxValue.text.toString().toIntOrNull()
                if (maxValue == null || maxValue < 1) {
                    sectionBinding.tvCycleStatus.text = getString(R.string.invalid_maximum, Int.MAX_VALUE)
                    return@setOnClickListener
                }

                val repeatEnabled = sectionBinding.switchRepeat.isChecked
                val existing = cycleGenerator
                if (existing == null || existing.max != maxValue || existing.repeatEnabled != repeatEnabled) {
                    cycleGenerator = RandomCycleGenerator(max = maxValue, repeatEnabled = repeatEnabled)
                }

                val output = cycleGenerator?.nextValue() ?: return@setOnClickListener
                sectionBinding.tvResult.text = output.value.toString()
                sectionBinding.tvCycleStatus.text = output.status
                val boxColor = randomBoxColor()
                sectionBinding.tvResult.backgroundTintList = ColorStateList.valueOf(boxColor)
                sectionBinding.tvResult.setTextColor(contrastTextColor(boxColor))
                animateResultBox()
            }
        }

        private fun animateResultBox() {
            val button = sectionBinding.tvResult
            button.animate().cancel()
            button.scaleX = 0.94f
            button.scaleY = 0.94f
            button.animate().scaleX(1f).scaleY(1f).setDuration(220L).start()
        }

        private fun contrastTextColor(backgroundColor: Int): Int {
            val luminance = ColorUtils.calculateLuminance(backgroundColor)
            return if (luminance < 0.179) Color.WHITE else Color.BLACK
        }

        private fun randomBoxColor(): Int {
            val red = Random.nextInt(70, 256)
            val green = Random.nextInt(70, 256)
            val blue = Random.nextInt(70, 256)
            return Color.rgb(red, green, blue)
        }
    }
}

private data class RandomOutput(val value: Int, val status: String)

private class RandomCycleGenerator(
    val max: Int,
    val repeatEnabled: Boolean
) {
    private var cycleNumber = 1
    private val swaps = mutableMapOf<Int, Int>()
    private var remaining = max

    fun nextValue(): RandomOutput {
        if (repeatEnabled) {
            return RandomOutput(
                value = Random.nextInt(max) + 1,
                status = "Repeat enabled: random draw from 1..$max"
            )
        }

        if (remaining == 0) {
            cycleNumber += 1
            remaining = max
        }

        // Draw from a virtual shuffled array, storing only positions touched so far.
        val index = Random.nextInt(remaining)
        val value = (swaps[index] ?: index) + 1
        val last = --remaining
        val lastValue = swaps.remove(last) ?: last
        if (index != last) swaps[index] = lastValue
        val status = if (remaining == 0) {
            "Cycle $cycleNumber complete (all numbers used). Next click starts cycle ${cycleNumber + 1}."
        } else {
            "Cycle $cycleNumber in progress: $remaining number(s) remaining."
        }

        return RandomOutput(value = value, status = status)
    }
}
