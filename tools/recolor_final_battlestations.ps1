$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path -Parent $PSScriptRoot
$starsectorRoot = Split-Path (Split-Path $modRoot -Parent) -Parent
$sourceRoot = Join-Path $starsectorRoot "starsector-core\graphics\ships\stations"
$outputRoot = Join-Path $modRoot "graphics\ships\final_battlestation"

$sources = [ordered]@{
    "station_level2_lowtech.png" = "station_level2_lowtech_teal_v2.png"
    "station_armour3b.png" = "station_armour3b_teal_v1.png"
    "station_armour4b.png" = "station_armour4b_teal_v1.png"
    "station_armour2.png" = "station_armour2_teal_v1.png"
    "module_large1_lowtech.png" = "module_large1_lowtech_teal_v2.png"
    "module_large2_lowtech.png" = "module_large2_lowtech_teal_v1.png"
    "module_medium_pd1_lowtech.png" = "module_medium_pd1_lowtech_teal_v1.png"
    "module_citadel1_lowtech.png" = "module_citadel1_lowtech_teal_v1.png"
}

[System.IO.Directory]::CreateDirectory($outputRoot) | Out-Null

$typeName = "ChiefNavigatorFinalBattlestationRecolor"
if (-not ($typeName -as [type])) {
    Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

public static class ChiefNavigatorFinalBattlestationRecolor
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

    private static double WarmHueWeight(double hue)
    {
        double orange = SmoothStep(330.0, 350.0, hue)
                + (1.0 - SmoothStep(62.0, 82.0, hue));
        return Clamp01(orange);
    }

    public static string Recolor(string sourcePath, string outputPath)
    {
        using (Bitmap source = new Bitmap(sourcePath))
        using (Bitmap output = new Bitmap(source.Width, source.Height,
                PixelFormat.Format32bppArgb))
        {
            output.SetResolution(source.HorizontalResolution,
                    source.VerticalResolution);
            Rectangle bounds = new Rectangle(0, 0, output.Width, output.Height);
            BitmapData data = output.LockBits(bounds, ImageLockMode.ReadWrite,
                    PixelFormat.Format32bppArgb);
            long recolored = 0;
            long opaque = 0;
            try
            {
                int byteCount = Math.Abs(data.Stride) * data.Height;
                byte[] pixels = new byte[byteCount];
                for (int y = 0; y < source.Height; y++)
                {
                    int row = y * data.Stride;
                    for (int x = 0; x < source.Width; x++)
                    {
                        int index = row + x * 4;
                        Color original = source.GetPixel(x, y);
                        pixels[index] = original.B;
                        pixels[index + 1] = original.G;
                        pixels[index + 2] = original.R;
                        pixels[index + 3] = original.A;
                        if (original.A == 0) continue;
                        opaque++;

                        double hue;
                        double saturation;
                        double lightness;
                        RgbToHsl(original.R, original.G, original.B,
                                out hue, out saturation, out lightness);

                        // Preserve saturated hazard paint and red status marks,
                        // but shift the station's brown/tan armor and gently
                        // cool its neutral structural metal. This is the same
                        // luminance-preserving, material-selective treatment as
                        // the Ithaca Wall and Combat Guard recolors, never a
                        // runtime whole-sprite tint.
                        double neutral = 0.30
                                * (1.0 - SmoothStep(0.025, 0.15, saturation));
                        double warm = 0.78 * WarmHueWeight(hue)
                                * SmoothStep(0.018, 0.20, saturation)
                                * (1.0 - 0.70
                                        * SmoothStep(0.48, 0.78, saturation));
                        double material = Math.Max(neutral, warm);
                        double tonal = SmoothStep(0.025, 0.11, lightness)
                                * (1.0 - SmoothStep(0.93, 0.995, lightness));
                        double amount = material * tonal;
                        if (amount <= 0.002) continue;

                        double targetSaturation = Math.Max(0.11,
                                Math.Min(0.30, saturation * 0.45 + 0.105));
                        byte targetRed;
                        byte targetGreen;
                        byte targetBlue;
                        HslToRgb(184.0, targetSaturation, lightness,
                                out targetRed, out targetGreen, out targetBlue);
                        pixels[index + 2] = (byte)Math.Round(original.R
                                + (targetRed - original.R) * amount);
                        pixels[index + 1] = (byte)Math.Round(original.G
                                + (targetGreen - original.G) * amount);
                        pixels[index] = (byte)Math.Round(original.B
                                + (targetBlue - original.B) * amount);
                        recolored++;
                    }
                }
                Marshal.Copy(pixels, 0, data.Scan0, byteCount);
            }
            finally
            {
                output.UnlockBits(data);
            }
            output.Save(outputPath, ImageFormat.Png);
            return String.Format("{0}x{1}; opaque={2}; recolored={3}",
                    source.Width, source.Height, opaque, recolored);
        }
    }
}
'@
}

foreach ($entry in $sources.GetEnumerator()) {
    $sourcePath = Join-Path $sourceRoot $entry.Key
    $outputPath = Join-Path $outputRoot $entry.Value
    if (-not (Test-Path -LiteralPath $sourcePath)) {
        throw "Vanilla battlestation sprite not found: $sourcePath"
    }
    $result = [ChiefNavigatorFinalBattlestationRecolor]::Recolor(
            $sourcePath, $outputPath)
    Write-Output "Built $outputPath"
    Write-Output $result
}
