import androidx.compose.runtime.*
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

enum class Player {
    BLUE, RED, NONE
}

private const val GAME_STATE_STORAGE_KEY = "connect4-game-state-v1"

data class SavedGameState(
    val firstNumber: Int?,
    val winCondition: Int?,
    val gridSize: Int,
    val boardMatrix: List<List<Player>>,
    val playerToMove: Player,
    val winner: Player?
)

fun playerFromStorageValue(value: String): Player? = when (value) {
    "BLUE" -> Player.BLUE
    "RED" -> Player.RED
    "NONE" -> Player.NONE
    else -> null
}

fun encodeBoard(boardMatrix: List<List<Player>>): String =
    boardMatrix.joinToString(";") { row -> row.joinToString(",") { cell -> cell.name } }

fun decodeBoard(rawBoard: String): List<List<Player>>? {
    if (rawBoard.isBlank()) return emptyList()
    val board = mutableListOf<List<Player>>()
    for (rawRow in rawBoard.split(";")) {
        if (rawRow.isBlank()) {
            board.add(emptyList())
            continue
        }
        val row = mutableListOf<Player>()
        for (rawCell in rawRow.split(",")) {
            val player = playerFromStorageValue(rawCell) ?: return null
            row.add(player)
        }
        board.add(row)
    }
    return board
}

fun serializeGameState(state: SavedGameState): String = listOf(
    state.firstNumber?.toString() ?: "",
    state.winCondition?.toString() ?: "",
    state.gridSize.toString(),
    state.playerToMove.name,
    state.winner?.name ?: "",
    encodeBoard(state.boardMatrix)
).joinToString("|")

fun deserializeGameState(rawState: String): SavedGameState? {
    val parts = rawState.split("|", limit = 6)
    if (parts.size != 6) return null

    val firstNumber = parts[0].takeIf { it.isNotBlank() }?.toIntOrNull()
    val winCondition = parts[1].takeIf { it.isNotBlank() }?.toIntOrNull()
    val parsedGridSize = parts[2].toIntOrNull() ?: 0
    val playerToMove = playerFromStorageValue(parts[3]) ?: Player.RED
    val winner = parts[4].takeIf { it.isNotBlank() }?.let(::playerFromStorageValue)
    val boardMatrix = decodeBoard(parts[5]) ?: return null
    val normalizedBoard = if (boardMatrix.isNotEmpty() && boardMatrix.all { row -> row.size == boardMatrix.size }) {
        boardMatrix
    } else {
        emptyList()
    }
    val gridSize = if (normalizedBoard.isNotEmpty()) normalizedBoard.size else parsedGridSize.coerceAtLeast(0)

    return SavedGameState(
        firstNumber = firstNumber,
        winCondition = winCondition,
        gridSize = gridSize,
        boardMatrix = normalizedBoard,
        playerToMove = playerToMove,
        winner = winner
    )
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
    var validationError by remember { mutableStateOf<String?>(null) }
    var gridSize by remember { mutableStateOf(0) }
    var boardMatrix by remember { mutableStateOf<List<List<Player>>>(emptyList()) }
    var playerToMove by remember { mutableStateOf(Player.RED) }
    var winner by remember { mutableStateOf<Player?>(null) }
    var hoveredColumn by remember { mutableStateOf<Int?>(null) }
    var lastMove by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var stateHydrated by remember { mutableStateOf(false) }
    val isDraw = winner == null && isBoardFull(boardMatrix)
    val effectiveWinCondition = winCondition?.toInt()?.takeIf { it > 0 } ?: 4
    val boardSizeInput = firstNumber?.toInt()?.takeIf { it > 0 }
    val noWinWarning = if (boardSizeInput != null && winCondition != null && boardSizeInput < effectiveWinCondition) {
        " (but you won't win)"
    } else {
        ""
    }

    LaunchedEffect(Unit) {
        val restoredState = window.localStorage.getItem(GAME_STATE_STORAGE_KEY)?.let(::deserializeGameState)
        if (restoredState != null) {
            firstNumber = restoredState.firstNumber
            winCondition = restoredState.winCondition
            gridSize = restoredState.gridSize
            boardMatrix = restoredState.boardMatrix
            playerToMove = restoredState.playerToMove
            winner = restoredState.winner
            validationError = null
            hoveredColumn = null
            lastMove = null
        }
        stateHydrated = true
    }

    LaunchedEffect(stateHydrated, firstNumber, winCondition, gridSize, boardMatrix, playerToMove, winner) {
        if (!stateHydrated) return@LaunchedEffect
        val savedState = SavedGameState(
            firstNumber = firstNumber?.toInt(),
            winCondition = winCondition?.toInt(),
            gridSize = gridSize,
            boardMatrix = boardMatrix,
            playerToMove = playerToMove,
            winner = winner
        )
        window.localStorage.setItem(GAME_STATE_STORAGE_KEY, serializeGameState(savedState))
    }



    Div(attrs = { classes("title") }) {
        Text("Play connect $effectiveWinCondition!$noWinWarning")
    }

    Div(attrs = { classes("controls-with-error") }) {
        Div(attrs = { classes("controls-panel") }) {
            Div (attrs = { classes("resp-text") }){
                Text("Size of board:", )
            }
            Input(
                type = InputType.Number,
                attrs = {
                    classes("game-input")
                    onInput { event ->
                        firstNumber = event.value
                        validationError = null
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
                        validationError = null
                    }
                }
            )

            Div {
                Button(
                    attrs = {
                        classes("start-button")
                        onClick { _ ->
                            val requestedGridSize = firstNumber?.toInt()
                            val requestedWinCondition = winCondition?.toInt()
                            validationError = when {
                                (requestedGridSize == null || requestedGridSize <= 0) &&
                                    (requestedWinCondition == null || requestedWinCondition <= 0) ->
                                    "Board size and win condition must be positive numbers."
                                requestedGridSize == null || requestedGridSize <= 0 ->
                                    "Board size must be a positive number."
                                requestedWinCondition == null || requestedWinCondition <= 0 ->
                                    "Win condition must be a positive number."
                                else -> null
                            }
                            if (validationError != null) return@onClick

                            gridSize = requestedGridSize ?: 0
                            boardMatrix = List(gridSize) { List(gridSize) { Player.NONE } }
                            playerToMove = Player.RED
                            winner = null
                            lastMove = null
                        }
                    }
                ) {
                    Text(" Start ")
                }
            }
        }
        if (validationError != null) {
            Div(attrs = { classes("validation-error") }) {
                Text(validationError!!)
            }
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
                                    lastMove = targetRow to columnIndex
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
                                Player.BLUE -> Div(attrs = {
                                    classes("piece", "piece-blue")
                                    if (lastMove?.first == rowIndex && lastMove?.second == columnIndex) {
                                        classes("piece-drop")
                                        attr("style", "--drop-distance: calc(${rowIndex + 1} * var(--cell-size));")
                                    }
                                })
                                Player.RED -> Div(attrs = {
                                    classes("piece", "piece-red")
                                    if (lastMove?.first == rowIndex && lastMove?.second == columnIndex) {
                                        classes("piece-drop")
                                        attr("style", "--drop-distance: calc(${rowIndex + 1} * var(--cell-size));")
                                    }
                                })
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
