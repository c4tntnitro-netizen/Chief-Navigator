$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# The user requested black-only extraction. Keep the generated alpha/edge detail,
# discard colored or light pixels, and bake neutral black into the actual PNG.
if (-not ('BouvoFilamentMatte' -as [type])) {
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
public static class BouvoFilamentMatte {
    public static string Build(string source, string destination, bool rear) {
        using (var original = new Bitmap(source))
        using (var image = original.Clone(new Rectangle(0,0,original.Width,original.Height), PixelFormat.Format32bppArgb)) {
            var bits=image.LockBits(new Rectangle(0,0,image.Width,image.Height),ImageLockMode.ReadWrite,PixelFormat.Format32bppArgb);
            long kept=0,partial=0;
            try {
                var bytes=new byte[Math.Abs(bits.Stride)*image.Height];
                Marshal.Copy(bits.Scan0,bytes,0,bytes.Length);
                for(int i=0;i<bytes.Length;i+=4) {
                    int max=Math.Max(bytes[i],Math.Max(bytes[i+1],bytes[i+2]));
                    int min=Math.Min(bytes[i],Math.Min(bytes[i+1],bytes[i+2]));
                    if(rear && bytes[i+3]>0) {
                        double x=((i/4)%image.Width)+0.5-image.Width*0.5;
                        double y=((i/4)/image.Width)+0.5-image.Height*0.5;
                        double hole=image.Width*0.5*(1.04/1.85);
                        double fade=Math.Max(0,Math.Min(1,(Math.Sqrt(x*x+y*y)-hole)/3));
                        bytes[i+3]=(byte)(bytes[i+3]*fade);
                    }
                    if(bytes[i+3]>=16 && max<=128 && max-min<=28) {
                        bytes[i]=8; bytes[i+1]=7; bytes[i+2]=7;
                        kept++; if(bytes[i+3]<255) partial++;
                    } else { bytes[i]=bytes[i+1]=bytes[i+2]=bytes[i+3]=0; }
                }
                Marshal.Copy(bytes,0,bits.Scan0,bytes.Length);
            } finally { image.UnlockBits(bits); }
            double coverage=(double)kept/(image.Width*image.Height);
            if(coverage<0.002 || coverage>0.20 || partial==0) throw new Exception("Invalid black filament alpha coverage: "+coverage+", partial="+partial);
            image.Save(destination,ImageFormat.Png);
            return image.Width+"x"+image.Height+", coverage="+coverage.ToString("P2")+", antialiased pixels="+partial;
        }
    }
}
'@
}
$root = Split-Path -Parent $PSScriptRoot
foreach ($layer in @('front', 'rear')) {
    $source = Join-Path $root ('graphics/source/bouvo_wrapping_v2/' + $layer + '_raw.png')
    $runtime = Join-Path $root ('graphics/campaign/ungaikyo/chief_navigator_bouvo_filaments_' + $layer + '_v2.png')
    Write-Output ($layer + ': ' + [BouvoFilamentMatte]::Build($source, $runtime, ($layer -eq 'rear')))
}
