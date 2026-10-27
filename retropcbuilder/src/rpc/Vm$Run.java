package rpc;

public class Vm$Run {
    public String[] lines;
    public int[] nums;
    public String error;
    public int steps;
    public boolean ok;
    
    public Vm$Run() {
        this.lines = new String[0];
        this.nums = new int[0];
        this.error = null;
        this.steps = 0;
        this.ok = true;
    }
    
    public int lastNum() {
        return (this.nums.length != 0) ? this.nums[this.nums.length - 1] : 0;
    }
    
    public int sumNums() {
        int i = 0;
        int i0 = 0;
        while(i0 < this.nums.length) {
            i = i + this.nums[i0];
            i0 = i0 + 1;
        }
        return i;
    }
    
    public int countNums() {
        return this.nums.length;
    }
}
