$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if (-not ("ChiefNavigatorFobGlowBuilder" -as [type])) {
    Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class ChiefNavigatorFobGlowBuilder {
    private static double Clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }

    private static double GetHue(int red, int green, int blue) {
        double r = red / 255.0;
        double g = green / 255.0;
        double b = blue / 255.0;
        double max = Math.Max(r, Math.Max(g, b));
        double min = Math.Min(r, Math.Min(g, b));
        double delta = max - min;
        if (delta <= 0.00001) return 0.0;

        double hue;
        if (max == r) {
            hue = 60.0 * (((g - b) / delta) % 6.0);
        } else if (max == g) {
            hue = 60.0 * (((b - r) / delta) + 2.0);
        } else {
            hue = 60.0 * (((r - g) / delta) + 4.0);
        }
        if (hue < 0.0) hue += 360.0;
        return hue;
    }

    private static Color GetReferenceGlowColor(string referencePath) {
        Bitmap reference = new Bitmap(referencePath);
        try {
            double totalWeight = 0.0;
            double red = 0.0;
            double green = 0.0;
            double blue = 0.0;
            for (int y = 0; y < reference.Height; y++) {
                for (int x = 0; x < reference.Width; x++) {
                    Color pixel = reference.GetPixel(x, y);
                    int brightness = Math.Max(
                            pixel.R,
                            Math.Max(pixel.G, pixel.B));
                    if (pixel.A < 8 || brightness < 32) continue;
                    double weight = pixel.A * brightness;
                    totalWeight += weight;
                    red += pixel.R * weight;
                    green += pixel.G * weight;
                    blue += pixel.B * weight;
                }
            }
            if (totalWeight <= 0.0) return Color.FromArgb(255, 196, 0);
            return Color.FromArgb(
                    255,
                    (int) Math.Round(red / totalWeight),
                    (int) Math.Round(green / totalWeight),
                    (int) Math.Round(blue / totalWeight));
        } finally {
            reference.Dispose();
        }
    }

    public static void ExtractWarmLights(
            string inputPath,
            string referencePath,
            string outputPath) {
        Bitmap source = new Bitmap(inputPath);
        Color glowColor = GetReferenceGlowColor(referencePath);
        Bitmap output = new Bitmap(
                source.Width,
                source.Height,
                PixelFormat.Format32bppArgb);
        try {
            for (int y = 0; y < source.Height; y++) {
                for (int x = 0; x < source.Width; x++) {
                    Color pixel = source.GetPixel(x, y);
                    if (pixel.A < 16) continue;

                    int maxChannel = Math.Max(
                            pixel.R,
                            Math.Max(pixel.G, pixel.B));
                    int minChannel = Math.Min(
                            pixel.R,
                            Math.Min(pixel.G, pixel.B));
                    double saturation = maxChannel == 0
                            ? 0.0
                            : (maxChannel - minChannel) / (double) maxChannel;
                    double hue = GetHue(pixel.R, pixel.G, pixel.B);

                    // Isolate the existing yellow/green equipment lamps while
                    // rejecting the station's ivory highlights and red metal.
                    if (hue < 42.0 || hue > 110.0
                            || saturation < 0.20
                            || maxChannel < 82) {
                        continue;
                    }

                    double saturationWeight = Clamp01(
                            (saturation - 0.17) / 0.36);
                    double brightnessWeight = Clamp01(
                            (maxChannel - 72.0) / 145.0);
                    double sourceAlpha = pixel.A / 255.0;
                    double intensity = sourceAlpha
                            * saturationWeight
                            * (0.35 + 0.65 * brightnessWeight)
                            * 1.45;
                    int alpha = (int) Math.Round(255.0 * Clamp01(intensity));
                    if (alpha < 8) continue;
                    output.SetPixel(
                            x,
                            y,
                            Color.FromArgb(
                                    alpha,
                                    glowColor.R,
                                    glowColor.G,
                                    glowColor.B));
                }
            }
            output.Save(outputPath, ImageFormat.Png);
        } finally {
            output.Dispose();
            source.Dispose();
        }
    }

}
'@
}

$modRoot = Split-Path -Parent $PSScriptRoot
$assetRoot = Join-Path $modRoot "graphics\campaign\fob_ithaca"
$station = Join-Path $assetRoot "ithaca_station.png"
$ring = Join-Path $assetRoot "ithaca_orbital_ring_v4.png"
$referenceGlow = Join-Path $assetRoot "ithaca_orbital_ring_glow.png"
$ringGlow = Join-Path $assetRoot "ithaca_orbital_ring_glow_v4.png"
$stationGlow = Join-Path $assetRoot "ithaca_station_glow.png"

[ChiefNavigatorFobGlowBuilder]::ExtractWarmLights(
        $ring,
        $referenceGlow,
        $ringGlow)
[ChiefNavigatorFobGlowBuilder]::ExtractWarmLights(
        $station,
        $referenceGlow,
        $stationGlow)

Write-Host "WROTE $ringGlow"
Write-Host "WROTE $stationGlow"
