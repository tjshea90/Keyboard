package app.kestrel.core.input.editor

/**
 * Counters only (plan 6.8 and 8.5 layer 1): how the keyboard was used and how often its beliefs about the editor had to be repaired. NEVER any text, word or
 * position. Read by the Diagnostics screen; single-threaded (the UI thread).
 */
class InputStats {
    @JvmField var keys = 0
    @JvmField var backspaces = 0
    @JvmField var enters = 0
    @JvmField var glides = 0
    @JvmField var swaps = 0
    @JvmField var cursorMoves = 0
    /** Selection reports that were not the keyboard's own edits: the user or the app moved or changed the text. */
    @JvmField var foreignReports = 0
    /** Times an editor call failed or surprised the keyboard and it re-read the editor. */
    @JvmField var resyncs = 0

    /** R28b: the touch stream of the glides (numbers only): how many strokes, their touch points and duration, the time to the first move, second-thumb hand-overs. Read by Diagnostics, so a report from the tablet says how its screen delivers touches. */
    @JvmField var strokes = 0
    @JvmField var strokePoints = 0L
    @JvmField var strokeMs = 0L
    @JvmField var firstMoveMs = 0L
    @JvmField var firstMoveMax = 0
    @JvmField var takeovers = 0
    @JvmField var restingIgnored = 0
    @JvmField var paddingStarts = 0
    /** R38: the erase key pressed by the other thumb during a glide, a finger kept that landed during a glide and had not moved yet, touches that began in the margin beside a squeezed keyboard. */
    @JvmField var sideErases = 0
    @JvmField var adoptedFingers = 0
    @JvmField var marginTouches = 0

    fun touchLine(): String {
        if (strokes == 0 && takeovers == 0 && restingIgnored == 0 && paddingStarts == 0 && sideErases == 0 && adoptedFingers == 0 && marginTouches == 0) return ""
        val n = if (strokes == 0) 1 else strokes
        val pts = strokePoints / n; val ms = strokeMs / n
        val every = if (strokePoints > strokes) strokeMs.toFloat() / (strokePoints - strokes) else 0f
        return "$strokes glide strokes, avg $pts touch points in $ms ms (a point every ${"%.1f".format(every)} ms), first move after avg ${firstMoveMs / n} ms (worst $firstMoveMax ms); second thumb took over $takeovers times, a resting second finger was ignored $restingIgnored times; $paddingStarts strokes began on the padding above the top row; erase pressed during the other thumb's glide $sideErases times, a finger that landed during a glide and moved later was kept $adoptedFingers times, $marginTouches touches began in the margin beside a squeezed keyboard"
    }

    fun line(): String = "keys $keys, backspaces $backspaces, enters $enters, glides $glides, picks $swaps, cursor moves $cursorMoves, foreign changes $foreignReports, resyncs $resyncs"
}
