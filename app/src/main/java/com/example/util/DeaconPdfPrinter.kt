package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High quality ID Card Generator, PDF Export & Native Android Spooler Printer
 * for "خدمة الشمامسة - كنيسة أبي سيفين والعزب ودير الملاك ميخائيل".
 */
object DeaconPdfPrinter {

    const val CHURCH_NAME = "كنيسة أبي سيفين والعزب ودير الملاك ميخائيل"
    const val DIOCESE_NAME = "إيبارشية القوصية ومير"
    const val SERVICE_TITLE = "خدمة ومدرسة الشمامسة"

    /**
     * Generates a high-resolution ID Card Bitmap for a member.
     * Contains Church Header, Photo, Name, Class/Semester, DOB, Phones, Detailed Address, and Square QR Code.
     */
    fun createIdCardBitmap(
        context: Context,
        member: Member,
        cardWidth: Int = 1100, // High resolution CR80 ratio
        cardHeight: Int = 700
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: Elegant deep dark canvas
        val bgPaint = Paint().apply {
            color = Color.parseColor("#141414")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), bgPaint)

        // Outer & Inner Gold Borders
        val goldPaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        val innerMargin = 16f
        canvas.drawRoundRect(
            RectF(innerMargin, innerMargin, cardWidth - innerMargin, cardHeight - innerMargin),
            24f,
            24f,
            goldPaint
        )

        // Header Background Banner
        val headerPaint = Paint().apply {
            color = Color.parseColor("#222222")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(innerMargin + 4, innerMargin + 4, cardWidth - innerMargin - 4, 145f),
            18f,
            18f,
            headerPaint
        )

        // Cross decoration & Header Text
        val headerTitlePaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("✟ $CHURCH_NAME ✟", cardWidth / 2f, 62f, headerTitlePaint)

        val headerSubPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = false
        }
        canvas.drawText("$DIOCESE_NAME • $SERVICE_TITLE", cardWidth / 2f, 108f, headerSubPaint)

        // Thin Gold Header Separator
        val separatorPaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(innerMargin + 20, 145f, cardWidth - innerMargin - 20, 145f, separatorPaint)

        // 1. Right Side: Member Photo in rounded frame (RTL)
        val photoSize = 210f
        val photoRight = cardWidth - 50f
        val photoLeft = photoRight - photoSize
        val photoTop = 175f
        val photoRect = RectF(photoLeft, photoTop, photoRight, photoTop + photoSize)

        var memberPhoto: Bitmap? = null
        if (!member.profileImage.isNullOrBlank()) {
            try {
                val file = File(member.profileImage)
                if (file.exists()) {
                    memberPhoto = BitmapFactory.decodeFile(file.absolutePath)
                }
            } catch (_: Exception) {}
        }

        val photoFramePaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        if (memberPhoto != null) {
            val src = Rect(0, 0, memberPhoto.width, memberPhoto.height)
            val dst = Rect(photoRect.left.toInt(), photoRect.top.toInt(), photoRect.right.toInt(), photoRect.bottom.toInt())
            canvas.drawBitmap(memberPhoto, src, dst, null)
            canvas.drawRoundRect(photoRect, 16f, 16f, photoFramePaint)
        } else {
            val placeholderBg = Paint().apply {
                color = Color.parseColor("#2A2A2A")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(photoRect, 16f, 16f, placeholderBg)
            canvas.drawRoundRect(photoRect, 16f, 16f, photoFramePaint)
            val placeholderText = Paint().apply {
                color = Color.parseColor("#B0B0B0")
                textSize = 32f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("صورة الشماس", photoRect.centerX(), photoRect.centerY() + 10f, placeholderText)
        }

        // Photo Badge: "شماس"
        val badgePaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.FILL
        }
        val badgeRect = RectF(photoLeft + 30f, photoTop + photoSize + 10f, photoRight - 30f, photoTop + photoSize + 48f)
        canvas.drawRoundRect(badgeRect, 10f, 10f, badgePaint)
        val badgeTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("شماس مخدوم", badgeRect.centerX(), badgeRect.centerY() + 8f, badgeTextPaint)

        // 2. Left Side: Square QR Code in white rounded container
        val barcodeId = BarcodeGenerator.getBarcodeId(member.id)
        val qrSize = 220f
        val qrLeft = 50f
        val qrTop = 175f
        val qrRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)

        // White background with gold border for QR
        val qrBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(qrRect, 16f, 16f, qrBgPaint)
        canvas.drawRoundRect(qrRect, 16f, 16f, photoFramePaint)

        val qrBitmap = BarcodeGenerator.generateQrCodeBitmap(
            content = barcodeId,
            targetSize = qrSize.toInt() - 20
        )
        val qrDst = Rect((qrLeft + 10).toInt(), (qrTop + 10).toInt(), (qrLeft + qrSize - 10).toInt(), (qrTop + qrSize - 10).toInt())
        canvas.drawBitmap(qrBitmap, null, qrDst, null)

        // QR Label below code
        val qrCodeLabelPaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(barcodeId, qrRect.centerX(), qrTop + qrSize + 36f, qrCodeLabelPaint)

        // 3. Center Info Area (RTL: between QR code and Photo)
        val textRightX = photoLeft - 30f
        var startY = 190f

        // Name
        val nameLabelPaint = Paint().apply {
            color = Color.parseColor("#B0B0B0")
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val nameValuePaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = true
        }

        canvas.drawText("الاسم الرباعي:", textRightX, startY, nameLabelPaint)
        startY += 40f
        canvas.drawText(member.fullName, textRightX, startY, nameValuePaint)
        startY += 44f

        // School Class / Stage
        val fieldLabelPaint = Paint().apply {
            color = Color.parseColor("#C8C8C8")
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val fieldValuePaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = true
        }

        val classText = member.schoolClass.ifBlank { "مدرسة الشمامسة" }
        canvas.drawText("المرحلة / الفصل: $classText", textRightX, startY, fieldValuePaint)
        startY += 38f

        // Date of Birth
        val dobText = member.birthDate.ifBlank { "غير مسجل" }
        canvas.drawText("تاريخ الميلاد: $dobText", textRightX, startY, fieldLabelPaint)
        startY += 38f

        // Phones
        val phoneStr = member.phone.ifBlank { "-" }
        val parentPhoneStr = member.parentPhone.ifBlank { "-" }
        canvas.drawText("هاتف: $phoneStr | ولي الأمر: $parentPhoneStr", textRightX, startY, fieldLabelPaint)
        startY += 38f

        // Address
        val addressParts = listOf(member.governorate, member.center, member.area, member.street).filter { it.isNotBlank() }
        val fullAddress = if (addressParts.isNotEmpty()) {
            addressParts.joinToString(" - ") + if (member.houseNumber.isNotBlank()) " (منزل ${member.houseNumber})" else ""
        } else {
            "العنوان مسجل بالخدمة"
        }
        // Truncate address if too long
        val displayAddress = if (fullAddress.length > 45) fullAddress.take(42) + "..." else fullAddress
        canvas.drawText("العنوان: $displayAddress", textRightX, startY, fieldLabelPaint)

        // 4. Bottom Footer Banner
        val footerPaint = Paint().apply {
            color = Color.parseColor("#1C1C1C")
            style = Paint.Style.FILL
        }
        val footerRect = RectF(innerMargin + 4, cardHeight - 110f, cardWidth - innerMargin - 4, cardHeight - innerMargin - 4)
        canvas.drawRoundRect(footerRect, 14f, 14f, footerPaint)

        val footerTextPaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(
            "✟ بطاقة وهوية شماس رسمية معتمدة من خدمة ومدرسة الشمامسة ✟",
            cardWidth / 2f,
            cardHeight - 56f,
            footerTextPaint
        )

        return bitmap
    }

    /**
     * Prints member ID Card using native Android PrintManager.
     */
    fun printMemberIdCard(context: Context, member: Member) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "خدمة الطباعة غير متاحة في هذا الجهاز", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "كارنيه_${member.fullName}_${member.id}"
        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                private var pdfDocument: PdfDocument? = null

                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }

                    val info = PrintDocumentInfo.Builder("id_card_${member.id}.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()

                    callback?.onLayoutFinished(info, newAttributes != oldAttributes)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    pdfDocument = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 Page
                    val page = pdfDocument!!.startPage(pageInfo)

                    val cardBitmap = createIdCardBitmap(context, member, 1050, 650)
                    val canvas = page.canvas

                    // Center card horizontally and vertically on A4
                    val dstRect = Rect(50, 150, 545, 456)
                    canvas.drawBitmap(cardBitmap, null, dstRect, null)

                    val notePaint = Paint().apply {
                        color = Color.DKGRAY
                        textSize = 12f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawText("بطاقة عضوية رسمية معتمدة — $CHURCH_NAME", 595f / 2f, 480f, notePaint)

                    pdfDocument!!.finishPage(page)

                    try {
                        destination?.let {
                            FileOutputStream(it.fileDescriptor).use { out ->
                                pdfDocument!!.writeTo(out)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    } finally {
                        pdfDocument?.close()
                        pdfDocument = null
                    }
                }
            },
            PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        )
    }

    /**
     * Exports member ID Card directly to a printable PDF file on device.
     */
    suspend fun exportMemberIdCardPdf(context: Context, member: Member): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "DeaconCards").apply { mkdirs() }
        val fileName = "Card_${member.id}_${member.fullName.replace(" ", "_")}.pdf"
        val outputFile = File(exportDir, fileName)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)

        val cardBitmap = createIdCardBitmap(context, member, 1050, 650)
        val canvas = page.canvas

        // Header decoration on page
        val headerPaint = Paint().apply {
            color = Color.parseColor("#B8860B")
            textSize = 16f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(CHURCH_NAME, 595f / 2f, 80f, headerPaint)

        // Draw Card
        val dstRect = Rect(50, 120, 545, 426)
        canvas.drawBitmap(cardBitmap, null, dstRect, null)

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 11f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("تاريخ الإصدار: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())} • خدمة الشمامسة", 595f / 2f, 460f, footerPaint)

        pdfDocument.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        outputFile
    }

    /**
     * Shares or Opens any generated PDF file.
     */
    fun sharePdfFile(context: Context, file: File, subject: String = "كارنيه الشماس المعتمد") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "مرفق ملف PDF الرسمي المعتمد من $CHURCH_NAME.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "مشاركة / طباعة ملف PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun openPdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePdfFile(context, file)
        }
    }
}
