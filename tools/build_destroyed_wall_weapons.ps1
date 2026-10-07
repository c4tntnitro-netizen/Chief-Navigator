param(
    [Parameter(Mandatory = $true)]
    [string] $ModRoot
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if (-not ("ChiefNavigator.WallWeaponSpriteBuilder" -as [type])) {
    Add-Type -ReferencedAssemblies "System.Drawing" -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

namespace ChiefNavigator {
    public static class WallWeaponSpriteBuilder {
        private static Bitmap ToArgb(Bitmap source) {
            Bitmap result = new Bitmap(source.Width, source.Height, PixelFormat.Format32bppArgb);
            using (Graphics graphics = Graphics.FromImage(result)) {
                graphics.CompositingMode = CompositingMode.SourceCopy;
                graphics.DrawImageUnscaled(source, 0, 0);
            }
            return result;
        }

        private static float Clamp(float value) {
            return Math.Max(0f, Math.Min(1f, value));
        }

        private static byte ToByte(float value) {
            return (byte)Math.Max(0, Math.Min(255, (int)Math.Round(value)));
        }

        // The built-in generator returns a nearly-flat magenta plate. Keying
        // by magenta dominance also handles its slightly darkened canvas rim.
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

        public static void ResizeFile(string inputPath, string outputPath) {
            using (Bitmap source = new Bitmap(inputPath))
            using (Bitmap resized = Resize(source, 640, 640)) {
                resized.Save(outputPath, ImageFormat.Png);
            }
        }

        public static void KeyAndResize(string inputPath, string outputPath) {
            using (Bitmap source = new Bitmap(inputPath))
            using (Bitmap keyed = RemoveMagentaKey(source))
            using (Bitmap resized = Resize(keyed, 640, 640)) {
                resized.Save(outputPath, ImageFormat.Png);
            }
        }

        private static Rectangle FindAlphaBounds(Bitmap source) {
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

        // Seats the generated single-barrel slab on the old Damocles breech
        // while keeping the rest of the 640-square animation layer empty.
        public static void KeyCropAndFit(
                string inputPath,
                string outputPath,
                int x,
                int y,
                int width,
                int height) {
            using (Bitmap source = new Bitmap(inputPath))
            using (Bitmap keyed = RemoveMagentaKey(source))
            using (Bitmap result = new Bitmap(640, 640, PixelFormat.Format32bppArgb)) {
                Rectangle bounds = FindAlphaBounds(keyed);
                using (Graphics graphics = Graphics.FromImage(result)) {
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
                result.Save(outputPath, ImageFormat.Png);
            }
        }

        public static void CompositeBelow(
                string lowerPath,
                string upperPath,
                string outputPath) {
            using (Bitmap lower = new Bitmap(lowerPath))
            using (Bitmap upper = new Bitmap(upperPath))
            using (Bitmap result = new Bitmap(640, 640, PixelFormat.Format32bppArgb))
            using (Graphics graphics = Graphics.FromImage(result)) {
                graphics.Clear(Color.Transparent);
                graphics.CompositingMode = CompositingMode.SourceOver;
                graphics.DrawImageUnscaled(lower, 0, 0);
                graphics.DrawImageUnscaled(upper, 0, 0);
                result.Save(outputPath, ImageFormat.Png);
            }
        }
    }
}
'@
}

$root = [System.IO.Path]::GetFullPath($ModRoot)
$weapons = Join-Path $root "graphics\weapons"
$source = Join-Path $root "graphics\source"

$resizeJobs = @(
    @("chief_navigator_damocles_base_v1.png", "chief_navigator_damocles_base_v2.png"),
    @("chief_navigator_damocles_barrels_v1.png", "chief_navigator_damocles_barrels_v2.png"),
    @("chief_navigator_damocles_v1.png", "chief_navigator_damocles_v2.png"),
    @("chief_navigator_metalstorm_base_v3.png", "chief_navigator_metalstorm_base_v4.png"),
    @("chief_navigator_metalstorm_v2.png", "chief_navigator_metalstorm_v3.png")
)
foreach ($job in $resizeJobs) {
    [ChiefNavigator.WallWeaponSpriteBuilder]::ResizeFile(
        (Join-Path $weapons $job[0]),
        (Join-Path $weapons $job[1]))
}

$destroyedJobs = @(
    @("chief_navigator_destroyed_naval_gun_chroma_v1.png", "chief_navigator_destroyed_naval_gun_v1.png"),
    @("chief_navigator_destroyed_damocles_chroma_v2.png", "chief_navigator_destroyed_damocles_v2.png"),
    @("chief_navigator_destroyed_metalstorm_chroma_v1.png", "chief_navigator_destroyed_metalstorm_v1.png")
)
foreach ($job in $destroyedJobs) {
    [ChiefNavigator.WallWeaponSpriteBuilder]::KeyAndResize(
        (Join-Path $source $job[0]),
        (Join-Path $weapons $job[1]))
}

[ChiefNavigator.WallWeaponSpriteBuilder]::KeyCropAndFit(
    (Join-Path $source "chief_navigator_damocles_single_barrel_chroma_v1.png"),
    (Join-Path $weapons "chief_navigator_damocles_barrel_v3.png"),
    140,
    5,
    360,
    255)
[ChiefNavigator.WallWeaponSpriteBuilder]::CompositeBelow(
    (Join-Path $weapons "chief_navigator_damocles_barrel_v3.png"),
    (Join-Path $weapons "chief_navigator_damocles_base_v2.png"),
    (Join-Path $weapons "chief_navigator_damocles_v3.png"))

Write-Host "WALL WEAPON SPRITES OK -> $weapons"
