import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

enum class Player {
    BLUE, RED, NONE
}

fun hasWinningLine(boardMatrix: List<List<Player>>, winCondition: Int, player: Player): Boolean {
    if (winCondition <= 0 || player == Player.NONE || boardMatrix.isEmpty()) return false
    val rowCount = boardMatrix.size
    val directions = listOf(
        0 to 1,   // horizontal
        1 to 0,   // vertical
        1 to 1,   // diagonal down-right
        1 to -1   // diagonal down-left
    )

    for (row in boardMatrix.indices) {
        for (column in boardMatrix[row].indices) {
            if (boardMatrix[row][column] != player) continue

            for ((rowStep, columnStep) in directions) {
                var count = 1
                var nextRow = row + rowStep
                var nextColumn = column + columnStep

                while (
                    nextRow in 0 until rowCount &&
                    nextColumn in boardMatrix[nextRow].indices &&
                    boardMatrix[nextRow][nextColumn] == player
                ) {
                    count++
                    if (count >= winCondition) return true
                    nextRow += rowStep
                    nextColumn += columnStep
                }
            }
        }
    }
    return false
}

fun isBoardFull(boardMatrix: List<List<Player>>): Boolean =
    boardMatrix.isNotEmpty() && boardMatrix.all { row -> row.none { cell -> cell == Player.NONE } }

fun main() {
    renderComposable(rootElementId = "root") {
        Body()
    }
}

@Composable
fun Body() {
    var firstNumber by remember { mutableStateOf<Number?>(null) }
    var winCondition by remember { mutableStateOf<Number?>(null) }
    var gridSize by remember { mutableStateOf(0) }
    var boardMatrix by remember { mutableStateOf<List<List<Player>>>(emptyList()) }
    var playerToMove by remember { mutableStateOf(Player.RED) }
    var winner by remember { mutableStateOf<Player?>(null) }
    var hoveredColumn by remember { mutableStateOf<Int?>(null) }
    val isDraw = winner == null && isBoardFull(boardMatrix)

    Div (attrs = { classes("resp-text") }){
        Text("Size of board:", )
    }
    Input(
        type = InputType.Number,
        attrs = {
            classes("game-input")
            onInput { event ->
                firstNumber = event.value
            }
        }
    )

    Div (attrs = { classes("resp-text") }){
        Text("Win condition (how many in a row):")
    }
    Input(
        type = InputType.Number,
        attrs = {
            classes("game-input")
            onInput { event ->
                winCondition = event.value
            }
        }
    )

    Div {
        Button(
            attrs = {
                onClick { _ ->
                    gridSize = firstNumber?.toInt() ?: 0
                    boardMatrix = List(gridSize) { List(gridSize) { Player.NONE } }
                    playerToMove = Player.RED
                    winner = null
                }
            }
        ) {
            Text("Start")
        }
    }

    if (boardMatrix.isNotEmpty()) {
        Div(attrs = {
            classes("board")
            attr("style", "--grid-size: ${boardMatrix.size};")
            onMouseLeave { hoveredColumn = null }
        }) {
            repeat(boardMatrix.size) { rowIndex ->
                Div(attrs = { classes("square-row") }) {
                    repeat(boardMatrix[rowIndex].size) { columnIndex ->
                        val cellPlayer = boardMatrix[rowIndex][columnIndex]
                        Div(attrs = {
                            classes("square")
                            if (hoveredColumn == columnIndex) {
                                classes("highlightedSquare")
                            }
                            onMouseEnter { hoveredColumn = columnIndex }
                            onClick {
                                if (winner != null || isDraw) return@onClick

                                val targetRow = boardMatrix.indices.reversed()
                                    .firstOrNull { row -> boardMatrix[row][columnIndex] == Player.NONE }
                                if (targetRow != null) {
                                    val currentPlayer = playerToMove
                                    val updatedBoardMatrix = boardMatrix.mapIndexed { row, currentRow ->
                                        if (row == targetRow) {
                                            currentRow.mapIndexed { column, cell ->
                                                if (column == columnIndex) currentPlayer else cell
                                            }
                                        } else {
                                            currentRow
                                        }
                                    }
                                    boardMatrix = updatedBoardMatrix
                                    val targetWinCondition = winCondition?.toInt() ?: 0
                                    if (hasWinningLine(updatedBoardMatrix, targetWinCondition, currentPlayer)) {
                                        winner = currentPlayer
                                    } else if (!isBoardFull(updatedBoardMatrix)) {
                                        playerToMove = if (currentPlayer == Player.BLUE) Player.RED else Player.BLUE
                                    }
                                }
                            }
                        }) {
                            when (cellPlayer) {
                                Player.BLUE -> Div(attrs = { classes("piece", "piece-blue") })
                                Player.RED -> Div(attrs = { classes("piece", "piece-red") })
                                Player.NONE -> {}
                            }
                        }
                    }
                }
            }
            Div(attrs = { classes("board-status", "resp-text") }) {
                if (winner != null) {
                    Text("$winner wins!")
                } else if (isDraw) {
                    Text("Draw!")
                } else {
                    Text("$playerToMove to move")
                }
            }
        }
    }
}
