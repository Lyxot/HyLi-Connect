package xyz.hyli.connect.ui.icon

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
private fun VectorPreview() {
    Image(AndroidPhone, null)
}

private var _AndroidPhone: ImageVector? = null

public val AndroidPhone: ImageVector
    get() {
        if (_AndroidPhone != null) {
            return _AndroidPhone!!
        }
        _AndroidPhone = ImageVector.Builder(
            name = "AndroidPhone",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(11f, 4f)
                curveTo(9.3555f, 4f, 8f, 5.3555f, 8f, 7f)
                lineTo(8f, 25f)
                curveTo(8f, 26.6445f, 9.3555f, 28f, 11f, 28f)
                lineTo(21f, 28f)
                curveTo(22.6445f, 28f, 24f, 26.6445f, 24f, 25f)
                lineTo(24f, 7f)
                curveTo(24f, 5.3555f, 22.6445f, 4f, 21f, 4f)
                close()
                moveTo(11f, 6f)
                lineTo(21f, 6f)
                curveTo(21.5547f, 6f, 22f, 6.4453f, 22f, 7f)
                lineTo(22f, 25f)
                curveTo(22f, 25.5547f, 21.5547f, 26f, 21f, 26f)
                lineTo(11f, 26f)
                curveTo(10.4453f, 26f, 10f, 25.5547f, 10f, 25f)
                lineTo(10f, 7f)
                curveTo(10f, 6.4453f, 10.4453f, 6f, 11f, 6f)
                close()
                moveTo(16f, 23f)
                curveTo(15.4492f, 23f, 15f, 23.4492f, 15f, 24f)
                curveTo(15f, 24.5508f, 15.4492f, 25f, 16f, 25f)
                curveTo(16.5508f, 25f, 17f, 24.5508f, 17f, 24f)
                curveTo(17f, 23.4492f, 16.5508f, 23f, 16f, 23f)
                close()
            }
        }.build()
        return _AndroidPhone!!
    }

