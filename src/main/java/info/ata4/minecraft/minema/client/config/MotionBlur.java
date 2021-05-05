package info.ata4.minecraft.minema.client.config;

public enum MotionBlur {
	DISABLE(0), POTATO(5), LOW(6), MEDIUM(7), HIGH(8), ULTRA(9);
	
	private int exp;
	
	private MotionBlur(int exp) {
		this.exp = exp;
	}
	
	public int getExp() {
		return this.exp;
	}
}
