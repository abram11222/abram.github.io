package com.example.util

/**
 * Configuration model for customizable Excel exports and in-app reports.
 * Allows full control over target scope, time period, individual column selection,
 * and multi-sheet vs single-sheet workbook layouts.
 */
data class ExcelExportConfig(
    // Target Scope
    val memberId: Long? = null,
    val groupId: Long? = null,
    val classFilter: String? = null,
    val classFilters: Set<String> = emptySet(),

    // Evaluation Period
    val periodType: Int = ExcelExporter.PERIOD_MONTH,
    val customStartDate: String = "",
    val customEndDate: String = "",

    // Structure Mode: True = Multi-sheet (.xlsx with individual sheets), False = Single aggregated sheet
    val isMultiSheetMode: Boolean = true,

    // Selected Sheets for Multi-Sheet Mode
    val includeSummarySheet: Boolean = true,
    val includeAttendanceSheet: Boolean = true,
    val includeMassSheet: Boolean = true,
    val includeHymnsSheet: Boolean = true,
    val includeBibleSheet: Boolean = true,
    val includeExamsSheet: Boolean = true,
    val includeMembersSheet: Boolean = true,
    val includeVisitsSheet: Boolean = true,

    // Personal Data Columns (البيانات الشخصية)
    val includeMemberName: Boolean = true, // Always true
    val includeGroupName: Boolean = false,
    val includeSchoolClass: Boolean = true,
    val includeStatus: Boolean = false,
    val includeBirthDate: Boolean = false,
    val includePhone: Boolean = false,
    val includeParentName: Boolean = false,
    val includeParentPhone: Boolean = false,
    val includeAddress: Boolean = false,

    // Service Attendance Columns (حضور الخدمة)
    val includeServicePresent: Boolean = true,
    val includeServiceAbsent: Boolean = true,
    val includeServiceExcused: Boolean = true,
    val includeServicePercentage: Boolean = true,

    // Mass Attendance Columns (حضور القداسات)
    val includeMassPresent: Boolean = true,
    val includeMassPercentage: Boolean = true,

    // Hymns Columns (الألحان)
    val includeHymnScores: Boolean = true,
    val includeHymnAverage: Boolean = true,

    // Bible Columns (الإنجيل)
    val includeBibleScores: Boolean = true,
    val includeBibleAverage: Boolean = true,

    // Exams Columns (الامتحانات)
    val includeExamScores: Boolean = true,
    val includeExamAverage: Boolean = true,

    // Results & Grading (النتيجة والتقدير)
    val includeTotalScore: Boolean = true,
    val includeTotalPercentage: Boolean = true,
    val includeOverallGrade: Boolean = true,
    val includeNotes: Boolean = false
) {
    fun toExportFilter(): ExcelExporter.ExportFilter {
        return ExcelExporter.ExportFilter(
            memberId = memberId,
            groupId = groupId,
            classFilter = classFilter,
            classFilters = if (classFilters.isNotEmpty()) classFilters else if (!classFilter.isNullOrBlank()) setOf(classFilter) else emptySet(),
            periodType = periodType,
            customStartDate = customStartDate,
            customEndDate = customEndDate
        )
    }

    companion object {
        val DEFAULT = ExcelExportConfig()

        fun allSelected(base: ExcelExportConfig = DEFAULT): ExcelExportConfig {
            return base.copy(
                includeSummarySheet = true,
                includeAttendanceSheet = true,
                includeMassSheet = true,
                includeHymnsSheet = true,
                includeBibleSheet = true,
                includeExamsSheet = true,
                includeMembersSheet = true,
                includeVisitsSheet = true,
                includeMemberName = true,
                includeGroupName = false,
                includeSchoolClass = true,
                includeStatus = false,
                includeBirthDate = true,
                includePhone = true,
                includeParentName = false,
                includeParentPhone = true,
                includeAddress = true,
                includeServicePresent = true,
                includeServiceAbsent = true,
                includeServiceExcused = true,
                includeServicePercentage = true,
                includeMassPresent = true,
                includeMassPercentage = true,
                includeHymnScores = true,
                includeHymnAverage = true,
                includeBibleScores = true,
                includeBibleAverage = true,
                includeExamScores = true,
                includeExamAverage = true,
                includeTotalScore = true,
                includeTotalPercentage = true,
                includeOverallGrade = true,
                includeNotes = true
            )
        }

        fun standardSelected(base: ExcelExportConfig = DEFAULT): ExcelExportConfig {
            return base.copy(
                includeSummarySheet = true,
                includeAttendanceSheet = true,
                includeMassSheet = true,
                includeHymnsSheet = true,
                includeBibleSheet = true,
                includeExamsSheet = true,
                includeMembersSheet = true,
                includeVisitsSheet = true,
                includeMemberName = true,
                includeGroupName = false,
                includeSchoolClass = true,
                includeStatus = false,
                includeBirthDate = false,
                includePhone = false,
                includeParentName = false,
                includeParentPhone = false,
                includeAddress = false,
                includeServicePresent = true,
                includeServiceAbsent = true,
                includeServiceExcused = true,
                includeServicePercentage = true,
                includeMassPresent = true,
                includeMassPercentage = true,
                includeHymnScores = true,
                includeHymnAverage = true,
                includeBibleScores = true,
                includeBibleAverage = true,
                includeExamScores = true,
                includeExamAverage = true,
                includeTotalScore = true,
                includeTotalPercentage = true,
                includeOverallGrade = true,
                includeNotes = false
            )
        }
    }
}
