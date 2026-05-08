import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HelperFunctionsTest {

    @Test
    fun hasWinningLine_detectsHorizontalWin() {
        val board = listOf(
            listOf(Player.NONE, Player.NONE, Player.NONE, Player.NONE),
            listOf(Player.NONE, Player.NONE, Player.NONE, Player.NONE),
            listOf(Player.BLUE, Player.BLUE, Player.BLUE, Player.BLUE),
            listOf(Player.RED, Player.NONE, Player.RED, Player.NONE)
        )

        assertTrue(hasWinningLine(board, 4, Player.BLUE))
        assertFalse(hasWinningLine(board, 4, Player.RED))
    }

    @Test
    fun hasWinningLine_detectsVerticalWin() {
        val board = listOf(
            listOf(Player.NONE, Player.RED, Player.NONE, Player.NONE),
            listOf(Player.NONE, Player.RED, Player.NONE, Player.NONE),
            listOf(Player.NONE, Player.RED, Player.NONE, Player.NONE),
            listOf(Player.NONE, Player.RED, Player.NONE, Player.NONE)
        )

        assertTrue(hasWinningLine(board, 4, Player.RED))
    }

    @Test
    fun hasWinningLine_detectsDiagonalWin() {
        val board = listOf(
            listOf(Player.BLUE, Player.NONE, Player.NONE, Player.NONE),
            listOf(Player.RED, Player.BLUE, Player.NONE, Player.NONE),
            listOf(Player.RED, Player.RED, Player.BLUE, Player.NONE),
            listOf(Player.NONE, Player.NONE, Player.NONE, Player.BLUE)
        )

        assertTrue(hasWinningLine(board, 4, Player.BLUE))
    }

    @Test
    fun hasWinningLine_rejectsInvalidInputs() {
        val board = listOf(
            listOf(Player.RED, Player.RED),
            listOf(Player.NONE, Player.NONE)
        )

        assertFalse(hasWinningLine(board, 0, Player.RED))
        assertFalse(hasWinningLine(board, 2, Player.NONE))
        assertFalse(hasWinningLine(emptyList(), 2, Player.RED))
    }

    @Test
    fun isBoardFull_returnsExpectedResult() {
        val fullBoard = listOf(
            listOf(Player.RED, Player.BLUE),
            listOf(Player.BLUE, Player.RED)
        )
        val boardWithEmpty = listOf(
            listOf(Player.RED, Player.NONE),
            listOf(Player.BLUE, Player.RED)
        )

        assertTrue(isBoardFull(fullBoard))
        assertFalse(isBoardFull(boardWithEmpty))
        assertFalse(isBoardFull(emptyList()))
    }

    @Test
    fun serializeAndDeserialize_roundTrip() {
        val state = SavedGameState(
            firstNumber = 7,
            winCondition = 4,
            gridSize = 7,
            boardMatrix = listOf(
                listOf(Player.NONE, Player.NONE, Player.NONE),
                listOf(Player.RED, Player.BLUE, Player.NONE),
                listOf(Player.RED, Player.BLUE, Player.RED)
            ),
            playerToMove = Player.BLUE,
            winner = null
        )

        val deserialized = deserializeGameState(serializeGameState(state))
        assertNotNull(deserialized)
        assertEquals(state, deserialized)
    }

    @Test
    fun deserializeGameState_rejectsMalformedData() {
        assertNull(deserializeGameState("bad|state"))
        assertNull(deserializeGameState("1|4|4|RED||NONE,RED;RED,INVALID"))
    }

    @Test
    fun playerFromStorageValue_mapsExpectedValues() {
        assertEquals(Player.BLUE, playerFromStorageValue("BLUE"))
        assertEquals(Player.RED, playerFromStorageValue("RED"))
        assertEquals(Player.NONE, playerFromStorageValue("NONE"))
        assertNull(playerFromStorageValue("???"))
    }
}
