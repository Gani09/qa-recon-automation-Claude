package com.fiserv.optis.qarecon.model;
public class Pair<L,R>{
    private final L left; private final R right;
    public Pair(L l, R r){ this.left=l; this.right=r; }
    public L getLeft(){ return left; }
    public R getRight(){ return right; }
}
