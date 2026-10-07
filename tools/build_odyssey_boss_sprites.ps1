param(
    [Parameter(Mandatory = $true)]
    [string] $ModRoot
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if (-not ("ChiefNavigator.OdysseyBossSpriteBuilder" -as [type])) {
    Add-Type -ReferencedAssemblies "System.Drawing" -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

namespace ChiefNavigator {
    public static class OdysseyBossSpriteBuilder {
        private static float Clamp(float value) {
            return Math.Max(0f, Math.Min(1f, value));
        }

        private static byte ToByte(float value) {
            return (byte)Math.Max(0, Math.Min(255, (int)Math.Round(value)));
        }

        private static Bitmap RemoveMagentaKey(Bitmap source) {
            Bitmap result = new Bitmap(source.Width, source.Height, PixelFormat.Format32bppArgb);
            using (Graphics graphics = Graphics.FromImage(result)) {
                graphics.CompositingMode = CompositingMode.SourceCopy;
                graphics.DrawImageUnscaled(source, 0, 0);
            }

            Rectangle rect = new Rectangle(0, 0, result.Width, result.Height);
            BitmapData data = result.LockBits(rect, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
            int bytes = Math.Abs(data.Stride) * data.Height;
            byte[] pixels = new byte[bytes];
            Marshal.Copy(data.Scan0, pixels, 0, bytes);

            for (int y = 0; y < data.Height; y++) {
                int row = y * data.Stride;
                for (int x = 0; x < data.Width; x++) {
                    int i = row + x * 4;
                    float b = pixels[i];
                    float g = pixels[i + 1];
                    float r = pixels[i + 2];
                    float opacity = 1f - Clamp((Math.Min(r, b) - g - 15f) / 180f);
                    if (opacity <= 0.08f) {
                        pixels[i] = pixels[i + 1] = pixels[i + 2] = pixels[i + 3] = 0;
                    } else {
                        float keyShare = 1f - opacity;
                        float cleanB = (b - 255f * keyShare) / opacity;
                        float cleanG = g / opacity;
                        float cleanR = (r - 255f * keyShare) / opacity;
                        if (opacity < 0.75f) {
                            cleanG = Math.Min(cleanG, Math.Max(cleanR, cleanB) + 20f);
                        }
                        pixels[i] = ToByte(cleanB);
                        pixels[i + 1] = ToByte(cleanG);
                        pixels[i + 2] = ToByte(cleanR);
                        pixels[i + 3] = ToByte(255f * opacity);
                    }
                }
            }

            Marshal.Copy(pixels, 0, data.Scan0, bytes);
            result.UnlockBits(data);
            return result;
        }

        private static Rectangle AlphaBounds(Bitmap source) {
            int minX = source.Width;
            int minY = source.Height;
            int maxX = -1;
            int maxY = -1;
            for (int y = 0; y < source.Height; y++) {
                for (int x = 0; x < source.Width; x++) {
                    if (source.GetPixel(x, y).A <= 16) continue;
                    minX = Math.Min(minX, x);
                    minY = Math.Min(minY, y);
                    maxX = Math.Max(maxX, x);
                    maxY = Math.Max(maxY, y);
                }
            }
            if (maxX < minX || maxY < minY) {
                throw new InvalidOperationException("No opaque sprite pixels found.");
            }
            return Rectangle.FromLTRB(minX, minY, maxX + 1, maxY + 1);
        }

        public static void Build(
                string inputPath,
                string outputPath,
                int canvasWidth,
                int canvasHeight,
                int margin) {
            using (Bitmap source = new Bitmap(inputPath))
            using (Bitmap keyed = RemoveMagentaKey(source))
            using (Bitmap output = new Bitmap(canvasWidth, canvasHeight, PixelFormat.Format32bppArgb)) {
                Rectangle bounds = AlphaBounds(keyed);
                float scale = Math.Min(
                        (canvasWidth - margin * 2f) / bounds.Width,
                        (canvasHeight - margin * 2f) / bounds.Height);
                int width = Math.Max(1, (int)Math.Round(bounds.Width * scale));
                int height = Math.Max(1, (int)Math.Round(bounds.Height * scale));
                int x = (canvasWidth - width) / 2;
                int y = (canvasHeight - height) / 2;

                using (Graphics graphics = Graphics.FromImage(output)) {
                    graphics.Clear(Color.Transparent);
                    graphics.CompositingMode = CompositingMode.SourceCopy;
                    graphics.CompositingQuality = CompositingQuality.HighQuality;
                    graphics.InterpolationMode = InterpolationMode.HighQualityBicubic;
                    graphics.PixelOffsetMode = PixelOffsetMode.HighQuality;
                    graphics.SmoothingMode = SmoothingMode.HighQuality;
                    graphics.DrawImage(
                            keyed,
                            new Rectangle(x, y, width, height),
                            bounds,
                            GraphicsUnit.Pixel);
                }
                output.Save(outputPath, ImageFormat.Png);
            }
        }

        public static void Scale(
                string inputPath,
                string outputPath,
                int canvasWidth,
                int canvasHeight) {
            using (Bitmap source = new Bitmap(inputPath))
            using (Bitmap output = new Bitmap(
                    canvasWidth, canvasHeight, PixelFormat.Format32bppArgb)) {
                using (Graphics graphics = Graphics.FromImage(output)) {
                    graphics.Clear(Color.Transparent);
                    graphics.CompositingMode = CompositingMode.SourceCopy;
                    graphics.CompositingQuality = CompositingQuality.HighQuality;
                    graphics.InterpolationMode = InterpolationMode.HighQualityBicubic;
                    graphics.PixelOffsetMode = PixelOffsetMode.HighQuality;
                    graphics.SmoothingMode = SmoothingMode.HighQuality;
                    graphics.DrawImage(
                            source,
                            new Rectangle(0, 0, canvasWidth, canvasHeight),
                            new Rectangle(0, 0, source.Width, source.Height),
                            GraphicsUnit.Pixel);
                }
                output.Save(outputPath, ImageFormat.Png);
            }
        }
    }
}
'@
}

$root = [System.IO.Path]::GetFullPath($ModRoot)
$source = Join-Path $root "graphics\source"
$output = Join-Path $root "graphics\ships\odyssey_bosses"
New-Item -ItemType Directory -Force -Path $output | Out-Null

[ChiefNavigator.OdysseyBossSpriteBuilder]::Build(
    (Join-Path $source "chief_navigator_scylla_chroma_v1.png"),
    (Join-Path $output "chief_navigator_scylla_v1.png"),
    380,
    370,
    5)
[ChiefNavigator.OdysseyBossSpriteBuilder]::Scale(
    (Join-Path $output "chief_navigator_scylla_v1.png"),
    (Join-Path $output "chief_navigator_scylla_v2.png"),
    570,
    555)
[ChiefNavigator.OdysseyBossSpriteBuilder]::Build(
    (Join-Path $source "chief_navigator_charybdis_chroma_v1.png"),
    (Join-Path $output "chief_navigator_charybdis_v1.png"),
    559,
    413,
    7)

Write-Host "ODYSSEY BOSS SPRITES OK -> $output"
