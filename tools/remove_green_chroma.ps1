param(
    [Parameter(Mandatory = $true)]
    [string] $InputPath,

    [Parameter(Mandatory = $true)]
    [string] $OutputPath
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

if (-not ("ChiefNavigatorChroma" -as [type])) {
    Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;

public static class ChiefNavigatorChroma {
    private static int Clamp(int value) {
        return Math.Max(0, Math.Min(255, value));
    }

    private static double SmoothStep(double value) {
        value = Math.Max(0.0, Math.Min(1.0, value));
        return value * value * (3.0 - 2.0 * value);
    }

    public static void RemoveGreen(string inputPath, string outputPath) {
        using (Image source = Image.FromFile(inputPath))
        using (Bitmap bitmap = new Bitmap(source.Width, source.Height, PixelFormat.Format32bppArgb)) {
            using (Graphics graphics = Graphics.FromImage(bitmap)) {
                graphics.DrawImageUnscaled(source, 0, 0);
            }

            Rectangle bounds = new Rectangle(0, 0, bitmap.Width, bitmap.Height);
            BitmapData data = bitmap.LockBits(bounds, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
            int byteCount = Math.Abs(data.Stride) * data.Height;
            byte[] pixels = new byte[byteCount];
            Marshal.Copy(data.Scan0, pixels, 0, byteCount);

            const int keyR = 0;
            const int keyG = 255;
            const int keyB = 0;
            const int transparentThreshold = 12;
            const int opaqueThreshold = 96;
            long visiblePixels = 0;
            long borderVisiblePixels = 0;

            for (int y = 0; y < data.Height; y++) {
                int row = y * data.Stride;
                for (int x = 0; x < data.Width; x++) {
                    int offset = row + x * 4;
                    int b = pixels[offset];
                    int g = pixels[offset + 1];
                    int r = pixels[offset + 2];
                    int originalAlpha = pixels[offset + 3];

                    int distance = Math.Max(Math.Abs(r - keyR), Math.Max(Math.Abs(g - keyG), Math.Abs(b - keyB)));
                    int other = Math.Max(r, b);
                    int dominance = g - other;
                    // Image-generation output can leave tiny darker key-color
                    // islands inside torn wreck edges. The generated wall art
                    // intentionally contains no green, so a low dominance
                    // floor safely removes those remnants as well as the main
                    // background matte.
                    bool keyLike = distance <= 32 || dominance >= 4;
                    int outputAlpha = 255;

                    if (keyLike) {
                        if (distance <= transparentThreshold) {
                            outputAlpha = 0;
                        } else if (distance < opaqueThreshold) {
                            double ratio = (distance - transparentThreshold) / (double)(opaqueThreshold - transparentThreshold);
                            outputAlpha = Clamp((int)Math.Round(255.0 * SmoothStep(ratio)));
                        }

                        if (dominance > 0) {
                            double denominator = Math.Max(1.0, keyG - other);
                            int dominanceAlpha = Clamp((int)Math.Round(255.0 * (1.0 - Math.Min(1.0, dominance / denominator))));
                            outputAlpha = Math.Min(outputAlpha, dominanceAlpha);
                        }
                    }

                    outputAlpha = Clamp((int)Math.Round(outputAlpha * (originalAlpha / 255.0)));
                    if (outputAlpha <= 8) outputAlpha = 0;
                    if (x == 0 || y == 0 || x == data.Width - 1 || y == data.Height - 1) {
                        outputAlpha = 0;
                    }

                    if (g > other) {
                        g = Math.Max(0, other - 1);
                    }

                    pixels[offset] = (byte)b;
                    pixels[offset + 1] = (byte)g;
                    pixels[offset + 2] = (byte)r;
                    pixels[offset + 3] = (byte)outputAlpha;
                    if (outputAlpha > 8) {
                        visiblePixels++;
                        if (x == 0 || y == 0 || x == data.Width - 1 || y == data.Height - 1) {
                            borderVisiblePixels++;
                        }
                    }
                }
            }

            Marshal.Copy(pixels, 0, data.Scan0, byteCount);
            bitmap.UnlockBits(data);

            string directory = Path.GetDirectoryName(outputPath);
            if (!String.IsNullOrEmpty(directory)) Directory.CreateDirectory(directory);
            bitmap.Save(outputPath, ImageFormat.Png);
            double coverage = 100.0 * visiblePixels / (bitmap.Width * (double)bitmap.Height);
            Console.WriteLine("{0}: visible coverage {1:F1}%, visible border pixels {2}",
                Path.GetFileName(outputPath), coverage, borderVisiblePixels);
        }
    }
}
"@
}

[ChiefNavigatorChroma]::RemoveGreen($InputPath, $OutputPath)
