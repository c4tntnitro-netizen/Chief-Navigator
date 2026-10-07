param(
    [string]$GeneratedRoot = "C:\Users\myauc\.codex\generated_images\019fe8ce-1f44-7693-b113-f4f76934e8ce"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

public static class StarvingThreatImageOps {
    private static byte Clamp(double value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return (byte)Math.Round(value);
    }

    public static Bitmap RemoveMagentaKey(Bitmap source) {
        Color sampledKey = source.GetPixel(0, 0);
        double keyR = sampledKey.R;
        double keyG = sampledKey.G;
        double keyB = sampledKey.B;
        Bitmap working = new Bitmap(source.Width, source.Height, PixelFormat.Format32bppArgb);
        using (Graphics graphics = Graphics.FromImage(working)) {
            graphics.CompositingMode = CompositingMode.SourceCopy;
            graphics.DrawImageUnscaled(source, 0, 0);
        }
        Rectangle rect = new Rectangle(0, 0, working.Width, working.Height);
        BitmapData data = working.LockBits(rect, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
        int stride = Math.Abs(data.Stride);
        byte[] bytes = new byte[stride * working.Height];
        Marshal.Copy(data.Scan0, bytes, 0, bytes.Length);
        for (int y = 0; y < working.Height; y++) {
            int row = y * stride;
            for (int x = 0; x < working.Width; x++) {
                int i = row + (x * 4);
                double b = bytes[i];
                double g = bytes[i + 1];
                double r = bytes[i + 2];
                double dr = keyR - r;
                double dg = keyG - g;
                double db = keyB - b;
                double distance = Math.Sqrt((dr * dr) + (dg * dg) + (db * db));
                if (distance <= 36) {
                    bytes[i + 3] = 0;
                    continue;
                }
                double alpha = Math.Min(1.0, (distance - 36.0) / 90.0);
                bytes[i + 3] = Clamp(255 * alpha);
                if (alpha > 0.02 && alpha < 1.0) {
                    double inverse = 1.0 - alpha;
                    bytes[i + 2] = Clamp((r - (keyR * inverse)) / alpha);
                    bytes[i + 1] = Clamp((g - (keyG * inverse)) / alpha);
                    bytes[i] = Clamp((b - (keyB * inverse)) / alpha);
                }
                double cleanR = bytes[i + 2];
                double cleanG = bytes[i + 1];
                double cleanB = bytes[i];
                if (cleanR > cleanG + 12 && cleanB > cleanG + 12) {
                    double luminance = (cleanR * 0.30) + (cleanG * 0.50) + (cleanB * 0.20);
                    bytes[i + 2] = Clamp((luminance * 0.96) + 8);
                    bytes[i + 1] = Clamp((luminance * 1.04) + 8);
                    bytes[i] = Clamp((luminance * 1.08) + 8);
                }
            }
        }
        Marshal.Copy(bytes, 0, data.Scan0, bytes.Length);
        working.UnlockBits(data);
        return working;
    }

    public static Rectangle FindAlphaBounds(Bitmap bitmap) {
        Rectangle rect = new Rectangle(0, 0, bitmap.Width, bitmap.Height);
        BitmapData data = bitmap.LockBits(rect, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        int stride = Math.Abs(data.Stride);
        byte[] bytes = new byte[stride * bitmap.Height];
        Marshal.Copy(data.Scan0, bytes, 0, bytes.Length);
        bitmap.UnlockBits(data);
        int minX = bitmap.Width, minY = bitmap.Height, maxX = -1, maxY = -1;
        for (int y = 0; y < bitmap.Height; y++) {
            int row = y * stride;
            for (int x = 0; x < bitmap.Width; x++) {
                if (bytes[row + (x * 4) + 3] <= 12) continue;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
        if (maxX < minX || maxY < minY) throw new InvalidOperationException("No non-transparent sprite pixels found.");
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
'@

$projectRoot = Split-Path -Parent $PSScriptRoot
$outputDir = Join-Path $projectRoot "graphics\ships\starving_threat"
$previewDir = Join-Path $projectRoot "docs\assets"
New-Item -ItemType Directory -Force -Path $outputDir, $previewDir | Out-Null

$jobs = @(
    @{ Source = "exec-3838376a-1565-453a-af01-2d24e769cfa7.png"; Output = "starving_threat_assault_unit.png"; Width = 160; Height = 154; Label = "Assault Unit" },
    @{ Source = "exec-a1d722b9-4117-4815-923e-e106a6535756.png"; Output = "starving_threat_fabricator.png"; Width = 380; Height = 370; Label = "Fabricator" },
    @{ Source = "exec-25c71d5e-cbb7-4285-ab3a-c05a1e15efbb.png"; Output = "starving_threat_hive.png"; Width = 212; Height = 280; Label = "Hive Unit" },
    @{ Source = "exec-f4acafd1-54a7-45f1-9d2f-6cf40fb5b001.png"; Output = "starving_threat_overseer.png"; Width = 148; Height = 160; Label = "Overseer" },
    @{ Source = "exec-59e9c199-6c1d-42a5-9241-df7fd6c9af47.png"; Output = "starving_threat_skirmish.png"; Width = 84; Height = 86; Label = "Skirmish Unit" },
    @{ Source = "exec-7be7f5ee-6290-475d-ac97-1a6b58abb26d.png"; Output = "starving_threat_standoff_unit.png"; Width = 248; Height = 164; Label = "Standoff Unit" }
)

function Clamp-Byte([double]$value) {
    if ($value -lt 0) { return [byte]0 }
    if ($value -gt 255) { return [byte]255 }
    return [byte][Math]::Round($value)
}

function Remove-MagentaKey([System.Drawing.Bitmap]$source) {
    $working = New-Object System.Drawing.Bitmap($source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($working)
    $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $graphics.DrawImageUnscaled($source, 0, 0)
    $graphics.Dispose()

    $rect = New-Object System.Drawing.Rectangle(0, 0, $working.Width, $working.Height)
    $data = $working.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadWrite, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $bytes = New-Object byte[] ([Math]::Abs($data.Stride) * $working.Height)
    [Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)

    for ($y = 0; $y -lt $working.Height; $y++) {
        $row = $y * [Math]::Abs($data.Stride)
        for ($x = 0; $x -lt $working.Width; $x++) {
            $i = $row + ($x * 4)
            $b = [double]$bytes[$i]
            $g = [double]$bytes[$i + 1]
            $r = [double]$bytes[$i + 2]
            $distance = [Math]::Sqrt(([Math]::Pow(255 - $r, 2)) + ([Math]::Pow($g, 2)) + ([Math]::Pow(255 - $b, 2)))

            if ($distance -le 8) {
                $bytes[$i + 3] = 0
                continue
            }

            $alpha = [Math]::Min(1.0, ($distance - 8.0) / 130.0)
            $bytes[$i + 3] = Clamp-Byte (255 * $alpha)

            if ($alpha -gt 0.02 -and $alpha -lt 1.0) {
                $inverse = 1.0 - $alpha
                $bytes[$i + 2] = Clamp-Byte (($r - (255 * $inverse)) / $alpha)
                $bytes[$i + 1] = Clamp-Byte ($g / $alpha)
                $bytes[$i] = Clamp-Byte (($b - (255 * $inverse)) / $alpha)
            }
        }
    }

    [Runtime.InteropServices.Marshal]::Copy($bytes, 0, $data.Scan0, $bytes.Length)
    $working.UnlockBits($data)
    return $working
}

function Find-AlphaBounds([System.Drawing.Bitmap]$bitmap) {
    $minX = $bitmap.Width
    $minY = $bitmap.Height
    $maxX = -1
    $maxY = -1
    $rect = New-Object System.Drawing.Rectangle(0, 0, $bitmap.Width, $bitmap.Height)
    $data = $bitmap.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $stride = [Math]::Abs($data.Stride)
    $bytes = New-Object byte[] ($stride * $bitmap.Height)
    [Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
    for ($y = 0; $y -lt $bitmap.Height; $y++) {
        $row = $y * $stride
        for ($x = 0; $x -lt $bitmap.Width; $x++) {
            if ($bytes[$row + ($x * 4) + 3] -gt 12) {
                if ($x -lt $minX) { $minX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    $bitmap.UnlockBits($data)
    if ($maxX -lt $minX -or $maxY -lt $minY) { throw "No non-transparent sprite pixels found." }
    return New-Object System.Drawing.Rectangle($minX, $minY, ($maxX - $minX + 1), ($maxY - $minY + 1))
}

$processed = @()
foreach ($job in $jobs) {
    $sourcePath = Join-Path $GeneratedRoot $job.Source
    if (-not (Test-Path -LiteralPath $sourcePath)) { throw "Missing generated source: $sourcePath" }

    $raw = New-Object System.Drawing.Bitmap($sourcePath)
    $keyed = [StarvingThreatImageOps]::RemoveMagentaKey($raw)
    $raw.Dispose()
    $bounds = [StarvingThreatImageOps]::FindAlphaBounds($keyed)

    $canvas = New-Object System.Drawing.Bitmap($job.Width, $job.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($canvas)
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $scale = [Math]::Min(($job.Width - 4.0) / $bounds.Width, ($job.Height - 4.0) / $bounds.Height)
    $drawWidth = [Math]::Max(1, [int][Math]::Round($bounds.Width * $scale))
    $drawHeight = [Math]::Max(1, [int][Math]::Round($bounds.Height * $scale))
    $drawX = [int][Math]::Round(($job.Width - $drawWidth) / 2.0)
    $drawY = [int][Math]::Round(($job.Height - $drawHeight) / 2.0)
    $destination = New-Object System.Drawing.Rectangle($drawX, $drawY, $drawWidth, $drawHeight)
    $graphics.DrawImage($keyed, $destination, $bounds, [System.Drawing.GraphicsUnit]::Pixel)
    $graphics.Dispose()
    $keyed.Dispose()

    $outputPath = Join-Path $outputDir $job.Output
    $canvas.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $canvas.Dispose()
    $processed += @{ Path = $outputPath; Label = $job.Label }
}

$sheetWidth = 1000
$sheetHeight = 1480
$sheet = New-Object System.Drawing.Bitmap($sheetWidth, $sheetHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$sheetGraphics = [System.Drawing.Graphics]::FromImage($sheet)
$sheetGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$sheetGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$sheetGraphics.Clear([System.Drawing.Color]::FromArgb(255, 10, 15, 20))
$titleFont = New-Object System.Drawing.Font("Segoe UI Semibold", 34)
$labelFont = New-Object System.Drawing.Font("Segoe UI", 22)
$subtitleFont = New-Object System.Drawing.Font("Segoe UI", 16)
$white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(235, 238, 240))
$muted = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(145, 169, 174))
$panel = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 18, 27, 32))
$outline = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 46, 68, 74), 2)
$sheetGraphics.DrawString("STARVING THREAT", $titleFont, $white, 48, 28)
$sheetGraphics.DrawString("Incomplete hulls grown under severe material deprivation", $subtitleFont, $muted, 51, 78)

$cellWidth = 440
$cellHeight = 300
for ($index = 0; $index -lt $processed.Count; $index++) {
    $column = $index % 2
    $row = [Math]::Floor($index / 2)
    $x = 45 + ($column * 475)
    $y = 125 + ($row * 325)
    $cellRect = New-Object System.Drawing.Rectangle($x, $y, $cellWidth, $cellHeight)
    $sheetGraphics.FillRectangle($panel, $cellRect)
    $sheetGraphics.DrawRectangle($outline, $cellRect)
    $sprite = New-Object System.Drawing.Bitmap($processed[$index].Path)
    $availableWidth = $cellWidth - 50
    $availableHeight = $cellHeight - 70
    $scale = [Math]::Min($availableWidth / $sprite.Width, $availableHeight / $sprite.Height)
    $drawWidth = [int][Math]::Round($sprite.Width * $scale)
    $drawHeight = [int][Math]::Round($sprite.Height * $scale)
    $drawX = $x + [int][Math]::Round(($cellWidth - $drawWidth) / 2.0)
    $drawY = $y + 12 + [int][Math]::Round(($availableHeight - $drawHeight) / 2.0)
    $sheetGraphics.DrawImage($sprite, $drawX, $drawY, $drawWidth, $drawHeight)
    $sheetGraphics.DrawString($processed[$index].Label, $labelFont, $white, $x + 18, $y + $cellHeight - 48)
    $sprite.Dispose()
}

$sheetPath = Join-Path $previewDir "starving_threat_contact_sheet.png"
$sheet.Save($sheetPath, [System.Drawing.Imaging.ImageFormat]::Png)
$outline.Dispose()
$panel.Dispose()
$muted.Dispose()
$white.Dispose()
$subtitleFont.Dispose()
$labelFont.Dispose()
$titleFont.Dispose()
$sheetGraphics.Dispose()
$sheet.Dispose()

Write-Output "Created $($processed.Count) transparent Starving Threat sprites in $outputDir"
Write-Output "Created preview sheet at $sheetPath"
