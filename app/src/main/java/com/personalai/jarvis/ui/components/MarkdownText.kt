package com.personalai.jarvis.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.personalai.jarvis.utils.FileOpener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalai.jarvis.ui.theme.JarvisBackground
import com.personalai.jarvis.ui.theme.JarvisBorder
import com.personalai.jarvis.ui.theme.JarvisCyan
import com.personalai.jarvis.ui.theme.JarvisSurface
import com.personalai.jarvis.ui.theme.TextMuted
import com.personalai.jarvis.ui.theme.TextPrimary
import com.personalai.jarvis.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Renders structured Markdown with support for headings, bold, italic,
 * inline code, code blocks with syntax styling & copy, bullet lists,
 * and numbered lists.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val headingFontSize = when (block.level) {
                        1 -> 18.sp
                        2 -> 16.sp
                        else -> 15.sp
                    }
                    Text(
                        text = parseInlineMarkdown(block.text, textColor),
                        fontSize = headingFontSize,
                        fontWeight = FontWeight.Bold,
                        color = JarvisCyan,
                        lineHeight = (headingFontSize.value + 6).sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(
                        language = block.language,
                        code = block.code
                    )
                }

                is MarkdownBlock.FileCard -> {
                    FileCardView(fileCard = block)
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(JarvisCyan)
                        )
                        ClickableMarkdownParagraph(
                            text = block.text,
                            textColor = textColor,
                            fontSize = fontSize,
                            lineHeight = lineHeight
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = JarvisCyan,
                            modifier = Modifier.width(22.dp)
                        )
                        ClickableMarkdownParagraph(
                            text = block.text,
                            textColor = textColor,
                            fontSize = fontSize,
                            lineHeight = lineHeight
                        )
                    }
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = JarvisBorder.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    ClickableMarkdownParagraph(
                        text = block.text,
                        textColor = textColor,
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    )
                }
            }
        }
    }
}

@Composable
private fun ClickableMarkdownParagraph(
    text: String,
    textColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val annotatedString = remember(text, textColor) { parseInlineMarkdown(text, textColor) }
    val hasLinks = remember(annotatedString) {
        annotatedString.getStringAnnotations(tag = "URL", start = 0, end = annotatedString.length).isNotEmpty()
    }

    if (hasLinks) {
        ClickableText(
            text = annotatedString,
            style = androidx.compose.ui.text.TextStyle(
                color = textColor,
                fontSize = fontSize,
                lineHeight = lineHeight
            ),
            modifier = modifier,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val target = annotation.item
                        if (target.startsWith("file://") || target.startsWith("/")) {
                            FileOpener.openFile(context, target)
                        } else if (target.startsWith("http://") || target.startsWith("https://")) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                    }
            }
        )
    } else {
        Text(
            text = annotatedString,
            fontSize = fontSize,
            color = textColor,
            lineHeight = lineHeight,
            modifier = modifier
        )
    }
}

@Composable
private fun FileCardView(
    fileCard: MarkdownBlock.FileCard
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(JarvisBackground)
            .border(1.dp, JarvisCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .clickable {
                FileOpener.openFile(context, fileCard.path)
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JarvisCyan.copy(alpha = 0.12f))
                .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fileCard.icon.ifBlank { "📄" },
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name, Size, Path
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = fileCard.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (fileCard.size.isNotBlank()) {
                Text(
                    text = fileCard.size,
                    fontSize = 11.sp,
                    color = JarvisCyan,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = fileCard.path,
                fontSize = 10.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Open Folder Action Button
        IconButton(
            onClick = { FileOpener.openFolder(context, fileCard.path) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = "Open Folder",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Open File Action Button
        IconButton(
            onClick = { FileOpener.openFile(context, fileCard.path) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open File",
                tint = JarvisCyan,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CodeBlockView(
    language: String,
    code: String
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisBackground)
            .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
    ) {
        // Code Block Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisSurface)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "CODE" }.uppercase(),
                color = JarvisCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                    copied = true
                    coroutineScope.launch {
                        delay(2000)
                        copied = false
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = if (copied) Color(0xFF10A37F) else TextMuted,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Code Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text(
                text = code,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// -------------------------------------------------------------
// Markdown AST Model & Parser
// -------------------------------------------------------------

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class FileCard(val name: String, val path: String, val size: String = "", val icon: String = "📄") : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // Code block start ```
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            continue
        }

        // Horizontal Rule
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // Headings (#, ##, ###)
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val headingText = trimmed.drop(level).trim()
            blocks.add(MarkdownBlock.Heading(level.coerceAtMost(3), headingText))
            i++
            continue
        }

        // File Card Item (e.g. "1. 🖼️ [filename](file:///path) (size)" or "🖼️ [filename](file:///path)" or "[filename](file:///path)")
        val fileRegex = Regex("""^(?:(\d+)[\.\)]\s+)?([🖼️🎵🎬📄📁]?)\s*\[([^\]]+)\]\((file:///[^\)]+|/[^\)]+)\)(?:\s*\(([^\)]+)\))?""")
        val fileMatch = fileRegex.find(trimmed)
        if (fileMatch != null) {
            val icon = fileMatch.groupValues[2].ifBlank { "📄" }
            val name = fileMatch.groupValues[3]
            val path = fileMatch.groupValues[4]
            val size = fileMatch.groupValues[5]

            // If next line is e.g. "Path: `...`", consume it
            if (i + 1 < lines.size && lines[i + 1].trim().startsWith("Path:", ignoreCase = true)) {
                i++
            }

            blocks.add(MarkdownBlock.FileCard(name = name, path = path, size = size, icon = icon))
            i++
            continue
        }

        // Bullet Items (*, -, •)
        val bulletRegex = Regex("""^[\*\-•]\s+(.*)""")
        val bulletMatch = bulletRegex.find(trimmed)
        if (bulletMatch != null) {
            blocks.add(MarkdownBlock.BulletItem(bulletMatch.groupValues[1]))
            i++
            continue
        }

        // Numbered Items (1. , 2. )
        val numberedRegex = Regex("""^(\d+)[\.\)]\s+(.*)""")
        val numMatch = numberedRegex.find(trimmed)
        if (numMatch != null) {
            blocks.add(MarkdownBlock.NumberedItem(numMatch.groupValues[1], numMatch.groupValues[2]))
            i++
            continue
        }

        // Normal paragraph text (can accumulate consecutive lines)
        val paragraphLines = mutableListOf<String>()
        paragraphLines.add(line)
        i++
        while (i < lines.size) {
            val nextLine = lines[i]
            val nextTrimmed = nextLine.trim()
            if (nextTrimmed.isEmpty() ||
                nextTrimmed.startsWith("```") ||
                nextTrimmed.startsWith("#") ||
                nextTrimmed.startsWith("* ") ||
                nextTrimmed.startsWith("- ") ||
                nextTrimmed.startsWith("• ") ||
                nextTrimmed.matches(Regex("""^\d+[\.\)]\s+.*""")) ||
                nextTrimmed == "---"
            ) {
                break
            }
            paragraphLines.add(nextLine)
            i++
        }
        blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n")))
    }

    return blocks
}

/**
 * Parses inline markdown:
 * - **bold** or __bold__ -> Bold
 * - *italic* or _italic_ -> Italic
 * - `code` -> Monospace + background
 * - ~~strikethrough~~ -> Strikethrough
 */
fun parseInlineMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            // Inline code: `...`
            if (text[cursor] == '`') {
                val nextTick = text.indexOf('`', cursor + 1)
                if (nextTick != -1) {
                    val codeContent = text.substring(cursor + 1, nextTick)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyan,
                            background = JarvisSurface,
                            fontSize = 13.sp
                        )
                    )
                    append(" $codeContent ")
                    pop()
                    cursor = nextTick + 1
                    continue
                }
            }

            // Bold: **...** or __...__
            if (cursor + 1 < length &&
                ((text[cursor] == '*' && text[cursor + 1] == '*') ||
                 (text[cursor] == '_' && text[cursor + 1] == '_'))
            ) {
                val marker = text.substring(cursor, cursor + 2)
                val endIdx = text.indexOf(marker, cursor + 2)
                if (endIdx != -1) {
                    val boldContent = text.substring(cursor + 2, endIdx)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor))
                    append(boldContent)
                    pop()
                    cursor = endIdx + 2
                    continue
                }
            }

            // Italic: *...* or _..._
            if ((text[cursor] == '*' || text[cursor] == '_') &&
                (cursor == 0 || !text[cursor - 1].isLetterOrDigit())
            ) {
                val marker = text[cursor]
                val endIdx = text.indexOf(marker, cursor + 1)
                if (endIdx != -1 && endIdx > cursor + 1 && (endIdx == length - 1 || !text[endIdx + 1].isLetterOrDigit())) {
                    val italicContent = text.substring(cursor + 1, endIdx)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor))
                    append(italicContent)
                    pop()
                    cursor = endIdx + 1
                    continue
                }
            }

            // Strikethrough: ~~...~~
            if (cursor + 1 < length && text[cursor] == '~' && text[cursor + 1] == '~') {
                val endIdx = text.indexOf("~~", cursor + 2)
                if (endIdx != -1) {
                    val strikeContent = text.substring(cursor + 2, endIdx)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = TextMuted))
                    append(strikeContent)
                    pop()
                    cursor = endIdx + 2
                    continue
                }
            }

            // Markdown link: [text](url)
            if (text[cursor] == '[') {
                val closeBracket = text.indexOf(']', cursor + 1)
                if (closeBracket != -1 && closeBracket + 1 < length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = text.substring(cursor + 1, closeBracket)
                        val linkUrl = text.substring(closeBracket + 2, closeParen)
                        pushStringAnnotation(tag = "URL", annotation = linkUrl)
                        pushStyle(
                            SpanStyle(
                                color = JarvisCyan,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        append(linkText)
                        pop()
                        pop()
                        cursor = closeParen + 1
                        continue
                    }
                }
            }

            // Regular character
            append(text[cursor])
            cursor++
        }
    }
}

/**
 * Strips markdown tags (asterisks, code blocks, hashes) for natural TTS speech output.
 */
fun cleanMarkdownForSpeech(raw: String): String {
    var text = raw

    // Remove code blocks
    text = text.replace(Regex("""```[\s\S]*?```"""), "Code block.")

    // Remove inline code backticks
    text = text.replace(Regex("""`([^`]+)`"""), "$1")

    // Remove bold and italic markers
    text = text.replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
    text = text.replace(Regex("""__([^_]+)__"""), "$1")
    text = text.replace(Regex("""\*([^*]+)\*"""), "$1")
    text = text.replace(Regex("""_([^_]+)_"""), "$1")

    // Remove strikethrough
    text = text.replace(Regex("""~~([^~]+)~~"""), "$1")

    // Remove markdown links to just link text
    text = text.replace(Regex("""\[([^\]]+)\]\([^\)]+\)"""), "$1")

    // Remove file path lines for natural speech
    text = text.replace(Regex("""(?m)^\s*Path:\s*`?[^\n]+`?"""), "")

    // Remove headers
    text = text.replace(Regex("""(?m)^#{1,6}\s*"""), "")

    // Remove bullet points
    text = text.replace(Regex("""(?m)^[\*\-•]\s+"""), "")

    // Remove horizontal rules
    text = text.replace(Regex("""(?m)^[-*_]{3,}$"""), "")

    return text.trim()
}
