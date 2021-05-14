package info.ata4.minecraft.minema.client.config;

public enum MotionBlur {
	  DISABLE(0), POTATO(128), LOW(512), MEDIUM(1024), HIGH(4096), ULTRA(8192);
	  
	  private int min;
	  
	  MotionBlur(int min) {
	    this.min = min;
	  }
	  
	  public int getExp(double fps) {
	    int i = 0;
	    while (fps < this.min) {
	      fps *= 2;
	      i++;
	    } 
	    return i;
	  }
}
