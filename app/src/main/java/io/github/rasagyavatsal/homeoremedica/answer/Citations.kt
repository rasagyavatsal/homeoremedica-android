package io.github.rasagyavatsal.homeoremedica.answer

import io.github.rasagyavatsal.homeoremedica.api.Citation

// Ports the web app's citation handling (src/web/Answer.tsx).

const val CITATION_HREF = "#cite-"
private val CODE_SPAN = Regex("```[\\s\\S]*?```|`[^`\\n]*`")
private const val LIST = """\d+(?:\s*[,–-]\s*\d+)*"""
// `[3]`, `[1, 2]` or `[2-4]`, but not a Markdown link's `[text](url)` or `[text][label]`; `[1][2]` is two labels.
private val CITATION = Regex("""\[($LIST)](?!\(|\[(?!$LIST]))""")

/** The numbers a label lists, as the web app reads them: `2-4` is 2, 3 and 4, up to 20 apart. */
private fun numbers(list: String): List<Int> = list.split(',').flatMap { part ->
    val ends = part.split('-', '–').map { it.trim().toIntOrNull() }
    val start = ends.first() ?: return@flatMap listOf(0)
    val end = ends.getOrNull(1)
    if (end == null || end < start || end - start > 20) listOf(start) else (start..end).toList()
}

/**
 * Whether the label from [start] to [end] opens a Markdown link definition such as `[1]: url`: followed
 * by a colon, at the start of a line or after up to three spaces. Mid-sentence, `[1]:` is a citation.
 */
private fun definesLink(markdown: String, start: Int, end: Int): Boolean {
    if (markdown.getOrNull(end) != ':') return false
    val indent = markdown.substring(markdown.lastIndexOf('\n', start - 1) + 1, start)
    return indent.length <= 3 && indent.all { it == ' ' }
}

/** Replace each label outside code and link definitions whose numbers all name a source. */
private fun replaceCitations(markdown: String, sourceCount: Int, replace: (List<Int>) -> String): String {
    val result = StringBuilder()
    var start = 0
    fun prose(end: Int): String {
        val offset = start
        return markdown.substring(start, end).replace(CITATION) { match ->
            val numbers = numbers(match.groupValues[1])
            if (!definesLink(markdown, offset + match.range.first, offset + match.range.last + 1) && numbers.all { it in 1..sourceCount }) {
                replace(numbers)
            } else {
                match.value
            }
        }
    }
    for (code in CODE_SPAN.findAll(markdown)) {
        result.append(prose(code.range.first)).append(code.value)
        start = code.range.last + 1
    }
    return result.append(prose(markdown.length)).toString()
}

/** Turn `[1]`, `[1, 2]` and `[2-4]` labels into citation links, leaving code and unknown numbers untouched. */
fun linkCitations(markdown: String, sourceCount: Int): String =
    replaceCitations(markdown, sourceCount) { numbers -> numbers.joinToString("") { "[$it]($CITATION_HREF$it)" } }

fun citedNumbers(markdown: String, sourceCount: Int): Set<Int> {
    val cited = mutableSetOf<Int>()
    replaceCitations(markdown, sourceCount) { numbers ->
        cited += numbers
        ""
    }
    return cited
}

/** The source numbers an answer lists as references: those it cites, or every one if it cites none. */
fun referencedNumbers(sourceCount: Int, cited: Set<Int>): List<Int> =
    (1..sourceCount).filter { cited.isEmpty() || it in cited }

/** The remedy, or the section for passages about principles rather than a remedy. */
val Citation.label: String get() = remedyName ?: sectionTitle
