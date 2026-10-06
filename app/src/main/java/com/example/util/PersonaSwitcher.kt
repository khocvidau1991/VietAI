package com.example.util

import com.example.data.Persona

/**
 * Parser lệnh chuyển persona trong chat.
 *
 * Ví dụ user gõ:
 *  - "chuyển sang gia sư toán"        → Ambiguous (12 lớp)
 *  - "chuyển sang gia sư toán lớp 10" → Matched
 *  - "chuyển sang daikin"             → Matched
 *  - "đổi sang tủ lạnh"               → Matched
 *  - "mở điện lạnh"                   → Ambiguous (17 persona)
 *
 * Nếu user đang trả lời câu hỏi làm rõ (pendingQuery != null), ghép câu
 * trả lời vào query cũ rồi search lại.
 */
object PersonaSwitcher {

    /** Các prefix kích hoạt lệnh chuyển persona. */
    private val TRIGGERS = listOf(
        "chuyển sang ", "chuyển qua ", "chuyển tới ",
        "đổi sang ", "đổi qua ", "đổi thành ",
        "chuyển nhân vật ", "đổi nhân vật ", "thay nhân vật ",
        "dùng nhân vật ", "dùng ", "chọn nhân vật ", "chọn ",
        "mở nhân vật ", "mở ", "bật nhân vật ", "bật ",
        "gọi nhân vật ", "gọi "
    )

    /** Từ tối thiểu để search — bỏ từ ngắn gây noise. */
    private const val MIN_WORD_LEN = 2

    sealed class Result {
        data class Matched(val persona: Persona) : Result()
        data class Ambiguous(val query: String, val candidates: List<Persona>) : Result()
        object None : Result()
    }

    /**
     * Parse input user.
     * @param rawInput tin nhắn user vừa gửi
     * @param all tất cả persona có trong DB
     * @param pendingQuery nếu != null → user đang trả lời câu hỏi làm rõ
     */
    fun parse(
        rawInput: String,
        all: List<Persona>,
        pendingQuery: String?
    ): Result {
        val input = rawInput.trim().lowercase()
        if (input.isBlank()) return Result.None

        // Case 1: đang chờ làm rõ → ghép pending + input
        if (!pendingQuery.isNullOrBlank()) {
            val combined = "$pendingQuery $input"
            val matches = search(combined, all)
            return when {
                matches.size == 1 -> Result.Matched(matches.first())
                matches.size > 1 -> Result.Ambiguous(combined, matches)
                else -> Result.None  // câu trả lời không match → coi như chat bình thường
            }
        }

        // Case 2: detect trigger prefix
        val target = extractTarget(input) ?: return Result.None
        if (target.isBlank()) return Result.None

        val matches = search(target, all)
        return when {
            matches.size == 1 -> Result.Matched(matches.first())
            matches.size > 1 -> Result.Ambiguous(target, matches)
            else -> Result.None
        }
    }

    private fun extractTarget(input: String): String? {
        // Sắp xếp trigger dài trước để match đúng ("chuyển nhân vật " trước "chuyển ")
        for (t in TRIGGERS.sortedByDescending { it.length }) {
            if (input.startsWith(t)) {
                return input.removePrefix(t).trim()
            }
        }
        return null
    }

    /**
     * Search AND: tất cả từ (>=2 ký tự) phải xuất hiện trong haystack.
     * Haystack = tên + mô tả + category (lowercase).
     */
    fun search(query: String, all: List<Persona>): List<Persona> {
        val words = query.lowercase()
            .split(Regex("\\s+"))
            .filter { it.length >= MIN_WORD_LEN }
            .distinct()
        if (words.isEmpty()) return emptyList()

        return all.filter { p ->
            val haystack = "${p.name} ${p.description} ${p.category}".lowercase()
            words.all { w -> haystack.contains(w) }
        }
    }

    /**
     * Diễn giải câu hỏi làm rõ khi Ambiguous.
     * VD: candidates = 5 persona "Gia sư Toán" các lớp → hỏi "lớp mấy"
     *     candidates = 17 persona Điện lạnh → hỏi "chuyên về gì"
     */
    fun buildClarificationQuestion(result: Result.Ambiguous): String {
        val names = result.candidates.map { it.name }.distinct()
        val categories = result.candidates.map { it.category }.distinct()
        val count = result.candidates.size

        val sample = if (categories.size <= 5) {
            categories.joinToString(", ") { it.substringAfterLast("/") }
        } else {
            names.take(5).joinToString(", ") + "…"
        }

        return when {
            count <= 1 -> "Mình chưa rõ bạn muốn nhân vật nào."
            count <= 5 -> "Mình có ${count} nhân vật phù hợp: $sample. Bạn muốn chọn cái nào?"
            else -> "Có tới ${count} nhân vật khớp với \"${result.query}\". " +
                "Bạn nói cụ thể hơn nhé. Ví dụ: \"lớp 10\", \"Daikin\", \"tủ lạnh\", \"máy giặt\"…"
        }
    }
}
