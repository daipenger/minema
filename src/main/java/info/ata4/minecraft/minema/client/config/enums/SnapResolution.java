/*
 ** 2014 September 05
 **
 ** The author disclaims copyright to this source code. In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.client.config.enums;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public enum SnapResolution {

    DISABLE(0), MOD2(2), MOD4(4), MOD8(8), MOD16(16);

    private final int mod;

    private SnapResolution(int mod) {
        this.mod = mod;
    }

    public int snap(int value) {
        return mod == 0 ? value : value - (value % mod);
    }
}
