param(
    [System.Drawing.Bitmap]$Target,
    [ValidateRange(0, 80)]
    [int]$SmallCapReductionPercent = 40
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot

if (-not ('IthacaMountCaps' -as [type])) {
    $drawingReferences = @('System.Drawing')
    if ($PSVersionTable.PSEdition -eq 'Core') {
        $drawingReferences = @((Get-ChildItem (Join-Path $PSHOME 'ref/*.dll')).FullName) +
            [System.Drawing.Bitmap].Assembly.Location +
            @((Get-ChildItem (Join-Path $PSHOME 'System.Private.Windows.*.dll')).FullName)
    }
    Add-Type -ReferencedAssemblies $drawingReferences -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;

public static class IthacaMountCaps
{
    // Empty left-side painted sockets on the established 5632x2216 PNG.
    // Omit the eight naval/Annihilator seats and the two small seats at
    // (680,1170) and its mirror, occupied by the outer Cloudswarm housings.
    // The upper SmallA seats remain separate empty sockets near the inner
    // Cloudswarm housings; foreground weapon overlap is not a shared seat.
    // Restore the original shield, hub and ribbed designs with their exact
    // pre-round fitted width/height baselines and original assignments.
    private static readonly int[,] SmallA = {
        {2328,170,310,260}, {670,1525,310,310}, {438,1310,250,230}
    };
    private static readonly int[,] SmallB = {
        {1610,645,280,230}, {398,1660,250,250}
    };
    private static readonly int[,] SmallC = {
        {863,773,280,250}, {203,1458,250,250}
    };
    private static readonly int[,] Hardpoints = {
        {1345,712,250,285}, {1933,770,325,360}
    };

    private static Rectangle VisibleBounds(Bitmap image)
    {
        int l=image.Width, t=image.Height, r=-1, b=-1;
        for (int y=0; y<image.Height; y++)
            for (int x=0; x<image.Width; x++)
                if (image.GetPixel(x,y).A>8) {
                    l=Math.Min(l,x); t=Math.Min(t,y);
                    r=Math.Max(r,x); b=Math.Max(b,y);
                }
        if (r<l) throw new InvalidOperationException("Empty cap image");
        return Rectangle.FromLTRB(Math.Max(0,l-1),Math.Max(0,t-1),
            Math.Min(image.Width,r+2),Math.Min(image.Height,b+2));
    }

    public static Bitmap Prepare(string root, string kind, int width, int height)
    {
        string name="chief_navigator_ithaca_"+kind+"_cap_teal_";
        string raw=Path.Combine(root,"graphics/source/"+name+"raw_v1.png");
        string output=Path.Combine(root,"graphics/weapons/covers/"+name+"v1.png");
        using (Bitmap original=new Bitmap(raw)) {
            Bitmap cap=new Bitmap(width,height,PixelFormat.Format32bppArgb);
            using (Graphics g=Graphics.FromImage(cap)) {
                g.CompositingMode=CompositingMode.SourceCopy;
                g.InterpolationMode=InterpolationMode.HighQualityBicubic;
                g.PixelOffsetMode=PixelOffsetMode.HighQuality;
                g.DrawImage(original,new Rectangle(0,0,width,height),
                    VisibleBounds(original),GraphicsUnit.Pixel);
            }
            Directory.CreateDirectory(Path.GetDirectoryName(output));
            cap.Save(output,ImageFormat.Png);
            return cap;
        }
    }

    private static void Stamp(Graphics g, Bitmap cap, int[,] mounts, bool[] occupied,
            double scale=1.0)
    {
        for (int i=0;i<mounts.GetLength(0);i++) {
            int x=mounts[i,0], y=mounts[i,1];
            int w=(int)Math.Round(mounts[i,2]*scale,MidpointRounding.AwayFromZero);
            int h=(int)Math.Round(mounts[i,3]*scale,MidpointRounding.AwayFromZero);
            int left=x-w/2, top=y-h/2;
            using (Bitmap stamp=new Bitmap(w,h,PixelFormat.Format32bppArgb)) {
                using (Graphics fit=Graphics.FromImage(stamp)) {
                    fit.InterpolationMode=InterpolationMode.HighQualityBicubic;
                    fit.PixelOffsetMode=PixelOffsetMode.HighQuality;
                    fit.DrawImage(cap,new Rectangle(0,0,w,h));
                }
                // Check the actual fitted silhouettes, not just rectangles:
                // shaped cap edges leave room for adjacent covers.
                for (int sy=0;sy<h;sy++)
                    for (int sx=0;sx<w;sx++) {
                        if (stamp.GetPixel(sx,sy).A<=8) continue;
                        int index=(top+sy)*5632+left+sx;
                        if (occupied[index])
                            throw new InvalidOperationException(
                                "Ithaca mount caps overlap at PNG pixel "+(left+sx)+","+(top+sy));
                        occupied[index]=true;
                    }
                g.DrawImageUnscaled(stamp,left,top);
            }
        }
    }

    public static void Apply(Bitmap target, Bitmap hardpoint,
            Bitmap smallA, Bitmap smallB, Bitmap smallC,
            double smallScale)
    {
        if (target.Width!=5632 || target.Height!=2216)
            throw new InvalidOperationException("Unexpected Ithaca canvas");
        using (Bitmap layer=new Bitmap(target.Width,target.Height,PixelFormat.Format32bppArgb)) {
            bool[] occupied=new bool[target.Width*target.Height];
            using (Graphics g=Graphics.FromImage(layer)) {
                g.InterpolationMode=InterpolationMode.HighQualityBicubic;
                g.PixelOffsetMode=PixelOffsetMode.HighQuality;
                Stamp(g,smallA,SmallA,occupied,smallScale);
                Stamp(g,smallB,SmallB,occupied,smallScale);
                Stamp(g,smallC,SmallC,occupied,smallScale);
                Stamp(g,hardpoint,Hardpoints,occupied);
            }
            Rectangle bounds=new Rectangle(0,0,target.Width,target.Height);
            BitmapData td=target.LockBits(bounds,ImageLockMode.ReadWrite,PixelFormat.Format32bppArgb);
            BitmapData ld=layer.LockBits(bounds,ImageLockMode.ReadOnly,PixelFormat.Format32bppArgb);
            try {
                byte[] dst=new byte[td.Stride*target.Height];
                byte[] src=new byte[ld.Stride*layer.Height];
                Marshal.Copy(td.Scan0,dst,0,dst.Length);
                Marshal.Copy(ld.Scan0,src,0,src.Length);
                for (int y=0;y<target.Height;y++)
                    for (int x=0;x<target.Width;x++) {
                        // Reflect the finished left overlay after resampling,
                        // avoiding bicubic rounding differences on the right.
                        int sourceX=x<target.Width/2 ? x : target.Width-1-x;
                        int d=y*td.Stride+x*4, s=y*ld.Stride+sourceX*4;
                        int a=src[s+3];
                        if (a==0) continue;
                        int oldAlpha=dst[d+3];
                        int combinedAlpha=a*255+oldAlpha*(255-a);
                        for (int c=0;c<3;c++)
                            dst[d+c]=(byte)((src[s+c]*a*255+
                                dst[d+c]*oldAlpha*(255-a)+combinedAlpha/2)/combinedAlpha);
                        dst[d+3]=(byte)((combinedAlpha+127)/255);
                        // Complete cap silhouettes may extend beyond a socket.
                        // Pixels outside the cover layer remain byte-identical.
                    }
                Marshal.Copy(dst,0,td.Scan0,dst.Length);
            } finally {
                target.UnlockBits(td); layer.UnlockBits(ld);
            }
        }
    }
}
'@
}

$caps = @{}
try {
    $caps['hardpoint'] = [IthacaMountCaps]::Prepare($root, 'hardpoint', 384, 480)
    foreach ($kind in @('small_a', 'small_b', 'small_c')) {
        $caps[$kind] = [IthacaMountCaps]::Prepare($root, $kind, 384, 384)
    }
    if ($null -ne $Target) {
        [IthacaMountCaps]::Apply($Target, $caps['hardpoint'],
            $caps['small_a'], $caps['small_b'], $caps['small_c'],
            (1.0 - $SmallCapReductionPercent / 100.0))
        Write-Output "Built 14 small covers in 3 original designs and 4 rectangular covers; small-cap size reduction: $SmallCapReductionPercent%."
    }
} finally {
    foreach ($cap in $caps.Values) { $cap.Dispose() }
}
