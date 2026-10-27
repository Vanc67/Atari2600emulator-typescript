package rpc;

class Game$InstHolder {
    static rpc.Inst bridge;
    
    static {
        bridge = new rpc.Inst();
    }
    
    Game$InstHolder() {
    }
    
    static rpc.Inst inst() {
        return bridge;
    }
}
