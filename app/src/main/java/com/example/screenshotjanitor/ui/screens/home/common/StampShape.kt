package com.example.screenshotjanitor.ui.screens.home.common

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.sin

object StampShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val amplitude = 3.5f * density.density
        val waves = 3
        val pointsPerWave = 16
        val totalPoints = waves * pointsPerWave

        val path = Path().apply {
            moveTo(0f, 0f)

            for (i in 1..totalPoints) {
                val t = i.toFloat() / totalPoints
                val y = h * t
                val angle = t * waves * 2f * PI.toFloat()
                val x = amplitude * sin(angle)
                lineTo(x, y)
            }

            lineTo(w, h)

            for (i in 1..totalPoints) {
                val t = i.toFloat() / totalPoints
                val y = h * (1f - t)
                val angle = t * waves * 2f * PI.toFloat()
                val x = w + amplitude * sin(angle)
                lineTo(x, y)
            }

            close()
        }

        return Outline.Generic(path)
    }
}