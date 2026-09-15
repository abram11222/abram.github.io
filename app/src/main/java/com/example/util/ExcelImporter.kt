package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.example.data.model.Group
import com.example.data.model.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ExcelImporter {

    data class ParsedMemberRow(
        val rowNumber: Int,
        val id: Long? = null,
        val fullName: String,
        val groupName: String,
        val schoolClass: String = "",
        val birthDate: String,
        val phone: String,
        val parentPhone: String,
        val governorate: String,
        val center: String,
        val area: String,
        val street: String,
        val houseNumber: String,
        val addressDetails: String,
        val notes: String,
        val isValid: Boolean,
        val errors: List<String>
    )

    data class ImportPreviewData(
        val totalRows: Int,
        val validCount: Int,
        val invalidCount: Int,
        val detectedColumns: List<String>,
        val previewRows: List<ParsedMemberRow>,
        val allErrors: List<String>
    )

    data class ImportResult(
        val importedCount: Int,
        val updatedCount: Int,
        val ignoredCount: Int,
        val errorsCount: Int,
        val summaryMessage: String
    )

    /**
     * Parses an XLSX file from Uri into structured preview data.
     */
    suspend fun parseExcelForPreview(context: Context, fileUri: Uri): ImportPreviewData =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(fileUri)
                ?: throw IllegalArgumentException("تعذر فتح ملف Excel المحدد")

            inputStream.use { stream ->
                parseXlsxStream(stream)
            }
        }

    private fun parseXlsxStream(stream: InputStream): ImportPreviewData {
        val sharedStrings = mutableListOf<String>()
        val sheetXmlBytesMap = mutableMapOf<String, ByteArray>()
        var targetSheetName = "sheet1.xml"
        val sheetNamesToFiles = mutableMapOf<String, String>()

        val zip = ZipInputStream(stream)
        var entry: ZipEntry? = zip.nextEntry
        while (entry != null) {
            val name = entry.name
            when {
                name == "xl/sharedStrings.xml" -> {
                    sharedStrings.addAll(parseSharedStrings(zip.readBytes()))
                }
                name == "xl/workbook.xml" -> {
                    val workbookMapping = parseWorkbook(zip.readBytes())
                    sheetNamesToFiles.putAll(workbookMapping)
                }
                name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml") -> {
                    val simpleName = name.substringAfterLast("/")
                    sheetXmlBytesMap[simpleName] = zip.readBytes()
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        // Determine which sheet to read: Prefer "Members" or "سجل المخدومين", otherwise first available
        for ((sheetName, fileName) in sheetNamesToFiles) {
            val cleanName = normalizeString(sheetName)
            if (cleanName.contains("member") || cleanName.contains("مخدوم") || cleanName.contains("شمامس")) {
                targetSheetName = fileName
                break
            }
        }

        val sheetBytes = sheetXmlBytesMap[targetSheetName]
            ?: sheetXmlBytesMap.values.firstOrNull()
            ?: throw IllegalStateException("لم يتم العثور على أي صفحات بيانات داخل ملف Excel")

        return parseWorksheet(sheetBytes, sharedStrings)
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(xmlBytes.inputStream(), "UTF-8")

        var eventType = parser.eventType
        var inText = false
        var currentText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tag == "t") {
                        inText = true
                        currentText = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inText) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tag == "t") {
                        inText = false
                    } else if (tag == "si") {
                        list.add(currentText.toString())
                        currentText = StringBuilder()
                    }
                }
            }
            eventType = parser.next()
        }
        return list
    }

    private fun parseWorkbook(xmlBytes: ByteArray): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val parser = Xml.newPullParser()
        parser.setInput(xmlBytes.inputStream(), "UTF-8")

        var eventType = parser.eventType
        var sheetIndex = 1

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                val name = parser.getAttributeValue(null, "name") ?: "Sheet$sheetIndex"
                val sheetFile = "sheet$sheetIndex.xml"
                map[name] = sheetFile
                sheetIndex++
            }
            eventType = parser.next()
        }
        return map
    }

    private fun parseWorksheet(xmlBytes: ByteArray, sharedStrings: List<String>): ImportPreviewData {
        val parser = Xml.newPullParser()
        parser.setInput(xmlBytes.inputStream(), "UTF-8")

        val tableRows = mutableMapOf<Int, MutableMap<Int, String>>()
        var currentRowNum = 0
        var currentColIndex = 0
        var currentCellType = ""
        var inCell = false
        var inValue = false
        var cellContent = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tag) {
                        "row" -> {
                            val rAttr = parser.getAttributeValue(null, "r")
                            currentRowNum = rAttr?.toIntOrNull() ?: (currentRowNum + 1)
                            tableRows[currentRowNum] = mutableMapOf()
                        }
                        "c" -> {
                            inCell = true
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            val cellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentColIndex = extractColIndex(cellRef)
                            cellContent = StringBuilder()
                        }
                        "v", "t" -> {
                            if (inCell) {
                                inValue = true
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue) {
                        cellContent.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tag) {
                        "v", "t" -> inValue = false
                        "c" -> {
                            inCell = false
                            val raw = cellContent.toString().trim()
                            val value = when (currentCellType) {
                                "s" -> {
                                    val index = raw.toIntOrNull()
                                    if (index != null && index in sharedStrings.indices) {
                                        sharedStrings[index]
                                    } else raw
                                }
                                "inlineStr", "str" -> raw
                                else -> raw
                            }
                            tableRows[currentRowNum]?.put(currentColIndex, value)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (tableRows.isEmpty()) {
            return ImportPreviewData(0, 0, 0, emptyList(), emptyList(), listOf("الملف فارغ أو لا يحتوي على صفوف بيانات."))
        }

        // Find header row (usually row 1 or the first row that has strings like 'اسم' / 'Name')
        var headerRowNum = 1
        var headerCols = tableRows[1] ?: emptyMap()
        for ((r, cols) in tableRows) {
            val valuesJoined = cols.values.joinToString(" ")
            if (valuesJoined.contains("اسم") || valuesJoined.contains("Name") || valuesJoined.contains("مخدوم")) {
                headerRowNum = r
                headerCols = cols
                break
            }
        }

        // Map column indices to field types
        val colMap = mutableMapOf<String, Int>()
        val detectedColumnsList = mutableListOf<String>()

        for ((colIdx, colName) in headerCols.entries.sortedBy { it.key }) {
            detectedColumnsList.add(colName)
            val clean = normalizeString(colName)
            when {
                clean.contains("كود") || clean == "id" || clean == "code" -> colMap["id"] = colIdx
                clean.contains("اسم المخدوم") || clean.contains("اسم الشماس") || clean.contains("الاسم بالكامل") || clean == "الاسم" || clean == "اسم" || clean == "name" || clean == "fullname" -> colMap["name"] = colIdx
                clean.contains("فصل") || clean.contains("صف") || clean.contains("مرحل") || clean == "class" || clean == "schoolclass" || clean == "grade" -> colMap["schoolClass"] = colIdx
                clean.contains("مجموعة") || clean == "group" -> colMap["group"] = colIdx
                clean.contains("ميلاد") || clean == "dob" || clean == "birthdate" || clean == "birth date" -> colMap["birthDate"] = colIdx
                (clean.contains("هاتف") || clean.contains("موبايل") || clean.contains("تليفون") || clean == "phone") && !clean.contains("ولي") && !clean.contains("والد") -> colMap["phone"] = colIdx
                (clean.contains("هاتف") || clean.contains("موبايل") || clean.contains("تليفون")) && (clean.contains("ولي") || clean.contains("والد") || clean.contains("parent")) -> colMap["parentPhone"] = colIdx
                clean.contains("محافظ") || clean == "governorate" -> colMap["governorate"] = colIdx
                clean.contains("مركز") || clean == "center" -> colMap["center"] = colIdx
                clean.contains("منطق") || clean.contains("قرية") || clean == "area" || clean == "village" -> colMap["area"] = colIdx
                clean.contains("شارع") || clean == "street" -> colMap["street"] = colIdx
                clean.contains("منزل") || clean.contains("بيت") || clean == "housenumber" || clean == "house no" -> colMap["houseNumber"] = colIdx
                clean.contains("تفاصيل العنوان") || clean.contains("العنوان") || clean == "address" -> colMap["addressDetails"] = colIdx
                clean.contains("ملاحظ") || clean == "notes" -> colMap["notes"] = colIdx
            }
        }

        // Fallback default column order if headers weren't named with keywords
        if (!colMap.containsKey("name")) {
            // Assume col 1 or 2 is name
            colMap["name"] = if (headerCols.size > 1 && headerCols[1]?.toIntOrNull() != null) 2 else 1
        }

        val parsedRows = mutableListOf<ParsedMemberRow>()
        val allErrors = mutableListOf<String>()

        for ((rowNum, cols) in tableRows) {
            if (rowNum <= headerRowNum) continue // skip headers
            if (cols.values.all { it.isBlank() }) continue // skip empty rows

            val rowErrors = mutableListOf<String>()

            val name = cols[colMap["name"]]?.trim() ?: ""
            val group = cols[colMap["group"]]?.trim() ?: "عام"
            val schoolClass = cols[colMap["schoolClass"]]?.trim() ?: ""
            val phone = cols[colMap["phone"]]?.trim() ?: ""
            val birthDate = cols[colMap["birthDate"]]?.trim() ?: ""
            val parentPhone = cols[colMap["parentPhone"]]?.trim() ?: ""
            val governorate = cols[colMap["governorate"]]?.trim() ?: "أسيوط"
            val center = cols[colMap["center"]]?.trim() ?: "القوصية"
            val area = cols[colMap["area"]]?.trim() ?: "مير"
            val street = cols[colMap["street"]]?.trim() ?: ""
            val houseNumber = cols[colMap["houseNumber"]]?.trim() ?: ""
            val addressDetails = cols[colMap["addressDetails"]]?.trim() ?: ""
            val notes = cols[colMap["notes"]]?.trim() ?: ""
            val id = cols[colMap["id"]]?.toLongOrNull()

            if (name.isBlank()) {
                val err = "الصف $rowNum: اسم المخدوم فارغ"
                rowErrors.add(err)
                allErrors.add(err)
            } else if (name.length < 3) {
                val err = "الصف $rowNum: اسم المخدوم غير كامل ($name)"
                rowErrors.add(err)
                allErrors.add(err)
            }

            val isValid = rowErrors.isEmpty()

            parsedRows.add(
                ParsedMemberRow(
                    rowNumber = rowNum,
                    id = id,
                    fullName = name,
                    groupName = group,
                    schoolClass = schoolClass,
                    birthDate = birthDate,
                    phone = phone,
                    parentPhone = parentPhone,
                    governorate = governorate,
                    center = center,
                    area = area,
                    street = street,
                    houseNumber = houseNumber,
                    addressDetails = addressDetails,
                    notes = notes,
                    isValid = isValid,
                    errors = rowErrors
                )
            )
        }

        val validCount = parsedRows.count { it.isValid }
        val invalidCount = parsedRows.size - validCount

        return ImportPreviewData(
            totalRows = parsedRows.size,
            validCount = validCount,
            invalidCount = invalidCount,
            detectedColumns = detectedColumnsList,
            previewRows = parsedRows,
            allErrors = allErrors
        )
    }

    private fun extractColIndex(cellRef: String): Int {
        var colLetters = ""
        for (ch in cellRef) {
            if (ch.isLetter()) {
                colLetters += ch.uppercaseChar()
            }
        }
        var col = 0
        for (ch in colLetters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return if (col == 0) 1 else col
    }

    private fun normalizeString(str: String): String {
        return str.lowercase()
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
    }
}
