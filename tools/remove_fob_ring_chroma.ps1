$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if (-not ("ChiefNavigatorChromaRemoval" -as [type])) {
    Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class ChiefNavigatorChromaRemoval {
    private static int ClampByte(double value) {
        if (value <= 0.0) return 0;
        if (value >= 255.0) return 255;
        return (int) Math.Round(value);
    }

    private static double Clamp01(double value) {
        if (value <= 0.0) return 0.0;
        if (value >= 1.0) return 1.0;
        return value;
    }

    private static double SmoothStep(double value) {
        value = Clamp01(value);
        return value * value * (3.0 - 2.0 * value);
    }

    public static void RemoveMagenta(string inputPath, string outputPath) {
        Bitmap source = new Bitmap(inputPath);
        Bitmap output = new Bitmap(
                source.Width,
                source.Height,
                PixelFormat.Format32bppArgb);
        try {
            const double transparentDominance = 150.0;
            const double opaqueDominance = 40.0;

            for (int y = 0; y < source.Height; y++) {
                for (int x = 0; x < source.Width; x++) {
                    Color pixel = source.GetPixel(x, y);
                    double magentaDominance = Math.Max(
                            0.0,
                            Math.Min(pixel.R, pixel.B) - pixel.G);
                    double alpha = 1.0 - SmoothStep(
                            (magentaDominance - opaqueDominance)
                            / (transparentDominance - opaqueDominance));
                    alpha *= pixel.A / 255.0;
                    if (alpha <= 0.002) continue;

                    double red = pixel.R;
                    double green = pixel.G;
                    double blue = pixel.B;
                    // Neutralize the magenta component without unpremultiplying
                    // the low-alpha green channel into a colored fringe.
                    double magentaExcess = Math.Max(
                            0.0,
                            Math.Min(red, blue) - green);
                    red -= magentaExcess;
                    blue -= magentaExcess;

                    output.SetPixel(
                            x,
                            y,
                            Color.FromArgb(
                                    ClampByte(alpha * 255.0),
                                    ClampByte(red),
                                    ClampByte(green),
                                    ClampByte(blue)));
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
$inputPath = Join-Path $assetRoot "ithaca_orbital_ring_v4_chroma.png"
$outputPath = Join-Path $assetRoot "ithaca_orbital_ring_v4.png"

[ChiefNavigatorChromaRemoval]::RemoveMagenta($inputPath, $outputPath)
Write-Host "WROTE $outputPath"
