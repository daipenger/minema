/*
 ** 2013 May 13
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.client.util;

import info.ata4.minecraft.minema.client.modules.modifiers.TimerModifier;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to keep track of various time-related information during a
 * capture.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class CaptureTime {

    private static final Map<TimeUnit, String> TU_SUFFIX;

    static {
        Map<TimeUnit, String> tuSuffix = new EnumMap<>(TimeUnit.class);
        tuSuffix.put(TimeUnit.DAYS, "d");
        tuSuffix.put(TimeUnit.HOURS, "h");
        tuSuffix.put(TimeUnit.MINUTES, "m");
        tuSuffix.put(TimeUnit.SECONDS, "s");
        tuSuffix.put(TimeUnit.MILLISECONDS, "ms");
        tuSuffix.put(TimeUnit.MICROSECONDS, "µs");
        tuSuffix.put(TimeUnit.NANOSECONDS, "ns");
        TU_SUFFIX = Collections.unmodifiableMap(tuSuffix);
    }

    public static String getTimeUnit(long nanos) {
        TimeUnit tu = null;
        TimeUnit[] tus = TimeUnit.values();
        long time = nanos;

        for (int i = tus.length - 1; i >= 0; i--) {
            tu = tus[i];
            time = tu.convert(nanos, TimeUnit.NANOSECONDS);
            if (time > 1) {
                break;
            }
        }

        return time + TU_SUFFIX.get(tu);
    }

    public static String getTimeStringFull(long nanos) {
        long hours = TimeUnit.NANOSECONDS.toHours(nanos);
        long minutes = TimeUnit.NANOSECONDS.toMinutes(nanos) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos) - TimeUnit.MINUTES.toSeconds(minutes)
                - TimeUnit.HOURS.toSeconds(hours);
        long milis = TimeUnit.NANOSECONDS.toMillis(nanos) - TimeUnit.SECONDS.toMillis(seconds)
                - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.HOURS.toMillis(hours);

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milis);
    }

    public static String getTimeStringSimple(long nanos) {
        long hours = TimeUnit.NANOSECONDS.toHours(nanos);
        long minutes = TimeUnit.NANOSECONDS.toMinutes(nanos) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos) - TimeUnit.MINUTES.toSeconds(minutes)
                - TimeUnit.HOURS.toSeconds(hours);

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    private final long startTime;
    private final long nanosPerFrame;
    private long currentFrameTime;
    private long prevFrameTime;
    private int frames;

    public CaptureTime(double frameRate) {
        nanosPerFrame = (long) (TimeUnit.SECONDS.toNanos(1) / frameRate);
        startTime = currentFrameTime = prevFrameTime = System.nanoTime();
    }

    public void nextFrame() {
        if (!TimerModifier.canRecord()) {
            return;
        }

        prevFrameTime = currentFrameTime;
        currentFrameTime = System.nanoTime();
        frames++;
    }

    public int getNumFrames() {
        return frames;
    }

    public boolean isNextFrame() {
        return getTimeSincePreviousFrame() >= nanosPerFrame;
    }

    public long getPreviousCaptureTime() {
        return currentFrameTime - prevFrameTime;
    }

    public long getTimeSincePreviousFrame() {
        return System.nanoTime() - prevFrameTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getRealTime() {
        return System.nanoTime() - startTime;
    }

    public String getRealTimeString() {
        return getTimeStringFull(getRealTime());
    }

    public long getVideoTime() {
        return frames * nanosPerFrame;
    }

    public String getVideoTimeString() {
        return getTimeStringFull(getVideoTime());
    }

    public double getAverageFPS() {
        return TimeUnit.SECONDS.toNanos(1) / ((double) getRealTime() / (double) getNumFrames());
    }
}
