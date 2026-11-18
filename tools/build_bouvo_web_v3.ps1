$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
if (-not ('BouvoWebAtlasV3' -as [type])) {
    $drawingAssembly = [System.Drawing.Bitmap].Assembly
    $drawingReferences = @($drawingAssembly.Location, [System.Drawing.Rectangle].Assembly.Location) + @(
        $drawingAssembly.GetReferencedAssemblies() |
            Where-Object { $_.Name -like 'System.Private.Windows.*' } |
            ForEach-Object { [System.Reflection.Assembly]::Load($_).Location }
    )
    Add-Type -ReferencedAssemblies $drawingReferences @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
public static class BouvoWebAtlasV3 {
    private static double starAlphaMass;
    public static void Reference(string path) {
        using(var image=new Bitmap(path)) {
            double mass=0;for(int y=0;y<image.Height;y++)for(int x=0;x<image.Width;x++)mass+=image.GetPixel(x,y).A/255.0;
            starAlphaMass=mass/(image.Width*image.Height);
        }
    }
    public static string Build(string source, string destination, int columns, int rows, int tile, bool rear, bool shell) {
        using (var original = new Bitmap(source)) {
            int w=original.Width/columns,h=original.Height/rows;
            var region=new Rectangle((tile%columns)*w,(tile/columns)*h,w,h);
            using (var image=original.Clone(region,PixelFormat.Format32bppArgb)) {
                var bits=image.LockBits(new Rectangle(0,0,w,h),ImageLockMode.ReadWrite,PixelFormat.Format32bppArgb);
                long kept=0;double mass=0;var histogram=new int[256];
                try {
                    var bytes=new byte[Math.Abs(bits.Stride)*h];Marshal.Copy(bits.Scan0,bytes,0,bytes.Length);
                    for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                        int i=y*bits.Stride+x*4;
                        int max=Math.Max(bytes[i],Math.Max(bytes[i+1],bytes[i+2]));
                        int min=Math.Min(bytes[i],Math.Min(bytes[i+1],bytes[i+2]));
                        int alpha=bytes[i+3];
                        if(alpha<16||max>128||max-min>28)alpha=0;
                        if(shell) {
                            // Transparent tile padding, without forcing the painted outline into a circle.
                            int edge=Math.Min(Math.Min(x,w-1-x),Math.Min(y,h-1-y));
                            alpha=(int)(alpha*Math.Max(0,Math.Min(1,(edge-2)/8.0)));
                        }
                        histogram[alpha]++;
                        if(rear) {
                            double distance=Math.Sqrt(Math.Pow(x+.5-w*.5,2)+Math.Pow(y+.5-h*.5,2));
                            double hole=w*.5*(1.04/1.85);
                            alpha=(int)(alpha*Math.Max(0,Math.Min(1,(distance-hole)/3)));
                        }
                        bytes[i]=alpha>0?(byte)8:(byte)0;bytes[i+1]=bytes[i+2]=alpha>0?(byte)7:(byte)0;
                        bytes[i+3]=(byte)alpha;if(alpha>0)kept++;mass+=alpha/255.0;
                    }
                    if(shell) {
                        // Preserve the approved sun's pigment density and match all planets to it.
                        double low=0,high=128;
                        for(int step=0;step<24;step++) {
                            double trial=(low+high)*.5,total=0;
                            for(int a=1;a<256;a++)total+=histogram[a]*Math.Min(255,a*trial);
                            if(total/(w*h*255.0)<starAlphaMass)low=trial;else high=trial;
                        }
                        double gain=(low+high)*.5;kept=0;mass=0;
                        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                            int i=y*bits.Stride+x*4;
                            int alpha=(int)Math.Min(255,bytes[i+3]*gain);
                            bytes[i+3]=(byte)alpha;if(alpha>0)kept++;mass+=alpha/255.0;
                            if(alpha==0)bytes[i]=bytes[i+1]=bytes[i+2]=0;
                        }
                    }
                    Marshal.Copy(bytes,0,bits.Scan0,bytes.Length);
                } finally {image.UnlockBits(bits);}
                double coverage=(double)kept/(w*h);
                if(coverage<.002||coverage>.3)throw new Exception("Invalid filament coverage "+coverage);
                image.Save(destination,ImageFormat.Png);
                return w+"x"+h+", occupied="+coverage.ToString("P2")+", alpha mass="+(mass/(w*h)).ToString("P2");
            }
        }
    }
}
'@
}
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root 'graphics/source/bouvo_web_v3'
$runtime = Join-Path $root 'graphics/campaign/ungaikyo'
[BouvoWebAtlasV3]::Reference((Join-Path $runtime 'chief_navigator_bouvo_filaments_front_v2.png'))
foreach ($layer in @('front','rear')) {
    $rear = $layer -eq 'rear'
    Write-Output ('star '+$layer+': '+[BouvoWebAtlasV3]::Build((Join-Path $source 'star_raw.png'),
        (Join-Path $runtime ('chief_navigator_bouvo_star_'+$layer+'_v3.png')),1,1,0,$rear,$true))
    for ($i=0;$i -lt 5;$i++) {
        Write-Output ('planet '+$i+' '+$layer+': '+[BouvoWebAtlasV3]::Build((Join-Path $source 'planets_atlas_raw.png'),
            (Join-Path $runtime ('chief_navigator_bouvo_planet_'+$i+'_'+$layer+'_v3.png')),3,2,$i,$rear,$true))
    }
}
for ($i=0;$i -lt 3;$i++) {
    Write-Output ('tether '+$i+': '+[BouvoWebAtlasV3]::Build((Join-Path $source 'tethers_atlas_raw.png'),
        (Join-Path $runtime ('chief_navigator_bouvo_tether_'+$i+'_v3.png')),1,3,$i,$false,$false))
}
