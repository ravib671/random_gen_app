package com.example.randomgen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.randomgen.databinding.ActivityMainBinding
import com.example.randomgen.databinding.ViewRandomSectionBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val controllerA = SectionController(binding.sectionA, getString(R.string.section_a_title))
        val controllerB = SectionController(binding.sectionB, getString(R.string.section_b_title))

        controllerA.bind()
        controllerB.bind()
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
            sectionBinding.btnGenerate.setOnClickListener {
                val maxValue = sectionBinding.etMaxValue.text.toString().toIntOrNull()
                if (maxValue == null || maxValue < 1) {
                    sectionBinding.tvResult.text = "Result: -"
                    sectionBinding.tvCycleStatus.text = "Please enter N >= 1"
                    return@setOnClickListener
                }

                val repeatEnabled = sectionBinding.switchRepeat.isChecked
                val existing = cycleGenerator
                if (existing == null || existing.max != maxValue || existing.repeatEnabled != repeatEnabled) {
                    cycleGenerator = RandomCycleGenerator(max = maxValue, repeatEnabled = repeatEnabled)
                }

                val output = cycleGenerator?.nextValue() ?: return@setOnClickListener
                sectionBinding.tvResult.text = "Result: ${output.value}"
                sectionBinding.tvCycleStatus.text = output.status
            }
        }
    }
}

private data class RandomOutput(val value: Int, val status: String)

private class RandomCycleGenerator(
    val max: Int,
    val repeatEnabled: Boolean
) {
    private var cycleNumber = 1
    private val remaining = mutableListOf<Int>()
    private var startFreshCycleOnNextClick = false

    fun nextValue(): RandomOutput {
        if (repeatEnabled) {
            return RandomOutput(
                value = Random.nextInt(1, max + 1),
                status = "Repeat enabled: random draw from 1..$max"
            )
        }

        if (startFreshCycleOnNextClick) {
            cycleNumber += 1
            startFreshCycleOnNextClick = false
        }

        if (remaining.isEmpty()) {
            remaining.addAll((1..max).shuffled())
        }

        val value = remaining.removeAt(0)
        val status = if (remaining.isEmpty()) {
            startFreshCycleOnNextClick = true
            "Cycle $cycleNumber complete (all numbers used). Next click starts cycle ${cycleNumber + 1}."
        } else {
            "Cycle $cycleNumber in progress: ${remaining.size} number(s) remaining."
        }

        return RandomOutput(value = value, status = status)
    }
}
