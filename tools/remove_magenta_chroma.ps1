param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath,

    [Parameter(Mandatory = $true)]
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

if (-not ('ChiefNavigator.Tools.MagentaKey' -as [type])) {
    Add-Type -ReferencedAssemblies 'System.Drawing.dll' -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;

namespace ChiefNavigator.Tools {
    public static class MagentaKey {
        private static float SmoothStep(float low, float high, float value) {
            float t = Math.Max(0f, Math.Min(1f, (value - low) / (high - low)));
            return t * t * (3f - 2f * t);
        }

        private static byte ClampByte(float value) {
            return (byte)Math.Max(0f, Math.Min(255f, value));
        }

        public static void Convert(string inputPath, string outputPath) {
            using (Bitmap source = new Bitmap(inputPath))
            using (Bitmap output = new Bitmap(
                    source.Width,
                    source.Height,
                    PixelFormat.Format32bppArgb)) {
                using (Graphics graphics = Graphics.FromImage(output)) {
                    graphics.DrawImageUnscaled(source, 0, 0);
                }

                Rectangle bounds = new Rectangle(0, 0, output.Width, output.Height);
                BitmapData data = output.LockBits(
                        bounds,
                        ImageLockMode.ReadWrite,
                        PixelFormat.Format32bppArgb);
                int byteCount = Math.Abs(data.Stride) * data.Height;
                byte[] pixels = new byte[byteCount];
                Marshal.Copy(data.Scan0, pixels, 0, byteCount);

                // The generated key varies slightly around #ef10dc. Identify it
                // by magenta dominance instead of requiring one exact RGB value.
                const float keyR = 239f;
                const float keyG = 18f;
                const float keyB = 220f;
                for (int y = 0; y < output.Height; y++) {
                    int row = y * data.Stride;
                    for (int x = 0; x < output.Width; x++) {
                        int offset = row + x * 4;
                        float blue = pixels[offset];
                        float green = pixels[offset + 1];
                        float red = pixels[offset + 2];

                        float magentaDominance = Math.Min(red, blue) - green;
                        float brightness = Math.Min(red, blue);
                        float redBlueBalance = Math.Abs(red - blue);
                        float keyAmount = SmoothStep(8f, 90f, magentaDominance)
                                * SmoothStep(45f, 145f, brightness)
                                * (1f - SmoothStep(55f, 130f, redBlueBalance));
                        float coverage = 1f - keyAmount;

                        if (coverage <= 0.06f) {
                            pixels[offset] = 0;
                            pixels[offset + 1] = 0;
                            pixels[offset + 2] = 0;
                            pixels[offset + 3] = 0;
                            continue;
                        }

                        // Remove the estimated key contribution from partially
                        // covered antialiased edge pixels to prevent a pink halo.
                        float restoredBlue = (blue - keyAmount * keyB) / coverage;
                        float restoredGreen = (green - keyAmount * keyG) / coverage;
                        float restoredRed = (red - keyAmount * keyR) / coverage;
                        float neutralize = SmoothStep(0.08f, 0.92f, keyAmount);
                        restoredBlue = restoredBlue
                                + (restoredGreen - restoredBlue) * neutralize;
                        restoredRed = restoredRed
                                + (restoredGreen - restoredRed) * neutralize;
                        pixels[offset] = ClampByte(restoredBlue);
                        pixels[offset + 1] = ClampByte(restoredGreen);
                        pixels[offset + 2] = ClampByte(restoredRed);
                        pixels[offset + 3] = ClampByte(coverage * 255f);
                    }
                }

                Marshal.Copy(pixels, 0, data.Scan0, byteCount);
                output.UnlockBits(data);
                Directory.CreateDirectory(Path.GetDirectoryName(outputPath));
                output.Save(outputPath, ImageFormat.Png);
            }
        }
    }
}
'@
}

$resolvedInput = (Resolve-Path -LiteralPath $InputPath).Path
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
[ChiefNavigator.Tools.MagentaKey]::Convert($resolvedInput, $resolvedOutput)
Write-Output "WROTE $resolvedOutput"
