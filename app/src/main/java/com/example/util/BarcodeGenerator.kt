package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Member
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Standard Code 128 (Subset B) Barcode Generator & Utilities.
 * 100% offline, pure Kotlin, zero external SDK dependencies.
 */
object BarcodeGenerator {

    /**
     * Code 128 pattern table (values 0 to 106).
     * Each pattern is 6 elements (widths of bar-space-bar-space-bar-space), sum of modules = 11.
     * Stop symbol is index 106 (7 elements, sum of modules = 13).
     */
    private val PATTERNS = arrayOf(
        intArrayOf(2, 1, 2, 2, 2, 2), // 0
        intArrayOf(2, 2, 2, 1, 2, 2), // 1
        intArrayOf(2, 2, 2, 2, 2, 1), // 2
        intArrayOf(1, 2, 1, 2, 2, 3), // 3
        intArrayOf(1, 2, 1, 3, 2, 2), // 4
        intArrayOf(1, 3, 1, 2, 2, 2), // 5
        intArrayOf(1, 2, 2, 2, 1, 3), // 6
        intArrayOf(1, 2, 2, 3, 1, 2), // 7
        intArrayOf(1, 3, 2, 2, 1, 2), // 8
        intArrayOf(2, 2, 1, 2, 1, 3), // 9
        intArrayOf(2, 2, 1, 3, 1, 2), // 10
        intArrayOf(2, 3, 1, 2, 1, 2), // 11
        intArrayOf(1, 1, 2, 2, 3, 2), // 12
        intArrayOf(1, 2, 2, 1, 3, 2), // 13
        intArrayOf(1, 2, 2, 2, 3, 1), // 14
        intArrayOf(1, 1, 3, 2, 2, 2), // 15
        intArrayOf(1, 2, 3, 1, 2, 2), // 16
        intArrayOf(1, 2, 3, 2, 2, 1), // 17
        intArrayOf(2, 2, 3, 2, 1, 1), // 18
        intArrayOf(2, 2, 1, 1, 3, 2), // 19
        intArrayOf(2, 2, 1, 2, 3, 1), // 20
        intArrayOf(2, 1, 3, 2, 1, 2), // 21
        intArrayOf(2, 2, 3, 1, 1, 2), // 22
        intArrayOf(3, 1, 2, 1, 3, 1), // 23
        intArrayOf(3, 1, 1, 2, 2, 2), // 24
        intArrayOf(3, 2, 1, 1, 2, 2), // 25
        intArrayOf(3, 2, 1, 2, 2, 1), // 26
        intArrayOf(3, 1, 2, 2, 1, 2), // 27
        intArrayOf(3, 2, 2, 1, 1, 2), // 28
        intArrayOf(3, 2, 2, 2, 1, 1), // 29
        intArrayOf(2, 1, 2, 1, 2, 3), // 30
        intArrayOf(2, 1, 2, 3, 2, 1), // 31
        intArrayOf(2, 3, 2, 1, 2, 1), // 32
        intArrayOf(1, 1, 1, 3, 2, 3), // 33
        intArrayOf(1, 3, 1, 1, 2, 3), // 34
        intArrayOf(1, 3, 1, 3, 2, 1), // 35
        intArrayOf(1, 1, 2, 3, 1, 3), // 36
        intArrayOf(1, 3, 2, 1, 1, 3), // 37
        intArrayOf(1, 3, 2, 3, 1, 1), // 38
        intArrayOf(2, 1, 1, 3, 1, 3), // 39
        intArrayOf(2, 3, 1, 1, 1, 3), // 40
        intArrayOf(2, 3, 1, 3, 1, 1), // 41
        intArrayOf(1, 1, 2, 1, 3, 3), // 42
        intArrayOf(1, 1, 2, 3, 3, 1), // 43
        intArrayOf(1, 3, 2, 1, 3, 1), // 44
        intArrayOf(1, 1, 3, 1, 2, 3), // 45
        intArrayOf(1, 1, 3, 3, 2, 1), // 46
        intArrayOf(1, 3, 3, 1, 2, 1), // 47
        intArrayOf(3, 1, 3, 1, 2, 1), // 48
        intArrayOf(2, 1, 1, 3, 3, 1), // 49
        intArrayOf(2, 3, 1, 1, 3, 1), // 50
        intArrayOf(2, 1, 3, 1, 1, 3), // 51
        intArrayOf(2, 1, 3, 3, 1, 1), // 52
        intArrayOf(2, 1, 3, 1, 3, 1), // 53
        intArrayOf(3, 1, 1, 1, 2, 3), // 54
        intArrayOf(3, 1, 1, 3, 2, 1), // 55
        intArrayOf(3, 3, 1, 1, 2, 1), // 56
        intArrayOf(3, 1, 2, 1, 1, 3), // 57
        intArrayOf(3, 1, 2, 3, 1, 1), // 58
        intArrayOf(3, 3, 2, 1, 1, 1), // 59
        intArrayOf(3, 1, 4, 1, 1, 1), // 60
        intArrayOf(2, 2, 1, 4, 1, 1), // 61
        intArrayOf(4, 3, 1, 1, 1, 1), // 62
        intArrayOf(1, 1, 1, 2, 2, 4), // 63
        intArrayOf(1, 1, 1, 4, 2, 2), // 64
        intArrayOf(1, 2, 1, 1, 2, 4), // 65
        intArrayOf(1, 2, 1, 4, 2, 1), // 66
        intArrayOf(1, 4, 1, 1, 2, 2), // 67
        intArrayOf(1, 4, 1, 2, 2, 1), // 68
        intArrayOf(1, 1, 2, 2, 1, 4), // 69
        intArrayOf(1, 1, 2, 4, 1, 2), // 70
        intArrayOf(1, 2, 2, 1, 1, 4), // 71
        intArrayOf(1, 2, 2, 4, 1, 1), // 72
        intArrayOf(1, 4, 2, 1, 1, 2), // 73
        intArrayOf(1, 4, 2, 2, 1, 1), // 74
        intArrayOf(2, 4, 1, 2, 1, 1), // 75
        intArrayOf(2, 2, 1, 1, 1, 4), // 76
        intArrayOf(4, 1, 3, 1, 1, 1), // 77
        intArrayOf(2, 4, 1, 1, 1, 2), // 78
        intArrayOf(1, 3, 4, 1, 1, 1), // 79
        intArrayOf(1, 1, 1, 2, 4, 2), // 80
        intArrayOf(1, 2, 1, 1, 4, 2), // 81
        intArrayOf(1, 2, 1, 2, 4, 1), // 82
        intArrayOf(1, 1, 4, 2, 1, 2), // 83
        intArrayOf(1, 2, 4, 1, 1, 2), // 84
        intArrayOf(1, 2, 4, 2, 1, 1), // 85
        intArrayOf(4, 1, 1, 2, 1, 2), // 86
        intArrayOf(4, 2, 1, 1, 1, 2), // 87
        intArrayOf(4, 2, 1, 2, 1, 1), // 88
        intArrayOf(2, 1, 2, 1, 4, 1), // 89
        intArrayOf(2, 1, 4, 1, 2, 1), // 90
        intArrayOf(4, 1, 2, 1, 2, 1), // 91
        intArrayOf(1, 1, 1, 1, 4, 3), // 92
        intArrayOf(1, 1, 1, 3, 4, 1), // 93
        intArrayOf(1, 3, 1, 1, 4, 1), // 94
        intArrayOf(1, 1, 4, 1, 1, 3), // 95
        intArrayOf(1, 1, 4, 3, 1, 1), // 96
        intArrayOf(4, 1, 1, 1, 1, 3), // 97
        intArrayOf(4, 1, 1, 3, 1, 1), // 98
        intArrayOf(1, 1, 3, 1, 4, 1), // 99
        intArrayOf(1, 1, 4, 1, 3, 1), // 100
        intArrayOf(3, 1, 1, 1, 4, 1), // 101
        intArrayOf(4, 1, 1, 1, 3, 1), // 102
        intArrayOf(2, 1, 1, 4, 1, 2), // 103: Start A
        intArrayOf(2, 1, 1, 2, 1, 4), // 104: Start B
        intArrayOf(2, 1, 1, 2, 3, 2), // 105: Start C
        intArrayOf(2, 3, 3, 1, 1, 1, 2) // 106: Stop (13 modules)
    )

    private const val START_B = 104
    private const val STOP = 106

    /**
     * Format stable Barcode ID for member: e.g. DCN-000042
     */
    fun getBarcodeId(memberId: Long): String {
        return "DCN-${memberId.toString().padStart(6, '0')}"
    }

    /**
     * Parse member ID from scanned barcode string.
     * Matches "DCN-000042", "DCN-42", or raw ID "42".
     */
    fun parseMemberId(rawScanned: String): Long? {
        val trimmed = rawScanned.trim()
        if (trimmed.isEmpty()) return null

        // Format DCN-xxxx
        val dcnRegex = Regex("""(?i)^DCN-?(\d+)$""")
        val match = dcnRegex.find(trimmed)
        if (match != null) {
            return match.groupValues[1].toLongOrNull()
        }

        // Pure digits
        return trimmed.toLongOrNull()
    }

    /**
     * Encode ASCII string into a boolean array of modules (true = black bar, false = white space)
     */
    fun encodeToModules(text: String): BooleanArray {
        val symbols = mutableListOf<Int>()
        symbols.add(START_B)

        var checksum = START_B
        for (i in text.indices) {
            val c = text[i]
            val value = c.code - 32
            val safeVal = if (value in 0..95) value else 0
            symbols.add(safeVal)
            checksum += (i + 1) * safeVal
        }

        val checkDigit = checksum % 103
        symbols.add(checkDigit)
        symbols.add(STOP)

        val moduleList = mutableListOf<Boolean>()
        // Quiet zone at start (10 modules)
        repeat(10) { moduleList.add(false) }

        for (sym in symbols) {
            val pattern = PATTERNS[sym]
            var isBar = true
            for (width in pattern) {
                repeat(width) { moduleList.add(isBar) }
                isBar = !isBar
            }
        }

        // Quiet zone at end (10 modules)
        repeat(10) { moduleList.add(false) }

        return moduleList.toBooleanArray()
    }

    /**
     * Generate high-quality Android Bitmap with barcode bars and member text label.
     */
    fun generateBarcodeBitmap(
        barcodeText: String,
        memberName: String = "",
        targetWidth: Int = 640,
        targetHeight: Int = 240
    ): Bitmap {
        val modules = encodeToModules(barcodeText)
        val moduleCount = modules.size
        val scale = (targetWidth.toFloat() / moduleCount).coerceAtLeast(2f)
        val bitmapWidth = (moduleCount * scale).toInt()
        val bitmapHeight = targetHeight.coerceAtLeast(160)

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val barPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = false
            style = Paint.Style.FILL
        }

        val barHeight = bitmapHeight - 60f

        var currentX = 0f
        for (isBar in modules) {
            val nextX = currentX + scale
            if (isBar) {
                canvas.drawRect(currentX, 15f, nextX, barHeight, barPaint)
            }
            currentX = nextX
        }

        // Draw text label below barcode
        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val displayLabel = if (memberName.isNotBlank()) "$barcodeText • $memberName" else barcodeText
        canvas.drawText(displayLabel, bitmapWidth / 2f, bitmapHeight - 16f, textPaint)

        return bitmap
    }

    /**
     * Save generated barcode bitmap to local device storage / MediaStore.
     */
    fun saveBarcodeToGallery(context: Context, bitmap: Bitmap, barcodeId: String, memberName: String): Uri? {
        val fileName = "Barcode_${barcodeId}_${System.currentTimeMillis()}.png"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DeaconsBarcodes")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    Toast.makeText(context, "تم حفظ الباركود في الصور بنجاح", Toast.LENGTH_SHORT).show()
                    uri
                } else null
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val folder = File(picturesDir, "DeaconsBarcodes").apply { mkdirs() }
                val file = File(folder, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri = Uri.fromFile(file)
                Toast.makeText(context, "تم حفظ الباركود بنجاح في: Pictures/DeaconsBarcodes", Toast.LENGTH_SHORT).show()
                uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: save to app internal cache
            try {
                val cacheFile = File(context.cacheDir, fileName)
                FileOutputStream(cacheFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(context, "تم حفظ الباركود مؤقتًا بنجاح", Toast.LENGTH_SHORT).show()
                Uri.fromFile(cacheFile)
            } catch (_: Exception) {
                Toast.makeText(context, "تعذر حفظ الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
                null
            }
        }
    }

    /**
     * Share barcode image and details via Android Intent.
     */
    fun shareBarcode(context: Context, bitmap: Bitmap, member: Member) {
        try {
            val barcodeId = getBarcodeId(member.id)
            val cachePath = File(context.cacheDir, "shared_barcodes").apply { mkdirs() }
            val file = File(cachePath, "barcode_${barcodeId}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "باركود الشماس: ${member.fullName}"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "بطاقة باركود الشماس:\n" +
                            "الاسم: ${member.fullName}\n" +
                            "الفصل: ${member.schoolClass.ifBlank { "مدرسة الشمامسة" }}\n" +
                            "رقم الباركود: $barcodeId\n" +
                            "مدرسة القديس اسطفانوس للشمامسة بمير"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة باركود المخدوم"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر مشاركة الباركود: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a square QR Code Bitmap (100% offline, pure Kotlin matrix generation).
     * High resolution square QR code suitable for camera scanning and ID card printing.
     */
    fun generateQrCodeBitmap(
        content: String,
        targetSize: Int = 400,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val qrMatrix = QrCodeEncoder.encode(content)
        val matrixSize = qrMatrix.size
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, targetSize.toFloat(), targetSize.toFloat(), bgPaint)

        val fgPaint = Paint().apply {
            color = foregroundColor
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        // Add 2-module quiet zone padding
        val totalModules = matrixSize + 4
        val moduleSize = targetSize.toFloat() / totalModules.toFloat()
        val offset = 2 * moduleSize

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (qrMatrix[r][c]) {
                    val left = offset + (c * moduleSize)
                    val top = offset + (r * moduleSize)
                    canvas.drawRect(left, top, left + moduleSize, top + moduleSize, fgPaint)
                }
            }
        }

        return bitmap
    }
}

/**
 * Pure Kotlin QR Code Model 2 Matrix Encoder.
 * Supports Byte/Alphanumeric Encoding with Reed-Solomon Error Correction.
 */
private object QrCodeEncoder {

    // Galois Field GF(256) with primitive polynomial 0x11D (285)
    private val EXP = IntArray(512)
    private val LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            EXP[i] = x
            EXP[i + 255] = x
            LOG[x] = i
            x = (x shl 1) xor (if ((x and 0x80) != 0) 0x11D else 0)
        }
        LOG[0] = 0
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return EXP[LOG[x] + LOG[y]]
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            for (j in poly.indices) {
                next[j] = next[j] xor gfMul(poly[j], EXP[i])
                next[j + 1] = next[j + 1] xor poly[j]
            }
            poly = next
        }
        return poly
    }

    private fun rsEncode(data: IntArray, ecCount: Int): IntArray {
        val gen = rsGeneratorPoly(ecCount)
        val res = IntArray(data.size + ecCount)
        System.arraycopy(data, 0, res, 0, data.size)
        for (i in data.indices) {
            val coef = res[i]
            if (coef != 0) {
                for (j in gen.indices) {
                    res[i + j] = res[i + j] xor gfMul(gen[j], coef)
                }
            }
        }
        val ec = IntArray(ecCount)
        System.arraycopy(res, data.size, ec, 0, ecCount)
        return ec
    }

    fun encode(text: String): Array<BooleanArray> {
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        val isAlphanumeric = text.all { it in "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:" }

        // Choose Version 1 (21x21) or Version 2 (25x25) or Version 3 (29x29) based on payload
        val version = when {
            rawBytes.size <= 14 -> 1
            rawBytes.size <= 26 -> 2
            else -> 3
        }

        val (dataCap, ecCap, size) = when (version) {
            1 -> Triple(16, 10, 21) // 16 data codewords, 10 EC codewords
            2 -> Triple(28, 16, 25) // 28 data codewords, 16 EC codewords
            else -> Triple(44, 26, 29) // 44 data codewords, 26 EC codewords
        }

        // Build data bitstream (Byte Mode: 0100)
        val bitBuffer = mutableListOf<Int>()
        fun putBits(value: Int, length: Int) {
            for (i in (length - 1) downTo 0) {
                bitBuffer.add((value ushr i) and 1)
            }
        }

        putBits(0b0100, 4) // Mode: Byte
        putBits(rawBytes.size, 8) // Character count
        for (b in rawBytes) {
            putBits(b.toInt() and 0xFF, 8)
        }

        // Terminator (up to 4 zeroes)
        val totalDataBits = dataCap * 8
        val termLen = (totalDataBits - bitBuffer.size).coerceIn(0, 4)
        for (i in 0 until termLen) bitBuffer.add(0)

        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0) bitBuffer.add(0)

        // Pad bytes (0xEC, 0x11)
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIndex = 0
        while (bitBuffer.size < totalDataBits) {
            putBits(padBytes[padIndex % 2], 8)
            padIndex++
        }

        // Convert bit buffer to data codewords
        val dataCodewords = IntArray(dataCap)
        for (i in 0 until dataCap) {
            var byteVal = 0
            for (b in 0 until 8) {
                byteVal = (byteVal shl 1) or bitBuffer[i * 8 + b]
            }
            dataCodewords[i] = byteVal
        }

        // Generate Error Correction Codewords
        val ecCodewords = rsEncode(dataCodewords, ecCap)
        val allCodewords = dataCodewords + ecCodewords

        // Initialize matrix
        val matrix = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setModule(r: Int, c: Int, isBlack: Boolean, isFunc: Boolean = true) {
            if (r in 0 until size && c in 0 until size) {
                matrix[r][c] = isBlack
                if (isFunc) isFunction[r][c] = true
            }
        }

        // 1. Finder Patterns (7x7)
        fun placeFinder(row: Int, col: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val mr = row + r
                    val mc = col + c
                    if (mr in 0 until size && mc in 0 until size) {
                        val isBlack = (r in 0..6 && (c == 0 || c == 6)) ||
                                (c in 0..6 && (r == 0 || r == 6)) ||
                                (r in 2..4 && c in 2..4)
                        setModule(mr, mc, isBlack, isFunc = true)
                    }
                }
            }
        }

        placeFinder(0, 0)
        placeFinder(0, size - 7)
        placeFinder(size - 7, 0)

        // 2. Alignment Patterns (for version >= 2)
        if (version >= 2) {
            val alignPos = if (version == 2) intArrayOf(18) else intArrayOf(22)
            for (pos in alignPos) {
                for (r in -2..2) {
                    for (c in -2..2) {
                        val mr = pos + r
                        val mc = pos + c
                        if (!isFunction[mr][mc]) {
                            val isBlack = (r == -2 || r == 2 || c == -2 || c == 2 || (r == 0 && c == 0))
                            setModule(mr, mc, isBlack, isFunc = true)
                        }
                    }
                }
            }
        }

        // 3. Timing Patterns
        for (i in 8 until (size - 8)) {
            val isBlack = (i % 2 == 0)
            setModule(6, i, isBlack, isFunc = true)
            setModule(i, 6, isBlack, isFunc = true)
        }

        // Dark Module
        setModule(4 * version + 9, 8, true, isFunc = true)

        // Reserve Format Information Areas
        for (i in 0..8) {
            isFunction[8][i] = true
            isFunction[i][8] = true
        }
        for (i in (size - 8) until size) {
            isFunction[8][i] = true
            isFunction[size - 1 - (size - 1 - i)][8] = true
        }

        // 4. Fill Data Codewords with Mask (Mask pattern 0: (row + col) % 2 == 0)
        val allBits = mutableListOf<Boolean>()
        for (cw in allCodewords) {
            for (b in 7 downTo 0) {
                allBits.add(((cw ushr b) and 1) != 0)
            }
        }

        var bitIdx = 0
        var right = size - 1
        var upward = true

        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing column
            val rows = if (upward) (size - 1 downTo 0).toList() else (0 until size).toList()
            for (r in rows) {
                for (c in intArrayOf(right, right - 1)) {
                    if (!isFunction[r][c]) {
                        val dataBit = if (bitIdx < allBits.size) allBits[bitIdx++] else false
                        // Mask 0: (row + col) % 2 == 0
                        val maskBit = (r + c) % 2 == 0
                        matrix[r][c] = dataBit xor maskBit
                    }
                }
            }
            right -= 2
            upward = !upward
        }

        // 5. Place Format Information (EC Level M = 00, Mask 0 = 000 -> Format Bits: 101010000010010 XOR 101010000010010 = 000000000000000)
        // Standard Format bits for EC=M, Mask=0 with BCH(15,5) XOR 0x5412: 0x5412 = 0b101010000010010
        val formatBits = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0)
        for (i in 0 until 15) {
            val bit = formatBits[i] == 1
            // Top-left format info
            if (i <= 5) setModule(8, i, bit, isFunc = true)
            else if (i == 6) setModule(8, 7, bit, isFunc = true)
            else if (i == 7) setModule(8, 8, bit, isFunc = true)
            else if (i == 8) setModule(7, 8, bit, isFunc = true)
            else setModule(14 - i, 8, bit, isFunc = true)

            // Bottom-left / Top-right format info
            if (i < 7) setModule(size - 1 - i, 8, bit, isFunc = true)
            else setModule(8, size - 15 + i, bit, isFunc = true)
        }

        return matrix
    }
}

