package com.example.taxi_application.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.taxi_application.data.model.LatLng
import com.example.taxi_application.data.model.NearbyDriver
import com.example.taxi_application.data.model.TariffType
import com.example.taxi_application.ui.theme.*
import kotlin.math.*

/**
 * Кастомная карта Глазова на Canvas.
 * Отображает улицы, районы, маркеры водителей и текущее местоположение.
 * Поддерживает жесты: тап для выбора точки, pinch для масштабирования.
 *
 * NOTE: Для продакшна замените на Yandex MapKit SDK или Mapbox SDK.
 * Инструкция по интеграции — в README.md
 */
@Composable
fun GlazovMapView(
    modifier: Modifier = Modifier,
    center: LatLng,
    currentLocation: LatLng?,
    nearbyDrivers: List<NearbyDriver>,
    onMapTap: (LatLng) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(Offset(1f, 1f)) }

    // Конвертация координат в пиксели на Canvas
    fun latLngToPixel(latLng: LatLng, canvasW: Float, canvasH: Float): Offset {
        val centerLat = center.lat
        val centerLng = center.lng
        val latRange = 0.04 / scale
        val lngRange = 0.06 / scale
        val x = (canvasW / 2f + ((latLng.lng - centerLng) / lngRange * canvasW / 2f) + offsetX).toFloat()
        val y = (canvasH / 2f - ((latLng.lat - centerLat) / latRange * canvasH / 2f) + offsetY).toFloat()
        return Offset(x, y)
    }

    fun pixelToLatLng(pixel: Offset, canvasW: Float, canvasH: Float): LatLng {
        val centerLat = center.lat
        val centerLng = center.lng
        val latRange = 0.04 / scale
        val lngRange = 0.06 / scale
        val lng = centerLng + ((pixel.x - offsetX - canvasW / 2f) / (canvasW / 2f)) * lngRange / 2f
        val lat = centerLat - ((pixel.y - offsetY - canvasH / 2f) / (canvasH / 2f)) * latRange / 2f
        return LatLng(lat, lng)
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .pointerInput(center) {
                detectTapGestures { tapOffset ->
                    val latLng = pixelToLatLng(tapOffset, canvasSize.x, canvasSize.y)
                    onMapTap(latLng)
                }
            }
    ) {
        canvasSize = Offset(size.width, size.height)
        val w = size.width
        val h = size.height

        // ─── Фон карты ────────────────────────────────────────────────────────
        drawRect(color = Color(0xFFE8F0E8))

        // ─── Кварталы / блоки ─────────────────────────────────────────────────
        drawGlazovBlocks(w, h, scale, offsetX, offsetY, center)

        // ─── Основные улицы Глазова ───────────────────────────────────────────
        drawGlazovStreets(w, h, scale, offsetX, offsetY, center, textMeasurer)

        // ─── Парки / зелёные зоны ─────────────────────────────────────────────
        drawGreenZones(w, h, scale, offsetX, offsetY, center)

        // ─── Водоёмы (пруд Глазовский) ────────────────────────────────────────
        drawWaterBodies(w, h, scale, offsetX, offsetY, center)

        // ─── Маркеры водителей ────────────────────────────────────────────────
        nearbyDrivers.forEach { driver ->
            val pos = latLngToPixel(LatLng(driver.lat, driver.lng), w, h)
            if (pos.x in 0f..w && pos.y in 0f..h) {
                drawDriverMarker(pos, driver.tariff)
            }
        }

        // ─── Текущее местоположение ───────────────────────────────────────────
        currentLocation?.let { loc ->
            val pos = latLngToPixel(loc, w, h)
            // Пульсирующий круг
            drawCircle(
                color = Color(0x330066FF),
                radius = 40f * scale.coerceAtMost(2f),
                center = pos
            )
            drawCircle(
                color = Color(0x660066FF),
                radius = 24f * scale.coerceAtMost(2f),
                center = pos
            )
            drawCircle(
                color = Color(0xFF0066FF),
                radius = 12f * scale.coerceAtMost(2f),
                center = pos
            )
            drawCircle(
                color = Color.White,
                radius = 6f * scale.coerceAtMost(2f),
                center = pos
            )
        }

        // ─── Центр карты (перекрестие) ────────────────────────────────────────
        val cx = w / 2f + offsetX
        val cy = h / 2f + offsetY
        drawLine(Color(0x40000000), Offset(cx - 20, cy), Offset(cx + 20, cy), 1.5f)
        drawLine(Color(0x40000000), Offset(cx, cy - 20), Offset(cx, cy + 20), 1.5f)
    }
}

private fun DrawScope.drawGlazovBlocks(
    w: Float, h: Float, scale: Float, offsetX: Float, offsetY: Float, center: LatLng
) {
    val blockColor = Color(0xFFD4C9A8)
    val blockStroke = Color(0xFFBBAA88)

    // Сетка кварталов (упрощённая)
    val blockSize = 60f * scale
    val startX = (w / 2f + offsetX) % blockSize
    val startY = (h / 2f + offsetY) % blockSize

    var x = startX - blockSize
    while (x < w + blockSize) {
        var y = startY - blockSize
        while (y < h + blockSize) {
            drawRect(
                color = blockColor,
                topLeft = Offset(x + 4, y + 4),
                size = Size(blockSize - 8, blockSize - 8)
            )
            y += blockSize
        }
        x += blockSize
    }
}

private fun DrawScope.drawGlazovStreets(
    w: Float, h: Float, scale: Float, offsetX: Float, offsetY: Float,
    center: LatLng, textMeasurer: TextMeasurer
) {
    // Главные улицы Глазова (координаты относительно центра)
    val streets = listOf(
        // пр. Ленина (горизонтальная главная)
        Triple(
            LatLng(58.1387, 52.630), LatLng(58.1387, 52.690),
            StreetStyle("пр. Ленина", 8f, Color(0xFFFFFFFF), Color(0xFFCCCCCC))
        ),
        // ул. Кирова
        Triple(
            LatLng(58.142, 52.640), LatLng(58.142, 52.680),
            StreetStyle("ул. Кирова", 6f, Color(0xFFFFFFFF), Color(0xFFDDDDDD))
        ),
        // ул. Луначарского
        Triple(
            LatLng(58.135, 52.645), LatLng(58.135, 52.675),
            StreetStyle("ул. Луначарского", 5f, Color(0xFFF5F5F5), Color(0xFFDDDDDD))
        ),
        // ул. Короленко (вертикальная)
        Triple(
            LatLng(58.130, 52.658), LatLng(58.148, 52.658),
            StreetStyle("ул. Короленко", 6f, Color(0xFFFFFFFF), Color(0xFFCCCCCC))
        ),
        // ул. Драгунова
        Triple(
            LatLng(58.132, 52.650), LatLng(58.145, 52.665),
            StreetStyle("ул. Драгунова", 5f, Color(0xFFF5F5F5), Color(0xFFDDDDDD))
        ),
        // ул. Сибирская
        Triple(
            LatLng(58.128, 52.640), LatLng(58.128, 52.680),
            StreetStyle("ул. Сибирская", 5f, Color(0xFFF5F5F5), Color(0xFFDDDDDD))
        ),
        // ул. Революции
        Triple(
            LatLng(58.138, 52.635), LatLng(58.150, 52.635),
            StreetStyle("ул. Революции", 5f, Color(0xFFF5F5F5), Color(0xFFDDDDDD))
        ),
        // ул. Советская
        Triple(
            LatLng(58.140, 52.670), LatLng(58.155, 52.670),
            StreetStyle("ул. Советская", 5f, Color(0xFFF5F5F5), Color(0xFFDDDDDD))
        ),
    )

    streets.forEach { (from, to, style) ->
        val fromPx = latLngToPixelStatic(from, w, h, scale, offsetX, offsetY, center)
        val toPx = latLngToPixelStatic(to, w, h, scale, offsetX, offsetY, center)

        // Обводка улицы
        drawLine(
            color = style.borderColor,
            start = fromPx,
            end = toPx,
            strokeWidth = (style.width + 2f) * scale.coerceIn(0.5f, 3f),
            cap = StrokeCap.Round
        )
        // Полотно улицы
        drawLine(
            color = style.color,
            start = fromPx,
            end = toPx,
            strokeWidth = style.width * scale.coerceIn(0.5f, 3f),
            cap = StrokeCap.Round
        )

        // Подпись улицы (при достаточном масштабе)
        if (scale > 0.8f) {
            val midX = (fromPx.x + toPx.x) / 2f
            val midY = (fromPx.y + toPx.y) / 2f
            val measured = textMeasurer.measure(
                style.name,
                style = TextStyle(
                    fontSize = (9 * scale.coerceIn(0.8f, 1.5f)).sp,
                    color = Color(0xFF555555),
                    fontWeight = FontWeight.Medium
                )
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(midX - measured.size.width / 2f, midY - measured.size.height / 2f)
            )
        }
    }
}

private fun DrawScope.drawGreenZones(
    w: Float, h: Float, scale: Float, offsetX: Float, offsetY: Float, center: LatLng
) {
    // Парк культуры и отдыха (центральный)
    val parkCenter = latLngToPixelStatic(LatLng(58.1395, 52.6620), w, h, scale, offsetX, offsetY, center)
    drawCircle(
        color = Color(0xFF8BC34A).copy(alpha = 0.6f),
        radius = 35f * scale.coerceIn(0.5f, 2f),
        center = parkCenter
    )

    // Сквер у администрации
    val squerCenter = latLngToPixelStatic(LatLng(58.1380, 52.6560), w, h, scale, offsetX, offsetY, center)
    drawCircle(
        color = Color(0xFF8BC34A).copy(alpha = 0.5f),
        radius = 20f * scale.coerceIn(0.5f, 2f),
        center = squerCenter
    )
}

private fun DrawScope.drawWaterBodies(
    w: Float, h: Float, scale: Float, offsetX: Float, offsetY: Float, center: LatLng
) {
    // Чепца (река)
    val riverPoints = listOf(
        LatLng(58.122, 52.630), LatLng(58.124, 52.645),
        LatLng(58.126, 52.660), LatLng(58.125, 52.675),
        LatLng(58.127, 52.690)
    )
    val path = Path()
    riverPoints.forEachIndexed { i, point ->
        val px = latLngToPixelStatic(point, w, h, scale, offsetX, offsetY, center)
        if (i == 0) path.moveTo(px.x, px.y) else path.lineTo(px.x, px.y)
    }
    drawPath(
        path = path,
        color = Color(0xFF64B5F6).copy(alpha = 0.7f),
        style = Stroke(width = 8f * scale.coerceIn(0.5f, 2f), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawDriverMarker(pos: Offset, tariffName: String) {
    val color = when (tariffName) {
        TariffType.ECONOMY.name -> Color(0xFF4CAF50)
        TariffType.COMFORT.name -> Color(0xFF2196F3)
        TariffType.BUSINESS.name -> Color(0xFF9C27B0)
        else -> Color(0xFF4CAF50)
    }
    // Тень
    drawCircle(color = Color(0x30000000), radius = 18f, center = Offset(pos.x + 2f, pos.y + 2f))
    // Фон маркера
    drawCircle(color = color, radius = 16f, center = pos)
    drawCircle(color = Color.White, radius = 10f, center = pos)
    // Иконка машины (упрощённо — прямоугольник)
    drawRect(
        color = color,
        topLeft = Offset(pos.x - 6f, pos.y - 4f),
        size = Size(12f, 8f)
    )
}

private fun latLngToPixelStatic(
    latLng: LatLng, w: Float, h: Float,
    scale: Float, offsetX: Float, offsetY: Float, center: LatLng
): Offset {
    val latRange = 0.04 / scale
    val lngRange = 0.06 / scale
    val x = (w / 2f + ((latLng.lng - center.lng) / lngRange * w / 2f) + offsetX).toFloat()
    val y = (h / 2f - ((latLng.lat - center.lat) / latRange * h / 2f) + offsetY).toFloat()
    return Offset(x, y)
}

private data class StreetStyle(
    val name: String,
    val width: Float,
    val color: Color,
    val borderColor: Color
)
