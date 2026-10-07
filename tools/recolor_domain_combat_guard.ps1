$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path -Parent $PSScriptRoot
$starsectorRoot = Split-Path (Split-Path $modRoot -Parent) -Parent
$coreGraphics = Join-Path $starsectorRoot "starsector-core\graphics\ships\derelict"
$outputRoot = Join-Path $modRoot "graphics\ships\domain_combat_guard"

$sources = [ordered]@{
    "warden"    = "derelict_warden.png"
    "defender"  = "derelict_defender.png"
    "picket"    = "derelict_picket.png"
    "sentry"    = "derelict_sentry.png"
    "bastillon" = "derelict_bastillon.png"
    "berserker" = "derelict_berserker.png"
    "rampart"   = "derelict_rampart.png"
    "guardian"  = "derelict_guardian.png"
}

New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null

$typeName = "ChiefNavigatorDomainCombatGuardRecolor"
if (-not ($typeName -as [type])) {
    Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

public static class ChiefNavigatorDomainCombatGuardRecolor
{
    private static double Clamp01(double value)
    {
        return Math.Max(0.0, Math.Min(1.0, value));
    }

    private static double SmoothStep(double edge0, double edge1, double value)
    {
        double t = Clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3.0 - 2.0 * t);
    }

    private static void RgbToHsl(byte red, byte green, byte blue,
            out double hue, out double saturation, out double lightness)
    {
        double r = red / 255.0;
        double g = green / 255.0;
        double b = blue / 255.0;
        double max = Math.Max(r, Math.Max(g, b));
        double min = Math.Min(r, Math.Min(g, b));
        double delta = max - min;
        lightness = (max + min) * 0.5;

        if (delta < 0.000001)
        {
            hue = 0.0;
            saturation = 0.0;
            return;
        }

        saturation = delta / (1.0 - Math.Abs(2.0 * lightness - 1.0));
        if (max == r)
            hue = 60.0 * (((g - b) / delta) % 6.0);
        else if (max == g)
            hue = 60.0 * (((b - r) / delta) + 2.0);
        else
            hue = 60.0 * (((r - g) / delta) + 4.0);

        if (hue < 0.0) hue += 360.0;
    }

    private static double HueToRgb(double p, double q, double t)
    {
        if (t < 0.0) t += 1.0;
        if (t > 1.0) t -= 1.0;
        if (t < 1.0 / 6.0) return p + (q - p) * 6.0 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6.0;
        return p;
    }

    private static void HslToRgb(double hue, double saturation,
            double lightness, out byte red, out byte green, out byte blue)
    {
        double h = hue / 360.0;
        double r;
        double g;
        double b;
        if (saturation < 0.000001)
        {
            r = g = b = lightness;
        }
        else
        {
            double q = lightness < 0.5
                    ? lightness * (1.0 + saturation)
                    : lightness + saturation - lightness * saturation;
            double p = 2.0 * lightness - q;
            r = HueToRgb(p, q, h + 1.0 / 3.0);
            g = HueToRgb(p, q, h);
            b = HueToRgb(p, q, h - 1.0 / 3.0);
        }

        red = (byte)Math.Round(Clamp01(r) * 255.0);
        green = (byte)Math.Round(Clamp01(g) * 255.0);
        blue = (byte)Math.Round(Clamp01(b) * 255.0);
    }

    public static void Recolor(string sourcePath, string outputPath)
    {
        using (Bitmap sourceFile = new Bitmap(sourcePath))
        using (Bitmap bitmap = new Bitmap(sourceFile.Width, sourceFile.Height,
                PixelFormat.Format32bppArgb))
        {
            // Preserve the source PNG's resolution metadata as well as its
            // pixel canvas. Starsector renders in pixels, but keeping both
            // values identical makes the derivative asset unambiguous in
            // image tools and prevents DPI-aware preview surprises.
            bitmap.SetResolution(sourceFile.HorizontalResolution,
                    sourceFile.VerticalResolution);

            Rectangle bounds = new Rectangle(0, 0, bitmap.Width, bitmap.Height);
            BitmapData data = bitmap.LockBits(bounds, ImageLockMode.ReadWrite,
                    PixelFormat.Format32bppArgb);
            try
            {
                int byteCount = Math.Abs(data.Stride) * data.Height;
                byte[] pixels = new byte[byteCount];
                Marshal.Copy(data.Scan0, pixels, 0, byteCount);

                for (int y = 0; y < data.Height; y++)
                {
                    int row = y * data.Stride;
                    for (int x = 0; x < data.Width; x++)
                    {
                        int index = row + x * 4;
                        // Copy the authoritative vanilla pixel directly.
                        // Graphics.DrawImage(), including DrawImageUnscaled(),
                        // can honor unusual PNG DPI metadata and resample some
                        // Starsector sprites inside an otherwise correct-size
                        // canvas. Direct pixel copying guarantees identical
                        // geometry, resolution, and alpha before recoloring.
                        Color source = sourceFile.GetPixel(x, y);
                        byte alpha = source.A;
                        pixels[index] = source.B;
                        pixels[index + 1] = source.G;
                        pixels[index + 2] = source.R;
                        pixels[index + 3] = alpha;
                        if (alpha == 0) continue;

                        byte blue = source.B;
                        byte green = source.G;
                        byte red = source.R;
                        double hue;
                        double saturation;
                        double lightness;
                        RgbToHsl(red, green, blue,
                                out hue, out saturation, out lightness);

                        // Select only the olive/green Explorarium armor.
                        // Amber hazard stripes, rust, exposed steel, engines,
                        // shadows, and all alpha values remain untouched.
                        double lowerHue = SmoothStep(58.0, 72.0, hue);
                        double upperHue = 1.0 - SmoothStep(102.0, 118.0, hue);
                        double chroma = SmoothStep(0.045, 0.15, saturation);
                        double amount = 0.68 * lowerHue * upperHue * chroma;
                        if (amount <= 0.001) continue;

                        // Ithaca's approved oxidized pale-aqua armor is near
                        // 184 degrees. Preserve the source pixel's luminance
                        // and texture while slightly tempering saturation.
                        double targetHue = 184.0;
                        double targetSaturation = Clamp01(
                                Math.Max(0.09, Math.Min(0.22,
                                saturation * 0.45 + 0.02)));
                        byte targetRed;
                        byte targetGreen;
                        byte targetBlue;
                        HslToRgb(targetHue, targetSaturation, lightness,
                                out targetRed, out targetGreen, out targetBlue);

                        pixels[index + 2] = (byte)Math.Round(
                                red + (targetRed - red) * amount);
                        pixels[index + 1] = (byte)Math.Round(
                                green + (targetGreen - green) * amount);
                        pixels[index] = (byte)Math.Round(
                                blue + (targetBlue - blue) * amount);
                    }
                }

                Marshal.Copy(pixels, 0, data.Scan0, byteCount);
            }
            finally
            {
                bitmap.UnlockBits(data);
            }

            bitmap.Save(outputPath, ImageFormat.Png);
        }
    }
}
'@
}

foreach ($entry in $sources.GetEnumerator()) {
    $sourcePath = Join-Path $coreGraphics $entry.Value
    $outputPath = Join-Path $outputRoot ("combat_guard_{0}.png" -f $entry.Key)
    if (-not (Test-Path -LiteralPath $sourcePath)) {
        throw "Vanilla Explorarium sprite not found: $sourcePath"
    }
    [ChiefNavigatorDomainCombatGuardRecolor]::Recolor($sourcePath, $outputPath)
    Write-Output "Built $outputPath"
}
