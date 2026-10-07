param(
    [string]$SourceHalf = "graphics/ships/chief_navigator_drifting_wall_module_half_upscaled_v1.png",
    [string]$GoldReference = "graphics/ships/chief_navigator_drifting_wall_base_v1.png",
    [string]$TealReference = "graphics/ships/chief_navigator_drifting_wall_base_domain_v2.png",
    [string]$Output = "graphics/ships/chief_navigator_ithaca_module_half_teal_v1.png"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$sourcePath = (Resolve-Path -LiteralPath $SourceHalf).Path
$goldPath = (Resolve-Path -LiteralPath $GoldReference).Path
$tealPath = (Resolve-Path -LiteralPath $TealReference).Path
$outputPath = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $Output))
$outputDirectory = [System.IO.Path]::GetDirectoryName($outputPath)
[System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null

$typeName = "ChiefNavigatorExactPaletteRecolor"
if (-not ($typeName -as [type])) {
    Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;

public static class ChiefNavigatorExactPaletteRecolor
{
    private struct Accumulator
    {
        public long R;
        public long G;
        public long B;
        public int Count;

        public void Add(byte r, byte g, byte b)
        {
            R += r;
            G += g;
            B += b;
            Count++;
        }
    }

    private static Bitmap LoadArgb(string path)
    {
        using (Bitmap loaded = new Bitmap(path))
        {
            Bitmap copy = new Bitmap(loaded.Width, loaded.Height, PixelFormat.Format32bppArgb);
            using (Graphics graphics = Graphics.FromImage(copy))
            {
                graphics.CompositingMode = System.Drawing.Drawing2D.CompositingMode.SourceCopy;
                graphics.DrawImageUnscaled(loaded, 0, 0);
            }
            return copy;
        }
    }

    private static byte[] ReadPixels(Bitmap bitmap, out BitmapData data)
    {
        Rectangle bounds = new Rectangle(0, 0, bitmap.Width, bitmap.Height);
        data = bitmap.LockBits(bounds, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        byte[] pixels = new byte[Math.Abs(data.Stride) * data.Height];
        System.Runtime.InteropServices.Marshal.Copy(data.Scan0, pixels, 0, pixels.Length);
        bitmap.UnlockBits(data);
        return pixels;
    }

    private static int RgbKey(byte r, byte g, byte b)
    {
        return (r << 16) | (g << 8) | b;
    }

    private static int BucketKey(byte r, byte g, byte b)
    {
        return ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
    }

    private static byte Clamp(long value)
    {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return (byte)value;
    }

    public static string Recolor(string sourcePath, string goldPath, string tealPath, string outputPath)
    {
        using (Bitmap source = LoadArgb(sourcePath))
        using (Bitmap gold = LoadArgb(goldPath))
        using (Bitmap teal = LoadArgb(tealPath))
        {
            if (gold.Width != teal.Width || gold.Height != teal.Height)
                throw new InvalidOperationException("The gold and teal reference sprites must have identical dimensions.");

            BitmapData unused;
            byte[] goldPixels = ReadPixels(gold, out unused);
            byte[] tealPixels = ReadPixels(teal, out unused);
            byte[] sourcePixels = ReadPixels(source, out unused);

            Dictionary<int, Accumulator> exact = new Dictionary<int, Accumulator>(262144);
            Accumulator[] coarse = new Accumulator[32768];

            for (int y = 0; y < gold.Height; y++)
            {
                int row = y * gold.Width * 4;
                for (int x = 0; x < gold.Width; x++)
                {
                    int i = row + x * 4;
                    byte ga = goldPixels[i + 3];
                    byte ta = tealPixels[i + 3];
                    if (ga < 8 || ta < 8) continue;

                    byte gb = goldPixels[i];
                    byte gg = goldPixels[i + 1];
                    byte gr = goldPixels[i + 2];
                    byte tb = tealPixels[i];
                    byte tg = tealPixels[i + 1];
                    byte tr = tealPixels[i + 2];

                    int key = RgbKey(gr, gg, gb);
                    Accumulator value;
                    exact.TryGetValue(key, out value);
                    value.Add(tr, tg, tb);
                    exact[key] = value;

                    int bucket = BucketKey(gr, gg, gb);
                    Accumulator bucketValue = coarse[bucket];
                    bucketValue.Add(tr, tg, tb);
                    coarse[bucket] = bucketValue;
                }
            }

            long opaquePixels = 0;
            long exactPixels = 0;
            long coarsePixels = 0;
            long unchangedFallbackPixels = 0;

            for (int y = 0; y < source.Height; y++)
            {
                int row = y * source.Width * 4;
                for (int x = 0; x < source.Width; x++)
                {
                    int i = row + x * 4;
                    byte alpha = sourcePixels[i + 3];
                    if (alpha == 0) continue;
                    opaquePixels++;

                    byte b = sourcePixels[i];
                    byte g = sourcePixels[i + 1];
                    byte r = sourcePixels[i + 2];
                    Accumulator mapped;

                    if (exact.TryGetValue(RgbKey(r, g, b), out mapped) && mapped.Count > 0)
                    {
                        sourcePixels[i] = Clamp(mapped.B / mapped.Count);
                        sourcePixels[i + 1] = Clamp(mapped.G / mapped.Count);
                        sourcePixels[i + 2] = Clamp(mapped.R / mapped.Count);
                        exactPixels++;
                    }
                    else
                    {
                        mapped = coarse[BucketKey(r, g, b)];
                        if (mapped.Count > 0)
                        {
                            sourcePixels[i] = Clamp(mapped.B / mapped.Count);
                            sourcePixels[i + 1] = Clamp(mapped.G / mapped.Count);
                            sourcePixels[i + 2] = Clamp(mapped.R / mapped.Count);
                            coarsePixels++;
                        }
                        else
                        {
                            unchangedFallbackPixels++;
                        }
                    }
                    // Alpha is deliberately untouched: this is a palette-only operation.
                }
            }

            using (Bitmap output = new Bitmap(source.Width, source.Height, PixelFormat.Format32bppArgb))
            {
                Rectangle bounds = new Rectangle(0, 0, output.Width, output.Height);
                BitmapData outputData = output.LockBits(bounds, ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
                System.Runtime.InteropServices.Marshal.Copy(sourcePixels, 0, outputData.Scan0, sourcePixels.Length);
                output.UnlockBits(outputData);
                output.Save(outputPath, ImageFormat.Png);
            }

            return String.Format(
                "{0}x{1}; alpha pixels={2}; exact={3}; coarse={4}; unchanged fallback={5}",
                source.Width, source.Height, opaquePixels, exactPixels, coarsePixels, unchangedFallbackPixels);
        }
    }
}
'@
}

$result = [ChiefNavigatorExactPaletteRecolor]::Recolor($sourcePath, $goldPath, $tealPath, $outputPath)
Write-Output "Wrote $outputPath"
Write-Output $result
