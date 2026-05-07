import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        Body()
    }
}

@Composable
fun Body() {
    var firstNumber by remember { mutableStateOf<Number?>(null) }
    var secondNumber by remember { mutableStateOf<Number?>(null) }
    var gridSize by remember { mutableStateOf(0) }
    var blueToMove : Boolean by remember { mutableStateOf(false) }
    var hoveredColumn by remember { mutableStateOf<Int?>(null) }

    Div {
        Text("Size of board:")
    }
    Input(
        type = InputType.Number,
        attrs = {
            onInput { event ->
                firstNumber = event.value
            }
        }
    )

    Div {
        Text("Win condition (how many in a row):")
    }
    Input(
        type = InputType.Number,
        attrs = {
            onInput { event ->
                secondNumber = event.value
            }
        }
    )

    Div {
        Button(
            attrs = {
                onClick { _ ->
                    gridSize = firstNumber?.toInt() ?: 0
                }
            }
        ) {
            Text("Start")
        }
    }

    if (gridSize > 0) {
        Div(attrs = {
            classes("board")
            onMouseLeave { hoveredColumn = null }
        }) {
            repeat(gridSize) {
                Div(attrs = { classes("square-row") }) {
                    repeat(gridSize) { columnIndex ->
                        Div(attrs = {
                            classes("square")
                            if (hoveredColumn == columnIndex) {
                                classes("highlightedSquare")
                            }
                            onMouseEnter { hoveredColumn = columnIndex }
                            onClick { blueToMove = !blueToMove }
                        }) {
                        }
                    }
                }
            }
        }
    }
    Div {
        Text(if (blueToMove) "BLUE to move" else "RED to move")
    }
}
