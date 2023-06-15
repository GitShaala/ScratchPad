package com.app.scratchpad

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

@Composable
fun ColorPicker(modifier : Modifier = Modifier,onColorSelected: (Color) -> Unit) {
    val scope = rememberCoroutineScope()
    val interactionSource = remember {
        MutableInteractionSource()
    }
    val pressOffset = remember {
        mutableStateOf(Offset.Zero)
    }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .emitDragGesture(interactionSource),
        onDraw = {
            val drawScopeSize = size
            val bitmap = Bitmap.createBitmap(
                size.width.toInt(),
                size.height.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val hueCanvas = Canvas(bitmap)
            val hueTrack = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            val hueColors = IntArray(hueTrack.width().toInt())
            var hue = 0f
            for (i in hueColors.indices) {
                hueColors[i] = AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))
                hue += 360f / hueColors.size
            }
            val linePaint = Paint()
            linePaint.strokeWidth = 0f
            for (i in hueColors.indices) {
                linePaint.color = hueColors[i]
                hueCanvas.drawLine(i.toFloat(), 0f, i.toFloat(), hueTrack.bottom, linePaint)
            }

            drawBitmap(bitmap, hueTrack)

            fun pointToHue(pointX: Float): Float {
                Log.d("pointToHue : pointX => ",pointX.toString())
                Log.d("pointToHue : left => ",hueTrack.left.toString())
                Log.d("pointToHue : right => ",hueTrack.right.toString())

                val width = hueTrack.width()
                val x = when {
                    pointX < hueTrack.left -> 0f
                    pointX > hueTrack.right -> width
                    else -> pointX - hueTrack.left
                }
                val outPut =  x * 360f / width
                Log.d("pointToHue : output => ",outPut.toString())
                return outPut
            }


            fun convertToColor(coloInFloat: Float): Color {
                val color = AndroidColor.HSVToColor(floatArrayOf(coloInFloat, coloInFloat, coloInFloat))
                return Color(color)
            }

            scope.collectForPress(interactionSource = interactionSource) { pressPosition ->
                val pressPos = pressPosition.x.coerceIn(0f..drawScopeSize.width)
                pressOffset.value = Offset(pressPos, 0f)
                val selectedHue = pointToHue(pressPos)
                onColorSelected(convertToColor(selectedHue))
            }

            drawCircle(
                Color.White,
                size.height / 2,
                center = Offset(pressOffset.value.x, size.height / 2),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    )
}

fun DrawScope.drawBitmap(bitmap: Bitmap, panel: RectF) {
    drawIntoCanvas {
        it.nativeCanvas.drawBitmap(
            bitmap, null, panel.toRect(), null
        )
    }
}

fun CoroutineScope.collectForPress(
    interactionSource: InteractionSource,
    setOffset: (Offset) -> Unit
) {
    launch {
        interactionSource.interactions.collect() { interaction ->
            (interaction as? PressInteraction.Press)?.pressPosition?.let(setOffset)
        }
    }
}

private fun Modifier.emitDragGesture(interactionSource: MutableInteractionSource): Modifier =
    composed {
        val scope = rememberCoroutineScope()
        pointerInput(Unit) {
            detectDragGestures { input, _ ->
                scope.launch {
                    interactionSource.emit(PressInteraction.Press(input.position))
                }
            }
        }.clickable(interactionSource, null) {}
    }