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
    Image(ExternalLink, null)
}

private var _ExternalLink: ImageVector? = null

public val ExternalLink: ImageVector
    get() {
        if (_ExternalLink != null) {
            return _ExternalLink!!
        }
        _ExternalLink = ImageVector.Builder(
            name = "ExternalLink",
            defaultWidth = 512.dp,
            defaultHeight = 512.dp,
            viewportWidth = 512f,
            viewportHeight = 512f
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
                moveTo(432f, 320f)
                horizontalLineTo(400f)
                arcToRelative(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = false, -16f, 16f)
                verticalLineTo(448f)
                horizontalLineTo(64f)
                verticalLineTo(128f)
                horizontalLineTo(208f)
                arcToRelative(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16f, -16f)
                verticalLineTo(80f)
                arcToRelative(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = false, -16f, -16f)
                horizontalLineTo(48f)
                arcTo(48f, 48f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 112f)
                verticalLineTo(464f)
                arcToRelative(48f, 48f, 0f, isMoreThanHalf = false, isPositiveArc = false, 48f, 48f)
                horizontalLineTo(400f)
                arcToRelative(48f, 48f, 0f, isMoreThanHalf = false, isPositiveArc = false, 48f, -48f)
                verticalLineTo(336f)
                arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = false, 432f, 320f)
                close()
                moveTo(488f, 0f)
                horizontalLineToRelative(-128f)
                curveToRelative(-21.37f, 0f, -32.05f, 25.91f, -17f, 41f)
                lineToRelative(35.73f, 35.73f)
                lineTo(135f, 320.37f)
                arcToRelative(24f, 24f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 34f)
                lineTo(157.67f, 377f)
                arcToRelative(24f, 24f, 0f, isMoreThanHalf = false, isPositiveArc = false, 34f, 0f)
                lineTo(435.28f, 133.32f)
                lineTo(471f, 169f)
                curveToRelative(15f, 15f, 41f, 4.5f, 41f, -17f)
                verticalLineTo(24f)
                arcTo(24f, 24f, 0f, isMoreThanHalf = false, isPositiveArc = false, 488f, 0f)
                close()
            }
        }.build()
        return _ExternalLink!!
    }

