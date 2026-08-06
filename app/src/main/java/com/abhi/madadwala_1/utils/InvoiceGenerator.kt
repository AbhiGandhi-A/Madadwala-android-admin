package com.abhi.madadwala_1.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.abhi.madadwala_1.R
import com.abhi.madadwala_1.data.remote.BookingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object InvoiceGenerator {

    suspend fun generateInvoice(context: Context, booking: BookingResponse) {
        withContext(Dispatchers.IO) {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
                isDither = true
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            // ---------- Palette ----------
            val primaryGreen = Color.parseColor("#1E5631")
            val lightGreen = Color.parseColor("#E8F5E9")
            val gray = Color.parseColor("#6B7280")
            val lightGray = Color.parseColor("#F3F4F6")
            val darkText = Color.parseColor("#16302A")
            val borderGray = Color.parseColor("#E5E7EB")
            val goldStar = Color.parseColor("#FBBF24")
            val white = Color.WHITE

            val marginX = 40f
            val contentRight = pageWidth - 40f
            val contentWidth = contentRight - marginX

            // ============================================================
            // HEADER
            // ============================================================
            try {
                val logo = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
                if (logo != null) {
                    val logoWidth = 60f
                    val logoHeight = (logoWidth / logo.width * logo.height)
                    val destRect = RectF(marginX, 30f, marginX + logoWidth, 30f + logoHeight)
                    canvas.drawBitmap(logo, null, destRect, paint)
                }
            } catch (e: Exception) {}

            textPaint.color = primaryGreen
            textPaint.textSize = 26f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Madadwala", marginX + 70f, 55f, textPaint)

            textPaint.color = gray
            textPaint.textSize = 10f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Har Ghar Ke Liye, Har Kaam Ke Liye", marginX + 70f, 70f, textPaint)

            // TAX INVOICE badge
            paint.color = primaryGreen
            val badgeRect = RectF(contentRight - 110f, 30f, contentRight, 52f)
            canvas.drawRoundRect(badgeRect, 6f, 6f, paint)
            textPaint.color = white
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 11f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TAX INVOICE", badgeRect.centerX(), badgeRect.centerY() + 4f, textPaint)

            // Invoice meta
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = darkText
            textPaint.textSize = 9.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val invoiceNo = "INV-${booking._id.takeLast(8).uppercase()}"
            val bookingIdStr = "BK-${booking._id.takeLast(8).uppercase()}"
            canvas.drawText("Invoice No.  : $invoiceNo", contentRight, 68f, textPaint)
            canvas.drawText("Invoice Date : ${formatDate(booking.createdAt)}", contentRight, 82f, textPaint)
            canvas.drawText("Booking ID   : $bookingIdStr", contentRight, 96f, textPaint)

            // Divider
            paint.color = primaryGreen
            canvas.drawRect(marginX, 118f, contentRight, 120f, paint)

            // ============================================================
            // BILLED TO / SERVICE PROVIDER
            // ============================================================
            var y = 145f
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = primaryGreen
            textPaint.textSize = 10.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("BILLED TO", marginX, y, textPaint)
            canvas.drawText("SERVICE PROVIDER", 320f, y, textPaint)

            y += 20f
            textPaint.color = darkText
            textPaint.textSize = 13f
            canvas.drawText(booking.customerName ?: "Customer", marginX, y, textPaint)

            y += 18f
            textPaint.textSize = 9.5f
            textPaint.color = gray
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            drawPhoneIcon(canvas, marginX, y - 6f, 8f, gray)
            canvas.drawText(booking.customerPhone ?: "", marginX + 14f, y, textPaint)

            y += 16f
            drawLocationIcon(canvas, marginX + 2f, y - 8f, 7f, gray)
            val addressLines = wrapText(booking.address, 220f, textPaint)
            var addrY = y
            for (line in addressLines) {
                canvas.drawText(line, marginX + 14f, addrY, textPaint)
                addrY += 12f
            }

            // Provider block
            val providerTopY = 165f
            val providerImage = booking.providerImage?.let { getBitmapFromUrl(context, it) }
            if (providerImage != null) {
                val circularBitmap = getCircularBitmap(providerImage)
                val destRect = RectF(320f, providerTopY, 320f + 46f, providerTopY + 46f)
                canvas.drawBitmap(circularBitmap, null, destRect, paint)
            } else {
                paint.color = lightGray
                canvas.drawCircle(343f, providerTopY + 23f, 23f, paint)
            }

            textPaint.color = darkText
            textPaint.textSize = 12f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val providerName = booking.providerName ?: "Partner"
            canvas.drawText(providerName, 375f, providerTopY + 12f, textPaint)
            drawCheckBadge(canvas, 375f + textPaint.measureText(providerName) + 10f, providerTopY + 8f, 6f, primaryGreen)

            textPaint.textSize = 9.5f
            textPaint.color = gray
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(booking.serviceName, 375f, providerTopY + 27f, textPaint)

            drawStar(canvas, 375f + 4f, providerTopY + 40f, 5f, goldStar)
            canvas.drawText(" 4.8 (Verified Partner)", 375f + 12f, providerTopY + 44f, textPaint)

            // ============================================================
            // SERVICE DETAILS TABLE
            // ============================================================
            y = 250f
            paint.color = primaryGreen
            canvas.drawRect(marginX, y, contentRight, y + 20f, paint)
            textPaint.color = white
            textPaint.textSize = 10f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("SERVICE DETAILS", marginX + 10f, y + 14f, textPaint)
            y += 20f

            val details = listOf(
                Triple("Service", booking.serviceName, false),
                Triple("Service Date & Time", booking.scheduledTime, false),
                Triple("Status", booking.status.uppercase(), true),
                Triple("Payment Status", booking.paymentStatus?.uppercase() ?: "PENDING", false)
            )
            for ((label, value, highlight) in details) {
                drawServiceDetailRow(
                    canvas, marginX, y, contentRight, label, value,
                    textPaint, paint, highlight, primaryGreen, gray, darkText, borderGray
                )
                y += 22f
            }

            // ============================================================
            // ITEMS TABLE
            // ============================================================
            y += 15f
            paint.color = primaryGreen
            canvas.drawRect(marginX, y, contentRight, y + 20f, paint)
            textPaint.color = white
            textPaint.textSize = 9f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("#", marginX + 10f, y + 14f, textPaint)
            canvas.drawText("DESCRIPTION", marginX + 40f, y + 14f, textPaint)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("QTY", 380f, y + 14f, textPaint)
            canvas.drawText("UNIT PRICE", 460f, y + 14f, textPaint)
            canvas.drawText("AMOUNT", contentRight - 30f, y + 14f, textPaint)
            y += 20f

            // NOTE: BookingResponse only carries a single totalAmount (no itemized
            // line items from the backend), so we render one row built from the
            // booking's service + total. If itemized pricing is added to the API
            // later, replace this single-item list with the real items array.
            val totalDouble = booking.totalAmount.toDouble()
            val items = listOf(Triple(booking.serviceName, 1, totalDouble))

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 9.5f
            items.forEachIndexed { index, (desc, qty, price) ->
                paint.style = Paint.Style.STROKE
                paint.color = borderGray
                canvas.drawRect(marginX, y, contentRight, y + 24f, paint)
                paint.style = Paint.Style.FILL
                textPaint.color = darkText
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("${index + 1}", marginX + 10f, y + 16f, textPaint)
                canvas.drawText(desc, marginX + 40f, y + 16f, textPaint)
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("$qty", 380f, y + 16f, textPaint)
                canvas.drawText("\u20B9${formatAmount(price)}", 460f, y + 16f, textPaint)
                canvas.drawText("\u20B9${formatAmount(price)}", contentRight - 30f, y + 16f, textPaint)
                y += 24f
            }

            // ============================================================
            // PAYMENT SUMMARY + TOTAL BOX
            // ============================================================
            y += 15f
            val total = totalDouble
            val isPaid = booking.paymentStatus?.lowercase() == "paid" || booking.status.lowercase() == "done"
            val paidAmount = if (isPaid) total else 0.0

            val summaryRect = RectF(marginX, y, 285f, y + 120f)
            paint.style = Paint.Style.FILL
            paint.color = white
            canvas.drawRoundRect(summaryRect, 8f, 8f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = borderGray
            canvas.drawRoundRect(summaryRect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = primaryGreen
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 10f
            canvas.drawText("PAYMENT SUMMARY", marginX + 10f, y + 18f, textPaint)

            textPaint.color = gray
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 9.5f
            canvas.drawText("Subtotal", marginX + 10f, y + 38f, textPaint)
            canvas.drawText("Platform Fee", marginX + 10f, y + 53f, textPaint)
            canvas.drawText("Convenience Fee", marginX + 10f, y + 68f, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = darkText
            canvas.drawText("\u20B9${formatAmount(total)}", 275f, y + 38f, textPaint)
            canvas.drawText("\u20B90.00", 275f, y + 53f, textPaint)
            canvas.drawText("\u20B90.00", 275f, y + 68f, textPaint)

            paint.color = borderGray
            canvas.drawLine(marginX + 10f, y + 78f, 275f, y + 78f, paint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = darkText
            textPaint.textSize = 10f
            canvas.drawText("Total Payable", marginX + 10f, y + 92f, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("\u20B9${formatAmount(total)}", 275f, y + 92f, textPaint)

            // Amount paid highlighted row
            paint.color = lightGreen
            canvas.drawRect(marginX, y + 100f, 285f, y + 120f, paint)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = primaryGreen
            canvas.drawText("Amount Paid", marginX + 10f, y + 114f, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("\u20B9${formatAmount(paidAmount)}", 275f, y + 114f, textPaint)

            // Total box (right column)
            val totalBoxRect = RectF(320f, y, contentRight, y + 55f)
            paint.color = primaryGreen
            canvas.drawRoundRect(totalBoxRect, 6f, 6f, paint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 10f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TOTAL AMOUNT", totalBoxRect.centerX(), y + 16f, textPaint)

            paint.color = white
            canvas.drawRect(322f, y + 22f, contentRight - 2f, y + 53f, paint)
            textPaint.color = primaryGreen
            textPaint.textSize = 20f
            canvas.drawText("\u20B9${formatAmount(total)}", totalBoxRect.centerX(), y + 44f, textPaint)

            // Amount in words
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = gray
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Amount in Words", 320f, y + 70f, textPaint)
            textPaint.color = darkText
            textPaint.textSize = 9.5f
            canvas.drawText("Rupees ${total.toInt()} Only", 320f, y + 83f, textPaint)

            // Signature
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = darkText
            textPaint.textSize = 9f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Authorized Signature", contentRight - 80f, y + 145f, textPaint)
            textPaint.typeface = Typeface.create("cursive", Typeface.ITALIC)
            textPaint.textSize = 18f
            textPaint.color = primaryGreen
            canvas.drawText("Madadwala", contentRight - 80f, y + 120f, textPaint)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 9f
            textPaint.color = darkText
            canvas.drawText("For Madadwala Technologies Pvt. Ltd.", contentRight - 80f, y + 133f, textPaint)

            // ============================================================
            // NEED HELP / QR / THANK YOU
            // ============================================================
            y += 165f
            val boxHeight = 90f
            val boxGap = 12f
            val boxWidth = (contentWidth - 2 * boxGap) / 3f

            // Need Help box
            val helpRect = RectF(marginX, y, marginX + boxWidth, y + boxHeight)
            drawDashedRoundRect(canvas, helpRect, borderGray)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = primaryGreen
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 11f
            canvas.drawText("Need Help?", helpRect.left + 14f, helpRect.top + 22f, textPaint)
            textPaint.color = darkText
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 9f
            drawMailIcon(canvas, helpRect.left + 14f, helpRect.top + 36f, 9f, gray)
            canvas.drawText("madadwala@gmail.com", helpRect.left + 30f, helpRect.top + 44f, textPaint)
            drawPhoneIcon(canvas, helpRect.left + 16f, helpRect.top + 56f, 8f, gray)
            canvas.drawText("+91 98793 38393", helpRect.left + 30f, helpRect.top + 61f, textPaint)

            // QR box
            val qrRect = RectF(marginX + boxWidth + boxGap, y, marginX + 2 * boxWidth + boxGap, y + boxHeight)
            drawDashedRoundRect(canvas, qrRect, borderGray)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = primaryGreen
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 9.5f
            canvas.drawText("Scan to Download Invoice", qrRect.centerX(), qrRect.top + 16f, textPaint)
            
            val qrContent = "https://madadwala.com/invoice/${booking._id}"
            val qrBitmap = generateQrCode(qrContent, 200) // Higher resolution matrix
            if (qrBitmap != null) {
                val destRect = RectF(qrRect.centerX() - 22f, qrRect.top + 22f, qrRect.centerX() + 22f, qrRect.top + 66f)
                canvas.drawBitmap(qrBitmap, null, destRect, paint)
            } else {
                drawFakeQr(canvas, qrRect.centerX() - 22f, qrRect.top + 22f, 44f)
            }

            // Thank you box
            val thanksRect = RectF(marginX + 2 * (boxWidth + boxGap), y, contentRight, y + boxHeight)
            drawDashedRoundRect(canvas, thanksRect, borderGray)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = primaryGreen
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 10.5f
            canvas.drawText("Thank you for choosing", thanksRect.centerX(), thanksRect.top + 22f, textPaint)
            canvas.drawText("Madadwala!", thanksRect.centerX(), thanksRect.top + 36f, textPaint)
            for (i in 0 until 5) {
                drawStar(canvas, thanksRect.centerX() - 44f + i * 20f, thanksRect.top + 52f, 6f, goldStar)
            }
            textPaint.color = gray
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 8f
            canvas.drawText("We appreciate your feedback!", thanksRect.centerX(), thanksRect.top + 72f, textPaint)

            // ============================================================
            // FOOTER
            // ============================================================
            val footerHeight = 45f
            val footerTop = pageHeight - footerHeight
            paint.color = primaryGreen
            canvas.drawRect(0f, footerTop, pageWidth.toFloat(), pageHeight.toFloat(), paint)

            textPaint.color = white
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 9f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Safe & Verified Professionals    |    Secure Payments    |    On-time Service", pageWidth / 2f, footerTop + 18f, textPaint)
            canvas.drawText("www.madadwala.com", pageWidth / 2f, footerTop + 33f, textPaint)

            pdfDocument.finishPage(page)

            savePdf(context, pdfDocument, booking)
        }
    }

    // ================================================================
    // SAVING LOGIC (unchanged behaviour from the original implementation)
    // ================================================================
    private suspend fun savePdf(context: Context, pdfDocument: PdfDocument, booking: BookingResponse) {
        val fileName = "Invoice_${booking._id.takeLast(6)}.pdf"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Invoice saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
                    }
                } else {
                    throw Exception("Failed to create MediaStore entry")
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Invoice downloaded to Downloads: $fileName", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            pdfDocument.close()
        }
    }

    // ================================================================
    // DRAWING HELPERS
    // ================================================================

    private fun drawServiceDetailRow(
        canvas: Canvas, x: Float, y: Float, endX: Float, label: String, value: String,
        textPaint: Paint, paint: Paint, isStatus: Boolean,
        primaryGreen: Int, gray: Int, darkText: Int, borderGray: Int
    ) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRect(x, y, endX, y + 22f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = borderGray
        canvas.drawRect(x, y, endX, y + 22f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = gray
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(label, x + 10f, y + 14f, textPaint)

        textPaint.color = if (isStatus) primaryGreen else darkText
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 160f, y + 14f, textPaint)
    }

    private fun drawDashedRoundRect(canvas: Canvas, rect: RectF, borderColor: Int) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(rect, 8f, 8f, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.color = borderColor
        strokePaint.strokeWidth = 1f
        strokePaint.pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
        canvas.drawRoundRect(rect, 8f, 8f, strokePaint)
    }

    private fun generateQrCode(content: String, size: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun drawFakeQr(canvas: Canvas, x: Float, y: Float, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawRect(x, y, x + size, y + size, paint)
        paint.color = Color.WHITE
        canvas.drawRect(x + 3f, y + 3f, x + size - 3f, y + size - 3f, paint)
        paint.color = Color.BLACK
        val block = size / 8f
        canvas.drawRect(x + block, y + block, x + block * 3, y + block * 3, paint)
        canvas.drawRect(x + size - block * 3, y + block, x + size - block, y + block * 3, paint)
        canvas.drawRect(x + block, y + size - block * 3, x + block * 3, y + size - block, paint)
        canvas.drawRect(x + block * 4, y + block * 4, x + block * 5, y + block * 5, paint)
    }

    private fun drawPhoneIcon(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.style = Paint.Style.FILL
        val path = Path()
        path.moveTo(x, y - size * 0.6f)
        path.lineTo(x + size * 0.35f, y - size * 0.9f)
        path.lineTo(x + size * 0.7f, y - size * 0.55f)
        path.lineTo(x + size * 0.5f, y - size * 0.3f)
        path.quadTo(x + size * 0.6f, y + size * 0.1f, x + size, y + size * 0.3f)
        path.lineTo(x + size * 0.75f, y + size * 0.65f)
        path.quadTo(x + size * 0.1f, y + size * 0.4f, x - size * 0.1f, y - size * 0.3f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawLocationIcon(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x + size * 0.5f, y, size * 0.5f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(x + size * 0.5f, y, size * 0.2f, paint)
        paint.color = color
        val path = Path()
        path.moveTo(x, y + size * 0.3f)
        path.lineTo(x + size, y + size * 0.3f)
        path.lineTo(x + size * 0.5f, y + size * 1.3f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawMailIcon(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = color
        val rect = RectF(x, y - size * 0.35f, x + size * 1.4f, y + size * 0.35f)
        canvas.drawRoundRect(rect, 1f, 1f, paint)
        val path = Path()
        path.moveTo(rect.left, rect.top)
        path.lineTo(rect.centerX(), rect.centerY())
        path.lineTo(rect.right, rect.top)
        canvas.drawPath(path, paint)
    }

    private fun drawCheckBadge(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = color
        canvas.drawCircle(cx, cy, radius, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        strokePaint.color = Color.WHITE
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        val path = Path()
        path.moveTo(cx - radius * 0.5f, cy)
        path.lineTo(cx - radius * 0.1f, cy + radius * 0.4f)
        path.lineTo(cx + radius * 0.5f, cy - radius * 0.4f)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        paint.color = color
        val path = Path()
        val outerRadius = radius
        val innerRadius = radius * 0.45f
        for (i in 0 until 10) {
            val angle = Math.PI / 5 * i - Math.PI / 2
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val px = cx + (r * Math.cos(angle)).toFloat()
            val py = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private suspend fun getBitmapFromUrl(context: Context, url: String): Bitmap? {
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(Size.ORIGINAL) // Fetch high-res image
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: Date()
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            dateStr.take(10)
        }
    }

    private fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "%,.2f", amount)
    }
}