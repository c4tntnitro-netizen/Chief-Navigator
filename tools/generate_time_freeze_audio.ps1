param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\sounds\chief_navigator')
)

$ErrorActionPreference = 'Stop'
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null

if (-not ('ChiefNavigatorChronalAudio' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.IO;

public static class ChiefNavigatorChronalAudio
{
    private const int SampleRate = 44100;
    private const double Tau = Math.PI * 2.0;

    public static void Generate(string outputDirectory)
    {
        WriteCharge(Path.Combine(outputDirectory, "omega_time_freeze_charge.wav"));
        WriteArrest(Path.Combine(outputDirectory, "omega_time_freeze.wav"));
    }

    private static void WriteCharge(string path)
    {
        const double duration = 2.0;
        int count = (int)(SampleRate * duration);
        double[] left = new double[count];
        double[] right = new double[count];
        uint noiseState = 0x4348524fU;
        double previousNoise = 0.0;

        for (int i = 0; i < count; i++) {
            double t = i / (double)SampleRate;
            double p = t / duration;
            double rise = p * p;
            double flutter = 0.78 + 0.22 * Math.Pow(Math.Sin(Tau * 3.25 * t), 2.0);
            double start = SmoothStep(0.0, 0.035, t);
            double end = 1.0 - SmoothStep(duration - 0.035, duration, t);

            double bassPhase = Tau * (42.0 * t + 13.0 * t * t);
            double bass = Math.Sin(bassPhase) * (0.18 + 0.48 * rise);
            double metal =
                    Math.Sin(Tau * (231.0 * t + 31.0 * t * t)) * 0.19
                    + Math.Sin(Tau * (367.0 * t + 47.0 * t * t) + 0.8) * 0.14
                    + Math.Sin(Tau * (613.0 * t + 71.0 * t * t) + 1.7) * 0.10
                    + Math.Sin(Tau * (947.0 * t + 89.0 * t * t) + 2.4) * 0.07;

            double noise = NextNoise(ref noiseState);
            double edge = noise - previousNoise;
            previousNoise = noise;
            double hiss = edge * 0.09 * p * p * p;
            double sample = (bass + metal * (0.25 + 0.75 * p) + hiss)
                    * rise * flutter * start * end;

            left[i] = sample;
            right[i] = sample * 0.93
                    + Math.Sin(Tau * (372.0 * t + 45.0 * t * t) + 1.1)
                    * 0.045 * rise * start * end;
        }

        NormalizeAndWrite(path, left, right, 0.82);
    }

    private static void WriteArrest(string path)
    {
        const double duration = 2.75;
        int count = (int)(SampleRate * duration);
        double[] left = new double[count];
        double[] right = new double[count];
        uint leftNoiseState = 0x54494d45U;
        uint rightNoiseState = 0x53544f50U;
        double previousLeftNoise = 0.0;
        double previousRightNoise = 0.0;
        double lowPassedLeft = 0.0;
        double lowPassedRight = 0.0;
        double subPhase = 0.0;

        for (int i = 0; i < count; i++) {
            double t = i / (double)SampleRate;
            double subFrequency = 29.0 + 92.0 * Math.Exp(-3.4 * t);
            subPhase += Tau * subFrequency / SampleRate;

            double attack = SmoothStep(0.0, 0.012, t);
            double tail = 1.0 - SmoothStep(duration - 0.18, duration, t);
            double impactEnvelope = attack * Math.Exp(-1.55 * t);
            double sub = Math.Sin(subPhase) * 0.68 * impactEnvelope;
            double pressure = Math.Sin(Tau * (43.0 * t - 4.2 * t * t))
                    * 0.25 * Math.Exp(-1.05 * t) * attack;

            double metalEnvelope = attack * Math.Exp(-1.72 * t);
            double metal =
                    Math.Sin(Tau * (319.0 * t - 7.5 * t * t) + 0.2) * 0.18
                    + Math.Sin(Tau * (487.0 * t - 10.5 * t * t) + 1.0) * 0.13
                    + Math.Sin(Tau * (733.0 * t - 14.0 * t * t) + 2.1) * 0.09
                    + Math.Sin(Tau * (1091.0 * t - 19.0 * t * t) + 2.8) * 0.06;

            double leftNoise = NextNoise(ref leftNoiseState);
            double rightNoise = NextNoise(ref rightNoiseState);
            double leftCrack = leftNoise - previousLeftNoise;
            double rightCrack = rightNoise - previousRightNoise;
            previousLeftNoise = leftNoise;
            previousRightNoise = rightNoise;

            lowPassedLeft += (leftNoise - lowPassedLeft) * 0.014;
            lowPassedRight += (rightNoise - lowPassedRight) * 0.014;
            double vacuumEnvelope = SmoothStep(0.10, 0.48, t)
                    * Math.Exp(-0.92 * Math.Max(0.0, t - 0.48));
            double crackEnvelope = Math.Exp(-37.0 * t);
            double lockDip = 1.0 - 0.62
                    * SmoothStep(0.12, 0.22, t)
                    * (1.0 - SmoothStep(0.34, 0.62, t));

            left[i] = ((sub + pressure + metal * metalEnvelope) * lockDip
                    + leftCrack * 0.34 * crackEnvelope
                    + lowPassedLeft * 0.13 * vacuumEnvelope) * tail;
            right[i] = ((sub * 0.96 + pressure * 1.03 + metal * 0.91 * metalEnvelope) * lockDip
                    + rightCrack * 0.34 * crackEnvelope
                    + lowPassedRight * 0.13 * vacuumEnvelope) * tail;
        }

        NormalizeAndWrite(path, left, right, 0.88);
    }

    private static double NextNoise(ref uint state)
    {
        state ^= state << 13;
        state ^= state >> 17;
        state ^= state << 5;
        return (state / (double)uint.MaxValue) * 2.0 - 1.0;
    }

    private static double SmoothStep(double edge0, double edge1, double value)
    {
        if (value <= edge0) return 0.0;
        if (value >= edge1) return 1.0;
        double x = (value - edge0) / (edge1 - edge0);
        return x * x * (3.0 - 2.0 * x);
    }

    private static void NormalizeAndWrite(
            string path, double[] left, double[] right, double targetPeak)
    {
        double peak = 0.000001;
        for (int i = 0; i < left.Length; i++) {
            peak = Math.Max(peak, Math.Abs(left[i]));
            peak = Math.Max(peak, Math.Abs(right[i]));
        }
        double gain = targetPeak / peak;
        int dataSize = left.Length * 4;

        using (FileStream stream = new FileStream(path, FileMode.Create, FileAccess.Write))
        using (BinaryWriter writer = new BinaryWriter(stream)) {
            writer.Write(new char[] { 'R', 'I', 'F', 'F' });
            writer.Write(36 + dataSize);
            writer.Write(new char[] { 'W', 'A', 'V', 'E' });
            writer.Write(new char[] { 'f', 'm', 't', ' ' });
            writer.Write(16);
            writer.Write((short)1);
            writer.Write((short)2);
            writer.Write(SampleRate);
            writer.Write(SampleRate * 4);
            writer.Write((short)4);
            writer.Write((short)16);
            writer.Write(new char[] { 'd', 'a', 't', 'a' });
            writer.Write(dataSize);
            for (int i = 0; i < left.Length; i++) {
                writer.Write(ToPcm16(left[i] * gain));
                writer.Write(ToPcm16(right[i] * gain));
            }
        }
    }

    private static short ToPcm16(double sample)
    {
        sample = Math.Max(-1.0, Math.Min(1.0, sample));
        return (short)Math.Round(sample * 32767.0);
    }
}
'@
}

[ChiefNavigatorChronalAudio]::Generate($OutputDirectory)

Get-Item -LiteralPath (
        Join-Path $OutputDirectory 'omega_time_freeze_charge.wav'), (
        Join-Path $OutputDirectory 'omega_time_freeze.wav') |
    Select-Object FullName, Length, LastWriteTime
