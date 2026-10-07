param(
    [Parameter(Mandatory = $true)]
    [string] $BaseFramePath,

    [Parameter(Mandatory = $true)]
    [string] $LeftSegmentChromaPath,

    [Parameter(Mandatory = $true)]
    [string] $RightSegmentChromaPath,

    [Parameter(Mandatory = $true)]
    [string] $LeftSegmentOutputPath,

    [Parameter(Mandatory = $true)]
    [string] $RightSegmentOutputPath,

    [Parameter(Mandatory = $true)]
    [string] $OutputPath
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if (-not ("ChiefNavigator.DriftingWallEndBuilder" -as [type])) {
    Add-Type -ReferencedAssemblies "System.Drawing" -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

namespace ChiefNavigator {
    public static class DriftingWallEndBuilder {
        private static Bitmap ToArgb(Bitmap source) {
            Bitmap result = new Bitmap(source.Width, source.Height, PixelFormat.Format32bppArgb);
            using (Graphics graphics = Graphics.FromImage(result)) {
                graphics.CompositingMode = CompositingMode.SourceCopy;
                graphics.DrawImageUnscaled(source, 0, 0);
            }
            return result;
        }

        // Removes the generated #ff00ff plate and reconstructs partially
        // antialiased edge colors before the wrecked ends are resized.
        private static Bitmap RemoveMagentaKey(Bitmap source) {
            Bitmap result = ToArgb(source);
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
                    float magentaDominance = Math.Min(r, b) - g;
                    // The generator's nominally flat plate varies from bright
                    // magenta to darker pink near the canvas edge. Its border
                    // still has far more red/blue than green, so key from that
                    // dominance rather than from distance to one exact RGB.
                    float opacity = 1f - Clamp((magentaDominance - 15f) / 180f);

                    if (opacity <= 0.08f) {
                        pixels[i] = 0;
                        pixels[i + 1] = 0;
                        pixels[i + 2] = 0;
                        pixels[i + 3] = 0;
                    } else {
                        // Undo the key color's contribution to edge pixels.
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

        private static Bitmap Resize(Bitmap source, int width, int height) {
            Bitmap result = new Bitmap(width, height, PixelFormat.Format32bppArgb);
            using (Graphics graphics = Graphics.FromImage(result)) {
                graphics.Clear(Color.Transparent);
                graphics.CompositingMode = CompositingMode.SourceCopy;
                graphics.CompositingQuality = CompositingQuality.HighQuality;
                graphics.InterpolationMode = InterpolationMode.HighQualityBicubic;
                graphics.PixelOffsetMode = PixelOffsetMode.HighQuality;
                graphics.SmoothingMode = SmoothingMode.HighQuality;
                graphics.DrawImage(source, new Rectangle(0, 0, width, height));
            }
            return result;
        }

        private static void DrawSegment(Graphics graphics, Bitmap segment, float x, float y, float angle) {
            graphics.TranslateTransform(x, y);
            graphics.RotateTransform(angle);
            graphics.DrawImage(segment, -390, -490, 780, 980);
            graphics.ResetTransform();
        }

        public static void Build(
                string basePath,
                string leftPath,
                string rightPath,
                string leftOutputPath,
                string rightOutputPath,
                string outputPath) {
            using (Bitmap baseSource = new Bitmap(basePath))
            using (Bitmap leftSource = new Bitmap(leftPath))
            using (Bitmap rightSource = new Bitmap(rightPath))
            using (Bitmap baseFrame = ToArgb(baseSource))
            using (Bitmap leftKeyed = RemoveMagentaKey(leftSource))
            using (Bitmap rightKeyed = RemoveMagentaKey(rightSource))
            using (Bitmap leftSegment = Resize(leftKeyed, 780, 980))
            using (Bitmap rightSegment = Resize(rightKeyed, 780, 980))
            using (Bitmap baseWorld = ToArgb(baseFrame)) {
                leftSegment.Save(leftOutputPath, ImageFormat.Png);
                rightSegment.Save(rightOutputPath, ImageFormat.Png);

                // v2 is pre-rotated for Starsector. Return it to the 4,500 x
                // 1,800 world-layout canvas, add one whole generated segment
                // at either end, then restore the tested combat orientation.
                baseWorld.RotateFlip(RotateFlipType.Rotate90FlipNone);
                using (Bitmap world = new Bitmap(5700, 1800, PixelFormat.Format32bppArgb))
                using (Graphics graphics = Graphics.FromImage(world)) {
                    graphics.Clear(Color.Transparent);
                    graphics.CompositingMode = CompositingMode.SourceOver;
                    graphics.CompositingQuality = CompositingQuality.HighQuality;
                    graphics.InterpolationMode = InterpolationMode.HighQualityBicubic;
                    graphics.PixelOffsetMode = PixelOffsetMode.HighQuality;
                    graphics.SmoothingMode = SmoothingMode.HighQuality;

                    // Continue the live frame's tested curve: the original
                    // outer live centers are +/-1620 at curves +/-9 degrees.
                    DrawSegment(graphics, leftSegment, 690f, 600f, 192f);
                    DrawSegment(graphics, rightSegment, 5010f, 600f, 168f);

                    // Seat the unchanged seven-live-segment v2 composite over
                    // the generated joining rails so the seam is deterministic.
                    graphics.DrawImageUnscaled(baseWorld, 600, 0);
                    world.RotateFlip(RotateFlipType.Rotate270FlipNone);
                    world.Save(outputPath, ImageFormat.Png);
                }
            }
        }

        private static float Clamp(float value) {
            return Math.Max(0f, Math.Min(1f, value));
        }

        private static byte ToByte(float value) {
            return (byte)Math.Max(0, Math.Min(255, (int)Math.Round(value)));
        }
    }
}
'@
}

$resolvedBase = (Resolve-Path -LiteralPath $BaseFramePath).Path
$resolvedLeft = (Resolve-Path -LiteralPath $LeftSegmentChromaPath).Path
$resolvedRight = (Resolve-Path -LiteralPath $RightSegmentChromaPath).Path
foreach ($path in @($LeftSegmentOutputPath, $RightSegmentOutputPath, $OutputPath)) {
    $directory = Split-Path -Parent $path
    if ($directory) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }
}
$resolvedLeftOutput = [System.IO.Path]::GetFullPath($LeftSegmentOutputPath)
$resolvedRightOutput = [System.IO.Path]::GetFullPath($RightSegmentOutputPath)
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)

[ChiefNavigator.DriftingWallEndBuilder]::Build(
    $resolvedBase,
    $resolvedLeft,
    $resolvedRight,
    $resolvedLeftOutput,
    $resolvedRightOutput,
    $resolvedOutput)

Write-Host "DRIFTING WALL LEFT SEGMENT OK -> $resolvedLeftOutput"
Write-Host "DRIFTING WALL RIGHT SEGMENT OK -> $resolvedRightOutput"
Write-Host "DRIFTING WALL DESTROYED ENDS OK -> $resolvedOutput"
