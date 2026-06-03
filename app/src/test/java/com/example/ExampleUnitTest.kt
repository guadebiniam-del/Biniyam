package com.example

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun fixDashboardScreen() {
    val path = "src/main/java/com/example/ui/DashboardScreen.kt"
    val file = File(path).let { if (it.exists()) it else File("app/$path") }
    if (!file.exists()) {
        println("File not found at standard paths")
        return
    }
    var text = file.readText()

    // 1. Fix Row syntax error
    val updated1 = text.replace(Regex("""color = if \(absentDays > 0\) BentoAlertText else BentoSubText\s+Row\("""), 
                 "color = if (absentDays > 0) BentoAlertText else BentoSubText\n                                                        )\n                                                        Row(")

    // 2. Fix duplicated corrupted block
    val updated2 = updated1.replace(Regex("""\}\s*ት[^\n]*deduction[^\n]*\s*style = MaterialTheme\.typography\.bodySmall\s*,\s*fontWeight = FontWeight\.Bold\s*,\s*color = BentoAlertText\s*\)\s*\}\s*\}\s*\}\s*\}"""), "")

    // Fallback: let's also remove any fragment starting with "ት፡" after a closing column brace
    val updated3 = updated2.replace(Regex("""\}\s*ት[^\n]*deduction[^\n]*\s*style = MaterialTheme\.typography\.bodySmall\s*,\s*fontWeight = FontWeight\.Bold\s*,\s*color = BentoAlertText\s*\)\s*\}\s*\}\s*\}\s*\}"""), "")

    // Let's write a very direct line-based replace as well for maximum robustness
    var lines = updated3.lines().toMutableList()
    var rowIdx = -1
    for (i in lines.indices) {
        if (lines[i].contains("Row(") && i > 0 && lines[i-1].contains("BentoAlertText else BentoSubText") && !lines[i-1].endsWith(")")) {
            rowIdx = i
            break
        }
    }
    if (rowIdx != -1) {
        lines[rowIdx-1] = lines[rowIdx-1] + " )"
        println("Fixed Row parenthesis line-by-line")
    }

    var cleanLines = mutableListOf<String>()
    var skipCount = 0
    for (i in lines.indices) {
        if (skipCount > 0) {
            skipCount--
            continue
        }
        if (lines[i].contains("deduction") && lines[i].contains("ት") && lines[i].contains("ብር") && i + 8 < lines.size) {
            if (lines[i+1].contains("style") && lines[i+2].contains("fontWeight") && lines[i+3].contains("color = BentoAlertText")) {
                println("Found corrupted block. Skipping lines.")
                skipCount = 9 // Skip the 10 lines of corrupted block
                continue
            }
        }
        cleanLines.add(lines[i])
    }

    file.writeText(cleanLines.joinToString("\n"))
    println("Saved updated DashboardScreen.kt")
  }
}
