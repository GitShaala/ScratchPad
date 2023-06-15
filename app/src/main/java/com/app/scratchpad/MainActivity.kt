package com.app.scratchpad

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.app.scratchpad.ui.theme.ScratchPadTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScratchPadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    ScratchPad()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("MutableCollectionMutableState", "UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    fun ScratchPad() {
        val path = remember {
            mutableStateOf(mutableListOf<PathState>())
        }
        Scaffold(topBar = {
            PaintAppBar {
                path.value = mutableListOf()
            }
        }) {
            PaintBody(path = path, it)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PaintAppBar(onDelete: () -> Unit) {
        TopAppBar(title = {
            Text(text = "ScratchPad")
        }, actions = {
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
            }
        })
    }

    @SuppressLint("MutableCollectionMutableState")
    @Composable
    fun PaintBody(path: MutableState<MutableList<PathState>>, paddingValues: PaddingValues) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val drawColor = remember {
                mutableStateOf(Color.Black)
            }

            val drawBrush = remember {
                mutableStateOf(5f)
            }

            val usedColors = remember {
                mutableStateOf(mutableSetOf(Color.Black, Color.White, Color.Gray))
            }

            path.value.add(PathState(Path(), color = drawColor.value, stroke = drawBrush.value))

            PaintCanvas(
                drawColor = drawColor,
                drawBrush = drawBrush,
                usedColor = usedColors,
                path = path.value
            )

            DrawTools(drawColor = drawColor, drawBrush = drawBrush, usedColor = usedColors.value)

        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun PaintCanvas(
        drawColor: MutableState<Color>,
        drawBrush: MutableState<Float>,
        usedColor: MutableState<MutableSet<Color>>,
        path: MutableList<PathState>
    ) {
        val currentPath = path.last().path
        val movePath = remember {
            mutableStateOf<Offset?>(null)
        }

        Canvas(modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp)
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        currentPath.moveTo(it.x, it.y)
                        usedColor.value.add(drawColor.value)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        movePath.value = Offset(it.x, it.y)
                    }

                    else -> {
                        movePath.value = null
                    }
                }
                true
            }, onDraw = {
            movePath.value?.let {
                currentPath.lineTo(it.x, it.y)
                drawPath(
                    path = currentPath,
                    color = drawColor.value,
                    style = Stroke(drawBrush.value)
                )
            }

            path.forEach {
                drawPath(
                    it.path, color = it.color, style = Stroke(it.stroke)
                )
            }
        })
    }

    @Composable
    fun DrawTools(
        drawColor: MutableState<Color>,
        drawBrush: MutableState<Float>,
        usedColor: MutableSet<Color>
    ) {
        var showBrushes by remember { mutableStateOf(false) }
        val strokes = remember {
            ((1..50) step 5).toList()
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, top = 8.dp, end = 8.dp)
            ) {
                ColorPicker(
                    Modifier
                        .weight(1f)
                        .height(35.dp).padding(end = 20.dp)
                ) {
                    drawColor.value = it
                }

                FloatingActionButton(onClick = {
                    showBrushes = !showBrushes
                }, modifier = Modifier.size(35.dp)) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.paint_brush),
                        contentDescription = null,
                        tint = drawColor.value
                    )
                }

                AnimatedVisibility(visible = showBrushes) {
                    LazyColumn {
                        items(strokes) {
                            IconButton(
                                onClick = {
                                    drawBrush.value = it.toFloat()
                                    showBrushes = false
                                }, modifier = with(Modifier) {
                                    padding(8.dp)
                                                                .border(
                                                                    border = BorderStroke(
                                                                        with(
                                                                            LocalDensity.current
                                                                        ) {
                                                                            it.dp
                                                                        }, color = Color.Gray
                                                                    ), shape = CircleShape
                                                                )
                                }
                            ) {

                            }

                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .horizontalGradientBackground(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            Color.Black
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .horizontalScroll(
                        rememberScrollState()
                    )
                    .animateContentSize()
            ) {
                usedColor.forEach {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = it,
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable {
                                drawColor.value = it
                            }
                    )
                }
            }

        }
    }
}

fun Modifier.horizontalGradientBackground(
    colors: List<Color>
) = gradientBackground(colors) { gradientColors, size ->
    Brush.horizontalGradient(
        colors = gradientColors,
        startX = 0f,
        endX = size.width
    )
}

fun Modifier.gradientBackground(
    colors: List<Color>,
    brushProvider: (List<Color>, Size) -> Brush
): Modifier = composed {
    var size by remember { mutableStateOf(Size.Zero) }
    val gradient = remember(colors, size) { brushProvider(colors, size) }
    drawWithContent {
        size = this.size
        drawRect(brush = gradient)
        drawContent()
    }
}