package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType
import com.example.data.model.BibleAssessment
import com.example.data.model.Exam
import com.example.data.model.ExamResult
import com.example.data.model.Group
import com.example.data.model.Hymn
import com.example.data.model.HymnAssessment
import com.example.data.model.Member
import com.example.data.model.VisitRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    // Supported Evaluation Periods
    const val PERIOD_MONTH = 0        // شهر واحد
    const val PERIOD_4_MONTHS = 1      // 4 أشهر (التيرم)
    const val PERIOD_HALF_YEAR = 2     // نصف سنة (6 أشهر)
    const val PERIOD_YEAR = 3          // سنة كاملة (12 شهر)
    const val PERIOD_CUSTOM = 4        // فترة مخصصة

    data class ExportFilter(
        val memberId: Long? = null,
        val groupId: Long? = null,
        val classFilter: String? = null,
        val classFilters: Set<String> = emptySet(),
        val periodType: Int = PERIOD_MONTH,
        val customStartDate: String = "",
        val customEndDate: String = ""
    )

    data class PeriodDateRange(
        val label: String,
        val startDateStr: String,
        val endDateStr: String,
        val displayDateRange: String,
        val requiredMasses: Int
    )

    data class DynamicColumn(
        val key: String,
        val title: String,
        val width: Int
    )

    data class DynamicRowData(
        val index: Int,
        val memberId: Long,
        val values: Map<String, String>,
        val overallGrade: String
    )

    data class InAppExcelPreview(
        val reportTitle: String,
        val periodLabel: String,
        val periodDates: String,
        val targetScopeDescription: String,
        val totalMembers: Int,
        val columns: List<DynamicColumn>,
        val rows: List<DynamicRowData>,
        val overallServiceAttendanceRate: Int,
        val overallMassAttendanceCount: Int,
        val overallHymnsAverage: Double,
        val overallBibleAverage: Double,
        val overallExamsAverage: Double
    )

    /**
     * Resolves exact start date, end date, label, and required mass count based on filter.
     */
    fun resolvePeriodRange(filter: ExportFilter): PeriodDateRange {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()

        return when (filter.periodType) {
            PERIOD_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = sdf.format(cal.time)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = sdf.format(cal.time)
                PeriodDateRange(
                    label = "تقرير شهري (شهر)",
                    startDateStr = start,
                    endDateStr = end,
                    displayDateRange = "$start إلى $end",
                    requiredMasses = 2
                )
            }
            PERIOD_4_MONTHS -> {
                val end = sdf.format(cal.time)
                cal.add(Calendar.MONTH, -4)
                val start = sdf.format(cal.time)
                PeriodDateRange(
                    label = "تقرير 4 أشهر (التيرم)",
                    startDateStr = start,
                    endDateStr = end,
                    displayDateRange = "$start إلى $end",
                    requiredMasses = 8
                )
            }
            PERIOD_HALF_YEAR -> {
                val end = sdf.format(cal.time)
                cal.add(Calendar.MONTH, -6)
                val start = sdf.format(cal.time)
                PeriodDateRange(
                    label = "تقرير نصف سنة (6 أشهر)",
                    startDateStr = start,
                    endDateStr = end,
                    displayDateRange = "$start إلى $end",
                    requiredMasses = 12
                )
            }
            PERIOD_YEAR -> {
                val end = sdf.format(cal.time)
                cal.add(Calendar.YEAR, -1)
                val start = sdf.format(cal.time)
                PeriodDateRange(
                    label = "تقرير سنوي شامل (12 شهراً)",
                    startDateStr = start,
                    endDateStr = end,
                    displayDateRange = "$start إلى $end",
                    requiredMasses = 24
                )
            }
            PERIOD_CUSTOM -> {
                val start = filter.customStartDate.ifBlank { "2026-01-01" }
                val end = filter.customEndDate.ifBlank { sdf.format(Date()) }
                PeriodDateRange(
                    label = "فترة مخصصة",
                    startDateStr = start,
                    endDateStr = end,
                    displayDateRange = "$start إلى $end",
                    requiredMasses = 2
                )
            }
            else -> {
                val now = sdf.format(Date())
                PeriodDateRange("تقرير شهري", now, now, now, 2)
            }
        }
    }

    /**
     * Checks if a date string is within the [startDate, endDate] window.
     */
    private fun isDateInRange(dateStr: String, startDate: String, endDate: String): Boolean {
        if (dateStr.isBlank()) return true
        val normalized = dateStr.trim().replace("/", "-")
        val normStart = startDate.trim().replace("/", "-")
        val normEnd = endDate.trim().replace("/", "-")
        return normalized >= normStart && normalized <= normEnd
    }

    /**
     * Generates a fully dynamic In-App Preview table according to the chosen [ExcelExportConfig].
     */
    fun generateInAppPreview(
        config: ExcelExportConfig,
        members: List<Member>,
        groups: List<Group>,
        attendances: List<Attendance>,
        hymns: List<Hymn>,
        hymnAssessments: List<HymnAssessment>,
        bibleAssessments: List<BibleAssessment>,
        exams: List<Exam>,
        examResults: List<ExamResult>
    ): InAppExcelPreview {
        val groupMap = groups.associateBy { it.id }
        val filter = config.toExportFilter()
        val periodRange = resolvePeriodRange(filter)

        val activeClasses = if (filter.classFilters.isNotEmpty()) filter.classFilters else if (!filter.classFilter.isNullOrBlank()) setOf(filter.classFilter) else emptySet()

        val targetMembers = members.filter { m ->
            val matchesMember = filter.memberId == null || m.id == filter.memberId
            val matchesGroup = filter.groupId == null || m.groupId == filter.groupId
            val matchesClass = activeClasses.isEmpty() || m.schoolClass in activeClasses
            matchesMember && matchesGroup && matchesClass
        }

        val classDescription = when {
            activeClasses.size == 1 -> "فصل: ${activeClasses.first()}"
            activeClasses.size > 1 -> "فصول مختارة: ${activeClasses.joinToString("، ")}"
            else -> "جميع الفصول الدراسية"
        }

        val targetScopeDescription = when {
            filter.memberId != null -> "مخدوم فردي: ${members.find { it.id == filter.memberId }?.fullName ?: ""}"
            filter.groupId != null && activeClasses.isNotEmpty() -> "مجموعة: ${groupMap[filter.groupId]?.name ?: ""} - $classDescription"
            filter.groupId != null -> "مجموعة: ${groupMap[filter.groupId]?.name ?: ""} ($classDescription)"
            activeClasses.isNotEmpty() -> classDescription
            else -> "جميع المجموعات والمخدومين (${targetMembers.size} مخدوم)"
        }

        // Apply strict date filtering
        val inRangeAttendances = attendances.filter { isDateInRange(it.date, periodRange.startDateStr, periodRange.endDateStr) }
        val inRangeHymns = hymnAssessments.filter { isDateInRange(it.date, periodRange.startDateStr, periodRange.endDateStr) }
        val inRangeBibles = bibleAssessments.filter { isDateInRange(it.date, periodRange.startDateStr, periodRange.endDateStr) }

        // Build Dynamic Columns List
        val cols = mutableListOf<DynamicColumn>()
        cols.add(DynamicColumn("index", "م", 6))
        cols.add(DynamicColumn("memberName", "اسم المخدوم", 26))

        if (config.includeGroupName) cols.add(DynamicColumn("groupName", "المجموعة", 18))
        if (config.includeSchoolClass) cols.add(DynamicColumn("schoolClass", "الفصل الدراسي", 18))
        if (config.includeBirthDate) cols.add(DynamicColumn("birthDate", "تاريخ الميلاد", 14))
        if (config.includePhone) cols.add(DynamicColumn("phone", "رقم الهاتف", 16))
        if (config.includeParentPhone) cols.add(DynamicColumn("parentPhone", "هاتف ولي الأمر", 16))
        if (config.includeAddress) cols.add(DynamicColumn("address", "العنوان", 24))

        if (config.includeServicePresent) cols.add(DynamicColumn("servicePresent", "حضور الخدمة", 14))
        if (config.includeServiceAbsent) cols.add(DynamicColumn("serviceAbsent", "غياب الخدمة", 14))
        if (config.includeServiceExcused) cols.add(DynamicColumn("serviceExcused", "الأعذار", 12))
        if (config.includeServicePercentage) cols.add(DynamicColumn("servicePercentage", "نسبة الخدمة %", 15))

        if (config.includeMassPresent) cols.add(DynamicColumn("massPresent", "حضور القداس", 15))
        if (config.includeMassPercentage) cols.add(DynamicColumn("massPercentage", "نسبة القداس %", 15))

        if (config.includeHymnScores) cols.add(DynamicColumn("hymnScores", "تسميع الألحان", 16))
        if (config.includeHymnAverage) cols.add(DynamicColumn("hymnAverage", "متوسط الألحان", 15))

        if (config.includeBibleScores) cols.add(DynamicColumn("bibleScores", "تقييم الإنجيل", 15))
        if (config.includeBibleAverage) cols.add(DynamicColumn("bibleAverage", "متوسط الإنجيل", 15))

        if (config.includeExamScores) cols.add(DynamicColumn("examScores", "الامتحانات", 15))
        if (config.includeExamAverage) cols.add(DynamicColumn("examAverage", "متوسط الامتحانات", 16))

        if (config.includeTotalScore) cols.add(DynamicColumn("totalScore", "المجموع", 14))
        if (config.includeTotalPercentage) cols.add(DynamicColumn("totalPercentage", "النسبة %", 14))
        if (config.includeOverallGrade) cols.add(DynamicColumn("overallGrade", "التقدير العام", 18))
        if (config.includeNotes) cols.add(DynamicColumn("notes", "الملاحظات", 24))

        // Build Dynamic Rows
        val dynamicRows = targetMembers.mapIndexed { idx, member ->
            val memberServiceAttendances = inRangeAttendances.filter { it.memberId == member.id && it.type == AttendanceType.SERVICE }
            val memberMassAttendances = inRangeAttendances.filter { it.memberId == member.id && it.type == AttendanceType.MASS }

            val servicePresent = memberServiceAttendances.count { it.status == AttendanceStatus.PRESENT }
            val serviceAbsent = memberServiceAttendances.count { it.status == AttendanceStatus.ABSENT }
            val serviceExcuses = memberServiceAttendances.count { it.status == AttendanceStatus.EXCUSED }
            val serviceTotal = memberServiceAttendances.size.coerceAtLeast(1)
            val serviceRate = (servicePresent.toDouble() / serviceTotal) * 100.0

            val massPresent = memberMassAttendances.count { it.status == AttendanceStatus.PRESENT }
            val massPercentage = if (periodRange.requiredMasses > 0) {
                ((massPresent.toDouble() / periodRange.requiredMasses) * 100.0).coerceAtMost(100.0)
            } else 0.0

            val memberHymns = inRangeHymns.filter { it.memberId == member.id }
            val hymnAvg = if (memberHymns.isNotEmpty()) {
                memberHymns.map { if (it.maxScore > 0) (it.score / it.maxScore) * 100.0 else it.score }.average()
            } else 0.0

            val memberBibles = inRangeBibles.filter { it.memberId == member.id }
            val bibleAvg = if (memberBibles.isNotEmpty()) {
                memberBibles.map { if (it.maxScore > 0) (it.score / it.maxScore) * 100.0 else it.score * 10.0 }.average()
            } else 0.0

            val memberExams = examResults.filter { it.memberId == member.id }
            val examAvg = if (memberExams.isNotEmpty()) {
                memberExams.map { it.score }.average()
            } else 0.0

            // Overall Grade Calculation
            val totalPercentage = (serviceRate * 0.25) +
                    (massPercentage * 0.25) +
                    (hymnAvg * 0.20) +
                    (bibleAvg * 0.15) +
                    (examAvg * 0.15)

            val overallGrade = when {
                totalPercentage >= 90.0 -> "امتياز"
                totalPercentage >= 80.0 -> "جيد جداً"
                totalPercentage >= 70.0 -> "جيد"
                totalPercentage >= 50.0 -> "مقبول"
                else -> "يحتاج متابعة"
            }

            val values = mutableMapOf<String, String>()
            values["index"] = (idx + 1).toString()
            values["memberName"] = member.fullName
            values["groupName"] = member.groupId?.let { groupMap[it]?.name } ?: "عام"
            values["schoolClass"] = member.schoolClass.ifBlank { "-" }
            values["birthDate"] = member.birthDate.ifBlank { "-" }
            values["phone"] = member.phone.ifBlank { "-" }
            values["parentPhone"] = member.parentPhone.ifBlank { "-" }
            values["address"] = "${member.area} ${member.street}".trim().ifBlank { "-" }

            values["servicePresent"] = servicePresent.toString()
            values["serviceAbsent"] = serviceAbsent.toString()
            values["serviceExcused"] = serviceExcuses.toString()
            values["servicePercentage"] = String.format(Locale.US, "%.0f%%", serviceRate)

            values["massPresent"] = "$massPresent / ${periodRange.requiredMasses}"
            values["massPercentage"] = String.format(Locale.US, "%.0f%%", massPercentage)

            values["hymnScores"] = if (memberHymns.isNotEmpty()) "${memberHymns.size} ألحان" else "-"
            values["hymnAverage"] = if (hymnAvg > 0) String.format(Locale.US, "%.1f", hymnAvg) else "-"

            values["bibleScores"] = if (memberBibles.isNotEmpty()) "${memberBibles.size} تقييم" else "-"
            values["bibleAverage"] = if (bibleAvg > 0) String.format(Locale.US, "%.1f", bibleAvg) else "-"

            values["examScores"] = if (memberExams.isNotEmpty()) "${memberExams.size} امتحان" else "-"
            values["examAverage"] = if (examAvg > 0) String.format(Locale.US, "%.1f", examAvg) else "-"

            values["totalScore"] = String.format(Locale.US, "%.1f", totalPercentage)
            values["totalPercentage"] = String.format(Locale.US, "%.0f%%", totalPercentage)
            values["overallGrade"] = overallGrade
            values["notes"] = member.notes.ifBlank { "-" }

            DynamicRowData(
                index = idx + 1,
                memberId = member.id,
                values = values,
                overallGrade = overallGrade
            )
        }

        val allServiceLogs = inRangeAttendances.filter { it.type == AttendanceType.SERVICE }
        val overallServiceRate = if (allServiceLogs.isNotEmpty()) {
            (allServiceLogs.count { it.status == AttendanceStatus.PRESENT } * 100) / allServiceLogs.size
        } else 0

        val overallMassCount = inRangeAttendances.count { it.type == AttendanceType.MASS && it.status == AttendanceStatus.PRESENT }

        val overallHymnAvg = if (inRangeHymns.isNotEmpty()) inRangeHymns.map { it.score }.average() else 0.0
        val overallBibleAvg = if (inRangeBibles.isNotEmpty()) inRangeBibles.map { it.score }.average() else 0.0
        val overallExamAvg = if (examResults.isNotEmpty()) examResults.map { it.score }.average() else 0.0

        return InAppExcelPreview(
            reportTitle = "مدرسة القديس اسطفانوس للشمامسة — تقرير المتابعة والتقييم الشامل",
            periodLabel = periodRange.label,
            periodDates = periodRange.displayDateRange,
            targetScopeDescription = targetScopeDescription,
            totalMembers = targetMembers.size,
            columns = cols,
            rows = dynamicRows,
            overallServiceAttendanceRate = overallServiceRate,
            overallMassAttendanceCount = overallMassCount,
            overallHymnsAverage = overallHymnAvg,
            overallBibleAverage = overallBibleAvg,
            overallExamsAverage = overallExamAvg
        )
    }

    /**
     * Exports customizable Excel (.xlsx) file based on [ExcelExportConfig] on Dispatchers.IO.
     */
    suspend fun exportToExcel(
        context: Context,
        config: ExcelExportConfig,
        members: List<Member>,
        groups: List<Group>,
        attendances: List<Attendance>,
        hymns: List<Hymn>,
        hymnAssessments: List<HymnAssessment>,
        bibleAssessments: List<BibleAssessment>,
        exams: List<Exam>,
        examResults: List<ExamResult>,
        visitRecords: List<VisitRecord> = emptyList()
    ): File = withContext(Dispatchers.IO) {
        val groupMap = groups.associateBy { it.id }
        val hymnMap = hymns.associateBy { it.id }
        val examMap = exams.associateBy { it.id }
        val memberMap = members.associateBy { it.id }

        val filter = config.toExportFilter()
        val periodRange = resolvePeriodRange(filter)

        val activeClasses = if (filter.classFilters.isNotEmpty()) filter.classFilters else if (!filter.classFilter.isNullOrBlank()) setOf(filter.classFilter) else emptySet()

        val targetMembers = members.filter { m ->
            val matchesMember = filter.memberId == null || m.id == filter.memberId
            val matchesGroup = filter.groupId == null || m.groupId == filter.groupId
            val matchesClass = activeClasses.isEmpty() || m.schoolClass in activeClasses
            matchesMember && matchesGroup && matchesClass
        }
        val targetMemberIds = targetMembers.map { it.id }.toSet()

        val filteredAttendance = attendances.filter {
            it.memberId in targetMemberIds && isDateInRange(it.date, periodRange.startDateStr, periodRange.endDateStr)
        }
        val filteredHymnAssessments = hymnAssessments.filter {
            it.memberId in targetMemberIds && isDateInRange(it.date, periodRange.startDateStr, periodRange.endDateStr)
        }
        val filteredBibleAssessments = bibleAssessments.filter {
            it.memberId in targetMemberIds && isDateInRange(it.date, periodRange.startDateStr, periodRange.endDateStr)
        }
        val filteredExamResults = examResults.filter { it.memberId in targetMemberIds }

        val exportDir = File(context.cacheDir, "Reports").apply { mkdirs() }
        val dateWindow = "${periodRange.startDateStr}_to_${periodRange.endDateStr}"
        val classTag = when {
            activeClasses.size in 1..2 -> activeClasses.joinToString("_") { it.replace(" ", "") }
            activeClasses.size > 2 -> "MultiClass_${activeClasses.size}"
            else -> "AllClasses"
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "Deacons_Report_${classTag}_${dateWindow}_$timestamp.xlsx".replace(Regex("[^a-zA-Z0-9._\\u0600-\\u06FF-]"), "_")
        val outputFile = File(exportDir, fileName)

        val previewData = generateInAppPreview(
            config = config,
            members = members,
            groups = groups,
            attendances = attendances,
            hymns = hymns,
            hymnAssessments = hymnAssessments,
            bibleAssessments = bibleAssessments,
            exams = exams,
            examResults = examResults
        )

        // Determine which sheets to generate
        data class SheetSpec(val id: Int, val name: String, val fileName: String, val contentXml: String)

        val activeSheets = mutableListOf<SheetSpec>()
        var sheetCounter = 1

        if (!config.isMultiSheetMode) {
            // Single Sheet Mode: Master Report
            val summaryXml = buildDynamicSummarySheetXml(previewData)
            activeSheets.add(SheetSpec(sheetCounter++, "التقرير الشامل", "sheet1.xml", summaryXml))
        } else {
            // Multi-sheet mode: selectively include enabled sheets
            if (config.includeSummarySheet) {
                val summaryXml = buildDynamicSummarySheetXml(previewData)
                activeSheets.add(SheetSpec(sheetCounter++, "الملخص العام", "sheet${activeSheets.size + 1}.xml", summaryXml))
            }
            if (config.includeAttendanceSheet) {
                val serviceXml = buildAttendanceSheetXml(
                    filteredAttendance.filter { it.type == AttendanceType.SERVICE },
                    memberMap,
                    groupMap
                )
                activeSheets.add(SheetSpec(sheetCounter++, "حضور الخدمة", "sheet${activeSheets.size + 1}.xml", serviceXml))
            }
            if (config.includeMassSheet) {
                val massXml = buildMassAttendanceSheetXml(
                    filteredAttendance.filter { it.type == AttendanceType.MASS },
                    memberMap,
                    groupMap,
                    periodRange.requiredMasses
                )
                activeSheets.add(SheetSpec(sheetCounter++, "حضور القداسات", "sheet${activeSheets.size + 1}.xml", massXml))
            }
            if (config.includeHymnsSheet) {
                val hymnsXml = buildHymnsSheetXml(filteredHymnAssessments, hymnMap, memberMap, groupMap)
                activeSheets.add(SheetSpec(sheetCounter++, "الألحان", "sheet${activeSheets.size + 1}.xml", hymnsXml))
            }
            if (config.includeBibleSheet) {
                val bibleXml = buildBibleSheetXml(filteredBibleAssessments, memberMap, groupMap)
                activeSheets.add(SheetSpec(sheetCounter++, "الإنجيل", "sheet${activeSheets.size + 1}.xml", bibleXml))
            }
            if (config.includeExamsSheet) {
                val examsXml = buildExamsSheetXml(filteredExamResults, examMap, memberMap, groupMap)
                activeSheets.add(SheetSpec(sheetCounter++, "الامتحانات", "sheet${activeSheets.size + 1}.xml", examsXml))
            }
            if (config.includeMembersSheet) {
                val membersXml = buildMembersSheetXml(targetMembers, groupMap)
                activeSheets.add(SheetSpec(sheetCounter++, "سجل المخدومين", "sheet${activeSheets.size + 1}.xml", membersXml))
            }
            if (config.includeVisitsSheet) {
                val visitsXml = buildVisitsSheetXml(targetMembers, visitRecords)
                activeSheets.add(SheetSpec(sheetCounter++, "الافتقاد", "sheet${activeSheets.size + 1}.xml", visitsXml))
            }

            // Fallback if none selected
            if (activeSheets.isEmpty()) {
                val summaryXml = buildDynamicSummarySheetXml(previewData)
                activeSheets.add(SheetSpec(1, "التقرير الشامل", "sheet1.xml", summaryXml))
            }
        }

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zip ->
                writeZipEntry(zip, "[Content_Types].xml", buildDynamicContentTypesXml(activeSheets.size))
                writeZipEntry(zip, "_rels/.rels", buildRootRelsXml())
                writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildDynamicWorkbookRelsXml(activeSheets.size))
                writeZipEntry(zip, "xl/workbook.xml", buildDynamicWorkbookXml(activeSheets.map { it.name }))
                writeZipEntry(zip, "xl/styles.xml", buildStylesXml())

                activeSheets.forEachIndexed { index, sheetSpec ->
                    writeZipEntry(zip, "xl/worksheets/sheet${index + 1}.xml", sheetSpec.contentXml)
                }
            }
        }

        outputFile
    }

    /**
     * Generates a clean template (.xlsx) for importing members into the system.
     */
    suspend fun generateExcelTemplate(context: Context): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "Templates").apply { mkdirs() }
        val templateFile = File(exportDir, "نموذج_استيراد_مخدومين_مدرسة_الشمامسة.xlsx")

        FileOutputStream(templateFile).use { fos ->
            ZipOutputStream(fos).use { zip ->
                writeZipEntry(zip, "[Content_Types].xml", buildTemplateContentTypesXml())
                writeZipEntry(zip, "_rels/.rels", buildRootRelsXml())
                writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildTemplateWorkbookRelsXml())
                writeZipEntry(zip, "xl/workbook.xml", buildTemplateWorkbookXml())
                writeZipEntry(zip, "xl/styles.xml", buildStylesXml())
                writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildTemplateSheetXml())
            }
        }

        templateFile
    }

    /**
     * Dedicated exporter for Visits (كشف الافتقاد)
     */
    suspend fun exportVisitsToExcel(
        context: Context,
        members: List<Member>,
        visits: List<VisitRecord>,
        classFilter: String? = null
    ): File = withContext(Dispatchers.IO) {
        val targetMembers = if (!classFilter.isNullOrBlank()) {
            members.filter { it.schoolClass == classFilter }
        } else {
            members
        }

        val exportDir = File(context.cacheDir, "Reports").apply { mkdirs() }
        val classTag = if (!classFilter.isNullOrBlank()) classFilter.replace(" ", "_") else "All"
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "Visits_Report_${classTag}_$timestamp.xlsx".replace(Regex("[^a-zA-Z0-9._\\u0600-\\u06FF-]"), "_")
        val outputFile = File(exportDir, fileName)

        val visitsXml = buildVisitsSheetXml(targetMembers, visits)

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zip ->
                writeZipEntry(zip, "[Content_Types].xml", buildDynamicContentTypesXml(1))
                writeZipEntry(zip, "_rels/.rels", buildRootRelsXml())
                writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildDynamicWorkbookRelsXml(1))
                writeZipEntry(zip, "xl/workbook.xml", buildDynamicWorkbookXml(listOf("الافتقاد")))
                writeZipEntry(zip, "xl/styles.xml", buildStylesXml())
                writeZipEntry(zip, "xl/worksheets/sheet1.xml", visitsXml)
            }
        }

        outputFile
    }

    fun shareExcelFile(context: Context, file: File, subject: String = "تقرير خدمة الشمامسة") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "مرفق ملف Excel (.xlsx) الرسمي المعتمد لمدرسة الشمامسة.\n© 2026 Kirolos Sabry Fouad. All Rights Reserved.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "مشاركة / فتح ملف Excel")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun openExcelFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareExcelFile(context, file)
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, entryName: String, content: String) {
        zip.putNextEntry(ZipEntry(entryName))
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zip.write(bytes, 0, bytes.size)
        zip.closeEntry()
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun columnLetter(colIndexZero: Int): String {
        var num = colIndexZero
        var result = ""
        while (num >= 0) {
            result = ('A' + (num % 26)).toString() + result
            num = (num / 26) - 1
        }
        return result
    }

    // -------------------------------------------------------------
    // Dynamic XML Builders
    // -------------------------------------------------------------

    private fun buildDynamicContentTypesXml(sheetCount: Int): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
""")
        for (i in 1..sheetCount) {
            sb.append("""    <Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
""")
        }
        sb.append("</Types>")
        return sb.toString()
    }

    private fun buildRootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun buildDynamicWorkbookRelsXml(sheetCount: Int): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
""")
        for (i in 1..sheetCount) {
            sb.append("""    <Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>
""")
        }
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun buildDynamicWorkbookXml(sheetNames: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
""")
        sheetNames.forEachIndexed { idx, name ->
            val id = idx + 1
            sb.append("""        <sheet name="${escapeXml(name)}" sheetId="$id" r:id="rId$id"/>
""")
        }
        sb.append("""    </sheets>
</workbook>""")
        return sb.toString()
    }

    private fun buildStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="6">
        <font><sz val="11"/><color theme="1"/><name val="Segoe UI"/></font>
        <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Segoe UI"/></font>
        <font><b/><sz val="15"/><color rgb="FFB8860B"/><name val="Segoe UI"/></font>
        <font><b/><sz val="11"/><color rgb="FF555555"/><name val="Segoe UI"/></font>
        <font><b/><sz val="11"/><color rgb="FFB8860B"/><name val="Segoe UI"/></font>
        <font><b/><sz val="12"/><color rgb="FFB8860B"/><name val="Segoe UI"/></font>
    </fonts>
    <fills count="6">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFB8860B"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFD4AF37"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFF4F4F6"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFFFF8E1"/></patternFill></fill>
    </fills>
    <borders count="2">
        <border><left/><right/><top/><bottom/><diagonal/></border>
        <border>
            <left style="thin"><color rgb="FFCCCCCC"/></left>
            <right style="thin"><color rgb="FFCCCCCC"/></right>
            <top style="thin"><color rgb="FFCCCCCC"/></top>
            <bottom style="thin"><color rgb="FFCCCCCC"/></bottom>
        </border>
    </borders>
    <cellStyleXfs count="1">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
    </cellStyleXfs>
    <cellXfs count="8">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center" wrapText="1"/>
        </xf>
        <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center" wrapText="1"/>
        </xf>
        <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center"/>
        </xf>
        <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center"/>
        </xf>
        <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center" wrapText="1"/>
        </xf>
        <xf numFmtId="0" fontId="0" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center" wrapText="1"/>
        </xf>
        <xf numFmtId="0" fontId="4" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center" wrapText="1"/>
        </xf>
        <xf numFmtId="0" fontId="5" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center" wrapText="1"/>
        </xf>
    </cellXfs>
</styleSheet>"""

    /**
     * Builds the Dynamic Summary / Master Sheet XML containing only the enabled columns.
     */
    private fun buildDynamicSummarySheetXml(previewData: InAppExcelPreview): String {
        val sb = StringBuilder()
        val numCols = previewData.columns.size.coerceAtLeast(1)

        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetViews>
        <sheetView tabSelected="1" workbookViewId="0" rightToLeft="1">
            <pane ySplit="5" topLeftCell="A6" activePane="bottomLeft" state="frozen"/>
        </sheetView>
    </sheetViews>
    <cols>
""")
        previewData.columns.forEachIndexed { i, col ->
            val colNum = i + 1
            sb.append("""        <col min="$colNum" max="$colNum" width="${col.width}" customWidth="1"/>
""")
        }
        sb.append("""    </cols>
    <sheetData>
""")

        var rowIdx = 1

        // Row 1: Main Header Title
        sb.append("""        <row r="$rowIdx" ht="32" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="1"><is><t>خدمة الشمامسة — تقرير المتابعة والتقييم الشامل</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="1"><is><t>إيبارشية القوصية ومير</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="1"><is><t>كنيسة أبي سيفين والعزب ودير الملاك ميخائيل</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="1"><is><t>التقرير المعتمد</t></is></c>
        </row>
""")
        rowIdx++

        // Row 2: Metadata Info
        sb.append("""        <row r="$rowIdx" ht="24" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="4"><is><t>الفترة التقييمية:</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="4"><is><t>${escapeXml(previewData.periodLabel)} (${escapeXml(previewData.periodDates)})</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="4"><is><t>نطاق التقرير:</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="4"><is><t>${escapeXml(previewData.targetScopeDescription)}</t></is></c>
        </row>
""")
        rowIdx++

        // Row 3: Overall Statistics Row
        val exportDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")).format(Date())
        sb.append("""        <row r="$rowIdx" ht="24" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="7"><is><t>إجمالي المخدومين: ${previewData.totalMembers}</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="7"><is><t>نسبة حضور الخدمة: ${previewData.overallServiceAttendanceRate}%</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="7"><is><t>حضور القداسات: ${previewData.overallMassAttendanceCount}</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="7"><is><t>تاريخ الاستخراج: ${escapeXml(exportDate)}</t></is></c>
        </row>
""")
        rowIdx++

        // Row 4: Spacer
        sb.append("""        <row r="$rowIdx" ht="12" customHeight="1"/>
""")
        rowIdx++

        // Row 5: Dynamic Table Header
        sb.append("""        <row r="$rowIdx" ht="30" customHeight="1">
""")
        previewData.columns.forEachIndexed { cIdx, col ->
            val colLetter = columnLetter(cIdx)
            sb.append("""            <c r="$colLetter$rowIdx" t="inlineStr" s="1"><is><t>${escapeXml(col.title)}</t></is></c>
""")
        }
        sb.append("""        </row>
""")
        rowIdx++

        // Data Rows
        previewData.rows.forEachIndexed { rIdx, row ->
            val style = if (rIdx % 2 == 0) "4" else "5"
            sb.append("""        <row r="$rowIdx" ht="22" customHeight="1">
""")
            previewData.columns.forEachIndexed { cIdx, col ->
                val colLetter = columnLetter(cIdx)
                val cellVal = row.values[col.key] ?: "-"
                sb.append("""            <c r="$colLetter$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(cellVal)}</t></is></c>
""")
            }
            sb.append("""        </row>
""")
            rowIdx++
        }

        // Footer Row
        sb.append("""        <row r="$rowIdx" ht="28" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="7"><is><t>© 2026 Kirolos Sabry Fouad. All Rights Reserved.</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="7"><is><t>Developed by Kirolos Sabry Fouad</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="7"><is><t>أمين الخدمة: كيرلس صبري فؤاد</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="7"><is><t>مدرسة القديس اسطفانوس بمير</t></is></c>
        </row>
""")

        sb.append("""    </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildAttendanceSheetXml(
        attendances: List<Attendance>,
        memberMap: Map<Long, Member>,
        groupMap: Map<Long, Group>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetViews>
        <sheetView tabSelected="0" workbookViewId="0" rightToLeft="1">
            <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
        </sheetView>
    </sheetViews>
    <cols>
        <col min="1" max="1" width="8" customWidth="1"/>
        <col min="2" max="2" width="16" customWidth="1"/>
        <col min="3" max="3" width="28" customWidth="1"/>
        <col min="4" max="4" width="22" customWidth="1"/>
        <col min="5" max="5" width="20" customWidth="1"/>
        <col min="6" max="6" width="16" customWidth="1"/>
        <col min="7" max="7" width="24" customWidth="1"/>
        <col min="8" max="8" width="24" customWidth="1"/>
    </cols>
    <sheetData>
        <row r="1" ht="28" customHeight="1">
            <c r="A1" t="inlineStr" s="1"><is><t>م</t></is></c>
            <c r="B1" t="inlineStr" s="1"><is><t>التاريخ</t></is></c>
            <c r="C1" t="inlineStr" s="1"><is><t>اسم المخدوم</t></is></c>
            <c r="D1" t="inlineStr" s="1"><is><t>المجموعة</t></is></c>
            <c r="E1" t="inlineStr" s="1"><is><t>الفصل الدراسي</t></is></c>
            <c r="F1" t="inlineStr" s="1"><is><t>الحالة</t></is></c>
            <c r="G1" t="inlineStr" s="1"><is><t>سبب العذر</t></is></c>
            <c r="H1" t="inlineStr" s="1"><is><t>الملاحظات</t></is></c>
        </row>
""")
        attendances.forEachIndexed { idx, item ->
            val rowIdx = idx + 2
            val member = memberMap[item.memberId]
            val memberName = member?.fullName ?: "مخدوم رقم ${item.memberId}"
            val groupName = member?.groupId?.let { groupMap[it]?.name } ?: "عام"
            val schoolClass = member?.schoolClass ?: "-"
            val statusText = when (item.status) {
                AttendanceStatus.PRESENT -> "حاضر"
                AttendanceStatus.ABSENT -> "غائب"
                AttendanceStatus.EXCUSED -> "معذور"
            }
            val excuseReason = if (item.status == AttendanceStatus.EXCUSED) item.notes else "-"
            val notesText = if (item.status != AttendanceStatus.EXCUSED) item.notes else ""
            val style = if (idx % 2 == 0) "4" else "5"

            sb.append("""        <row r="$rowIdx" ht="22" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="$style"><is><t>${idx + 1}</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(item.date)}</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(memberName)}</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(groupName)}</t></is></c>
            <c r="E$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(schoolClass)}</t></is></c>
            <c r="F$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(statusText)}</t></is></c>
            <c r="G$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(excuseReason)}</t></is></c>
            <c r="H$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(notesText)}</t></is></c>
        </row>
""")
        }
        sb.append("""    </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildMassAttendanceSheetXml(
        attendances: List<Attendance>,
        memberMap: Map<Long, Member>,
        groupMap: Map<Long, Group>,
        requiredMasses: Int
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetViews>
        <sheetView tabSelected="0" workbookViewId="0" rightToLeft="1">
            <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
        </sheetView>
    </sheetViews>
    <cols>
        <col min="1" max="1" width="8" customWidth="1"/>
        <col min="2" max="2" width="16" customWidth="1"/>
        <col min="3" max="3" width="28" customWidth="1"/>
        <col min="4" max="4" width="20" customWidth="1"/>
        <col min="5" max="5" width="18" customWidth="1"/>
        <col min="6" max="6" width="16" customWidth="1"/>
        <col min="7" max="7" width="22" customWidth="1"/>
        <col min="8" max="8" width="28" customWidth="1"/>
    </cols>
    <sheetData>
        <row r="1" ht="28" customHeight="1">
            <c r="A1" t="inlineStr" s="1"><is><t>م</t></is></c>
            <c r="B1" t="inlineStr" s="1"><is><t>التاريخ</t></is></c>
            <c r="C1" t="inlineStr" s="1"><is><t>اسم المخدوم</t></is></c>
            <c r="D1" t="inlineStr" s="1"><is><t>المجموعة</t></is></c>
            <c r="E1" t="inlineStr" s="1"><is><t>رقم القداس</t></is></c>
            <c r="F1" t="inlineStr" s="1"><is><t>الحالة</t></is></c>
            <c r="G1" t="inlineStr" s="1"><is><t>القداسات المطلوبة</t></is></c>
            <c r="H1" t="inlineStr" s="1"><is><t>الملاحظات والتناول</t></is></c>
        </row>
""")
        attendances.forEachIndexed { idx, item ->
            val rowIdx = idx + 2
            val member = memberMap[item.memberId]
            val memberName = member?.fullName ?: "مخدوم رقم ${item.memberId}"
            val groupName = member?.groupId?.let { groupMap[it]?.name } ?: "عام"
            val massNumberText = if (item.massNumber == 2) "القداس الثاني" else "القداس الأول"
            val statusText = when (item.status) {
                AttendanceStatus.PRESENT -> "حاضر"
                AttendanceStatus.ABSENT -> "غائب"
                AttendanceStatus.EXCUSED -> "عذر / إذن"
            }
            val style = if (idx % 2 == 0) "4" else "5"

            sb.append("""        <row r="$rowIdx" ht="22" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="$style"><is><t>${idx + 1}</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(item.date)}</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(memberName)}</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(groupName)}</t></is></c>
            <c r="E$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(massNumberText)}</t></is></c>
            <c r="F$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(statusText)}</t></is></c>
            <c r="G$rowIdx" t="inlineStr" s="$style"><is><t>$requiredMasses قداسات</t></is></c>
            <c r="H$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(item.notes)}</t></is></c>
        </row>
""")
        }
        sb.append("""    </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildHymnsSheetXml(
        assessments: List<HymnAssessment>,
        hymnMap: Map<Long, Hymn>,
        memberMap: Map<Long, Member>,
        groupMap: Map<Long, Group>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetViews>
        <sheetView tabSelected="0" workbookViewId="0" rightToLeft="1">
            <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
        </sheetView>
    </sheetViews>
    <cols>
        <col min="1" max="1" width="8" customWidth="1"/>
        <col min="2" max="2" width="28" customWidth="1"/>
        <col min="3" max="3" width="24" customWidth="1"/>
        <col min="4" max="4" width="16" customWidth="1"/>
        <col min="5" max="5" width="14" customWidth="1"/>
        <col min="6" max="6" width="14" customWidth="1"/>
        <col min="7" max="7" width="14" customWidth="1"/>
        <col min="8" max="8" width="30" customWidth="1"/>
    </cols>
    <sheetData>
        <row r="1" ht="28" customHeight="1">
            <c r="A1" t="inlineStr" s="1"><is><t>م</t></is></c>
            <c r="B1" t="inlineStr" s="1"><is><t>اسم المخدوم</t></is></c>
            <c r="C1" t="inlineStr" s="1"><is><t>اسم اللحن</t></is></c>
            <c r="D1" t="inlineStr" s="1"><is><t>التاريخ</t></is></c>
            <c r="E1" t="inlineStr" s="1"><is><t>الدرجة المستحقة</t></is></c>
            <c r="F1" t="inlineStr" s="1"><is><t>الدرجة النهائية</t></is></c>
            <c r="G1" t="inlineStr" s="1"><is><t>النسبة %</t></is></c>
            <c r="H1" t="inlineStr" s="1"><is><t>ملاحظات الأداء والهزات</t></is></c>
        </row>
""")
        assessments.forEachIndexed { idx, item ->
            val rowIdx = idx + 2
            val member = memberMap[item.memberId]
            val memberName = member?.fullName ?: "مخدوم رقم ${item.memberId}"
            val hymn = hymnMap[item.hymnId]
            val hymnName = hymn?.name ?: "لحن رقم ${item.hymnId}"
            val pct = if (item.maxScore > 0) ((item.score / item.maxScore) * 100).toInt() else 0
            val style = if (idx % 2 == 0) "4" else "5"

            sb.append("""        <row r="$rowIdx" ht="22" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="$style"><is><t>${idx + 1}</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(memberName)}</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(hymnName)}</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(item.date)}</t></is></c>
            <c r="E$rowIdx" t="inlineStr" s="$style"><is><t>${item.score}</t></is></c>
            <c r="F$rowIdx" t="inlineStr" s="$style"><is><t>${item.maxScore}</t></is></c>
            <c r="G$rowIdx" t="inlineStr" s="$style"><is><t>$pct%</t></is></c>
            <c r="H$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(item.notes)}</t></is></c>
        </row>
""")
        }
        sb.append("""    </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildBibleSheetXml(
        assessments: List<BibleAssessment>,
        memberMap: Map<Long, Member>,
        groupMap: Map<Long, Group>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetViews>
        <sheetView tabSelected="0" workbookViewId="0" rightToLeft="1">
            <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
        </sheetView>
    </sheetViews>
    <cols>
        <col min="1" max="1" width="8" customWidth="1"/>
        <col min="2" max="2" width="28" customWidth="1"/>
        <col min="3" max="3" width="26" customWidth="1"/>
        <col min="4" max="4" width="16" customWidth="1"/>
        <col min="5" max="5" width="14" customWidth="1"/>
        <col min="6" max="6" width="14" customWidth="1"/>
        <col min="7" max="7" width="14" customWidth="1"/>
        <col min="8" max="8" width="30" customWidth="1"/>
    </cols>
    <sheetData>
        <row r="1" ht="28" customHeight="1">
            <c r="A1" t="inlineStr" s="1"><is><t>م</t></is></c>
            <c r="B1" t="inlineStr" s="1"><is><t>اسم المخدوم</t></is></c>
            <c r="C1" t="inlineStr" s="1"><is><t>الدرس / الأصحاح</t></is></c>
            <c r="D1" t="inlineStr" s="1"><is><t>التاريخ</t></is></c>
            <c r="E1" t="inlineStr" s="1"><is><t>الدرجة المستحقة</t></is></c>
            <c r="F1" t="inlineStr" s="1"><is><t>الدرجة النهائية</t></is></c>
            <c r="G1" t="inlineStr" s="1"><is><t>النسبة %</t></is></c>
            <c r="H1" t="inlineStr" s="1"><is><t>ملاحظات الحفظ والتفسير</t></is></c>
        </row>
""")
        assessments.forEachIndexed { idx, item ->
            val rowIdx = idx + 2
            val member = memberMap[item.memberId]
            val memberName = member?.fullName ?: "مخدوم رقم ${item.memberId}"
            val pct = if (item.maxScore > 0) ((item.score / item.maxScore) * 100).toInt() else 0
            val style = if (idx % 2 == 0) "4" else "5"

            sb.append("""        <row r="$rowIdx" ht="22" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="$style"><is><t>${idx + 1}</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(memberName)}</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(item.lessonName)}</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(item.date)}</t></is></c>
            <c r="E$rowIdx" t="inlineStr" s="$style"><is><t>${item.score}</t></is></c>
            <c r="F$rowIdx" t="inlineStr" s="$style"><is><t>${item.maxScore}</t></is></c>
            <c r="G$rowIdx" t="inlineStr" s="$style"><is><t>$pct%</t></is></c>
            <c r="H$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(item.notes)}</t></is></c>
        </row>
""")
        }
        sb.append("""    </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildExamsSheetXml(
        examResults: List<ExamResult>,
        examMap: Map<Long, Exam>,
        memberMap: Map<Long, Member>,
        groupMap: Map<Long, Group>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetViews>
        <sheetView tabSelected="0" workbookViewId="0" rightToLeft="1">
            <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
        </sheetView>
    </sheetViews>
    <cols>
        <col min="1" max="1" width="8" customWidth="1"/>
        <col min="2" max="2" width="28" customWidth="1"/>
        <col min="3" max="3" width="26" customWidth="1"/>
        <col min="4" max="4" width="16" customWidth="1"/>
        <col min="5" max="5" width="14" customWidth="1"/>
        <col min="6" max="6" width="14" customWidth="1"/>
        <col min="7" max="7" width="14" customWidth="1"/>
        <col min="8" max="8" width="30" customWidth="1"/>
    </cols>
    <sheetData>
        <row r="1" ht="28" customHeight="1">
            <c r="A1" t="inlineStr" s="1"><is><t>م</t></is></c>
            <c r="B1" t="inlineStr" s="1"><is><t>اسم المخدوم</t></is></c>
            <c r="C1" t="inlineStr" s="1"><is><t>اسم الامتحان</t></is></c>
            <c r="D1" t="inlineStr" s="1"><is><t>تاريخ الامتحان</t></is></c>
            <c r="E1" t="inlineStr" s="1"><is><t>الدرجة المستحقة</t></is></c>
            <c r="F1" t="inlineStr" s="1"><is><t>الدرجة النهائية</t></is></c>
            <c r="G1" t="inlineStr" s="1"><is><t>النسبة %</t></is></c>
            <c r="H1" t="inlineStr" s="1"><is><t>ملاحظات النتيجة والتقدير</t></is></c>
        </row>
""")
        examResults.forEachIndexed { idx, item ->
            val rowIdx = idx + 2
            val member = memberMap[item.memberId]
            val memberName = member?.fullName ?: "مخدوم رقم ${item.memberId}"
            val exam = examMap[item.examId]
            val examName = exam?.name ?: "امتحان رقم ${item.examId}"
            val maxScore = exam?.maxScore ?: 100.0
            val pct = if (maxScore > 0) ((item.score / maxScore) * 100).toInt() else 0
            val style = if (idx % 2 == 0) "4" else "5"

            sb.append("""        <row r="$rowIdx" ht="22" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="$style"><is><t>${idx + 1}</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(memberName)}</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(examName)}</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(exam?.date ?: "-")}</t></is></c>
            <c r="E$rowIdx" t="inlineStr" s="$style"><is><t>${item.score}</t></is></c>
            <c r="F$rowIdx" t="inlineStr" s="$style"><is><t>$maxScore</t></is></c>
            <c r="G$rowIdx" t="inlineStr" s="$style"><is><t>$pct%</t></is></c>
            <c r="H$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(item.notes)}</t></is></c>
        </row>
""")
        }
        sb.append("""    </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildMembersSheetXml(
        members: List<Member>,
        groupMap: Map<Long, Group>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetViews>
        <sheetView tabSelected="0" workbookViewId="0" rightToLeft="1">
            <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
        </sheetView>
    </sheetViews>
    <cols>
        <col min="1" max="1" width="8" customWidth="1"/>
        <col min="2" max="2" width="28" customWidth="1"/>
        <col min="3" max="3" width="20" customWidth="1"/>
        <col min="4" max="4" width="20" customWidth="1"/>
        <col min="5" max="5" width="16" customWidth="1"/>
        <col min="6" max="6" width="16" customWidth="1"/>
        <col min="7" max="7" width="16" customWidth="1"/>
        <col min="8" max="8" width="28" customWidth="1"/>
    </cols>
    <sheetData>
        <row r="1" ht="28" customHeight="1">
            <c r="A1" t="inlineStr" s="1"><is><t>م</t></is></c>
            <c r="B1" t="inlineStr" s="1"><is><t>الاسم بالكامل</t></is></c>
            <c r="C1" t="inlineStr" s="1"><is><t>المجموعة</t></is></c>
            <c r="D1" t="inlineStr" s="1"><is><t>الفصل الدراسي</t></is></c>
            <c r="E1" t="inlineStr" s="1"><is><t>تاريخ الميلاد</t></is></c>
            <c r="F1" t="inlineStr" s="1"><is><t>رقم الهاتف</t></is></c>
            <c r="G1" t="inlineStr" s="1"><is><t>هاتف ولي الأمر</t></is></c>
            <c r="H1" t="inlineStr" s="1"><is><t>العنوان بالتفصيل</t></is></c>
        </row>
""")
        members.forEachIndexed { idx, member ->
            val rowIdx = idx + 2
            val groupName = member.groupId?.let { groupMap[it]?.name } ?: "عام"
            val style = if (idx % 2 == 0) "4" else "5"
            val fullAddress = "${member.area} ${member.street} ${member.houseNumber}".trim()

            sb.append("""        <row r="$rowIdx" ht="22" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="$style"><is><t>${idx + 1}</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.fullName)}</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(groupName)}</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.schoolClass.ifBlank { "-" })}</t></is></c>
            <c r="E$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.birthDate)}</t></is></c>
            <c r="F$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.phone)}</t></is></c>
            <c r="G$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.parentPhone)}</t></is></c>
            <c r="H$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(fullAddress)}</t></is></c>
        </row>
""")
        }
        sb.append("""    </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildVisitsSheetXml(
        members: List<Member>,
        visits: List<VisitRecord>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetViews>
        <sheetView tabSelected="0" workbookViewId="0" rightToLeft="1">
            <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
        </sheetView>
    </sheetViews>
    <cols>
        <col min="1" max="1" width="8" customWidth="1"/>
        <col min="2" max="2" width="28" customWidth="1"/>
        <col min="3" max="3" width="16" customWidth="1"/>
        <col min="4" max="4" width="16" customWidth="1"/>
        <col min="5" max="5" width="18" customWidth="1"/>
        <col min="6" max="6" width="22" customWidth="1"/>
        <col min="7" max="7" width="14" customWidth="1"/>
        <col min="8" max="8" width="24" customWidth="1"/>
        <col min="9" max="9" width="18" customWidth="1"/>
        <col min="10" max="10" width="16" customWidth="1"/>
        <col min="11" max="11" width="16" customWidth="1"/>
        <col min="12" max="12" width="16" customWidth="1"/>
        <col min="13" max="13" width="16" customWidth="1"/>
        <col min="14" max="14" width="30" customWidth="1"/>
    </cols>
    <sheetData>
        <row r="1" ht="28" customHeight="1">
            <c r="A1" t="inlineStr" s="1"><is><t>م</t></is></c>
            <c r="B1" t="inlineStr" s="1"><is><t>اسم المخدوم</t></is></c>
            <c r="C1" t="inlineStr" s="1"><is><t>المحافظة</t></is></c>
            <c r="D1" t="inlineStr" s="1"><is><t>المركز</t></is></c>
            <c r="E1" t="inlineStr" s="1"><is><t>المنطقة</t></is></c>
            <c r="F1" t="inlineStr" s="1"><is><t>الشارع</t></is></c>
            <c r="G1" t="inlineStr" s="1"><is><t>رقم المنزل</t></is></c>
            <c r="H1" t="inlineStr" s="1"><is><t>العنوان التفصيلي</t></is></c>
            <c r="I1" t="inlineStr" s="1"><is><t>الصف الدراسي</t></is></c>
            <c r="J1" t="inlineStr" s="1"><is><t>رقم الهاتف</t></is></c>
            <c r="K1" t="inlineStr" s="1"><is><t>رقم ولي الأمر</t></is></c>
            <c r="L1" t="inlineStr" s="1"><is><t>تاريخ آخر افتقاد</t></is></c>
            <c r="M1" t="inlineStr" s="1"><is><t>حالة الافتقاد</t></is></c>
            <c r="N1" t="inlineStr" s="1"><is><t>ملاحظات الافتقاد</t></is></c>
        </row>
""")
        val visitsByMember = visits.groupBy { it.memberId }
        members.forEachIndexed { idx, member ->
            val rowIdx = idx + 2
            val memberVisits = visitsByMember[member.id]?.sortedByDescending { it.visitDate } ?: emptyList()
            val latestVisit = memberVisits.firstOrNull()
            val visitStatus = if (latestVisit != null) "تم الافتقاد" else "لم يتم الافتقاد"
            val lastDate = latestVisit?.visitDate ?: "-"
            val notes = latestVisit?.notes ?: "-"
            val style = if (idx % 2 == 0) "4" else "5"

            sb.append("""        <row r="$rowIdx" ht="22" customHeight="1">
            <c r="A$rowIdx" t="inlineStr" s="$style"><is><t>${idx + 1}</t></is></c>
            <c r="B$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.fullName)}</t></is></c>
            <c r="C$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.governorate.ifBlank { "أسيوط" })}</t></is></c>
            <c r="D$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.center.ifBlank { "القوصية" })}</t></is></c>
            <c r="E$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.area.ifBlank { "مير" })}</t></is></c>
            <c r="F$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.street.ifBlank { "-" })}</t></is></c>
            <c r="G$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.houseNumber.ifBlank { "-" })}</t></is></c>
            <c r="H$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.addressDetails.ifBlank { "-" })}</t></is></c>
            <c r="I$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.schoolClass.ifBlank { "-" })}</t></is></c>
            <c r="J$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.phone.ifBlank { "-" })}</t></is></c>
            <c r="K$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(member.parentPhone.ifBlank { "-" })}</t></is></c>
            <c r="L$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(lastDate)}</t></is></c>
            <c r="M$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(visitStatus)}</t></is></c>
            <c r="N$rowIdx" t="inlineStr" s="$style"><is><t>${escapeXml(notes)}</t></is></c>
        </row>
""")
        }
        sb.append("""    </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildTemplateContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun buildTemplateWorkbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

    private fun buildTemplateWorkbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="سجل المخدومين" sheetId="1" r:id="rId1"/>
    </sheets>
</workbook>"""

    private fun buildTemplateSheetXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetViews>
        <sheetView tabSelected="1" workbookViewId="0" rightToLeft="1">
            <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
        </sheetView>
    </sheetViews>
    <cols>
        <col min="1" max="1" width="8" customWidth="1"/>
        <col min="2" max="2" width="28" customWidth="1"/>
        <col min="3" max="3" width="20" customWidth="1"/>
        <col min="4" max="4" width="20" customWidth="1"/>
        <col min="5" max="5" width="16" customWidth="1"/>
        <col min="6" max="6" width="16" customWidth="1"/>
        <col min="7" max="7" width="24" customWidth="1"/>
        <col min="8" max="8" width="16" customWidth="1"/>
        <col min="9" max="9" width="16" customWidth="1"/>
        <col min="10" max="10" width="16" customWidth="1"/>
        <col min="11" max="11" width="18" customWidth="1"/>
        <col min="12" max="12" width="20" customWidth="1"/>
        <col min="13" max="13" width="14" customWidth="1"/>
        <col min="14" max="14" width="24" customWidth="1"/>
        <col min="15" max="15" width="28" customWidth="1"/>
    </cols>
    <sheetData>
        <row r="1" ht="28" customHeight="1">
            <c r="A1" t="inlineStr" s="1"><is><t>كود المخدوم (ID)</t></is></c>
            <c r="B1" t="inlineStr" s="1"><is><t>الاسم بالكامل *</t></is></c>
            <c r="C1" t="inlineStr" s="1"><is><t>المجموعة *</t></is></c>
            <c r="D1" t="inlineStr" s="1"><is><t>الفصل الدراسي</t></is></c>
            <c r="E1" t="inlineStr" s="1"><is><t>تاريخ الميلاد</t></is></c>
            <c r="F1" t="inlineStr" s="1"><is><t>رقم الهاتف</t></is></c>
            <c r="G1" t="inlineStr" s="1"><is><t>اسم ولي الأمر</t></is></c>
            <c r="H1" t="inlineStr" s="1"><is><t>هاتف ولي الأمر</t></is></c>
            <c r="I1" t="inlineStr" s="1"><is><t>المحافظة</t></is></c>
            <c r="J1" t="inlineStr" s="1"><is><t>المركز / المدينة</t></is></c>
            <c r="K1" t="inlineStr" s="1"><is><t>المنطقة / القرية</t></is></c>
            <c r="L1" t="inlineStr" s="1"><is><t>الشارع</t></is></c>
            <c r="M1" t="inlineStr" s="1"><is><t>رقم المنزل</t></is></c>
            <c r="N1" t="inlineStr" s="1"><is><t>تفاصيل العنوان</t></is></c>
            <c r="O1" t="inlineStr" s="1"><is><t>ملاحظات</t></is></c>
        </row>
        <row r="2" ht="22" customHeight="1">
            <c r="A2" t="inlineStr" s="4"><is><t></t></is></c>
            <c r="B2" t="inlineStr" s="4"><is><t>كيرلس صبري فؤاد</t></is></c>
            <c r="C2" t="inlineStr" s="4"><is><t>مجموعة إبصالتوس (أ)</t></is></c>
            <c r="D2" t="inlineStr" s="4"><is><t>أولى إعدادي</t></is></c>
            <c r="E2" t="inlineStr" s="4"><is><t>2012-05-15</t></is></c>
            <c r="F2" t="inlineStr" s="4"><is><t>01234567890</t></is></c>
            <c r="G2" t="inlineStr" s="4"><is><t>صبري فؤاد</t></is></c>
            <c r="H2" t="inlineStr" s="4"><is><t>01234567891</t></is></c>
            <c r="I2" t="inlineStr" s="4"><is><t>أسيوط</t></is></c>
            <c r="J2" t="inlineStr" s="4"><is><t>القوصية</t></is></c>
            <c r="K2" t="inlineStr" s="4"><is><t>مير</t></is></c>
            <c r="L2" t="inlineStr" s="4"><is><t>شارع الكنيسة</t></is></c>
            <c r="M2" t="inlineStr" s="4"><is><t>12</t></is></c>
            <c r="N2" t="inlineStr" s="4"><is><t>بجوار الكنيسة</t></is></c>
            <c r="O2" t="inlineStr" s="4"><is><t>شماس متميز</t></is></c>
        </row>
    </sheetData>
</worksheet>"""
}
