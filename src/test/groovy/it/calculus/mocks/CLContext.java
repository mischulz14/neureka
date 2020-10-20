package it.calculus.mocks;

import neureka.devices.opencl.utility.DispatchUtility;

public class CLContext {

    private int[] _local;

    public int[] getLocal() {
        return _local;
    }

    public int[] getGlobal() {
        return _global;
    }

    public int getLws() {
        return _lws;
    }

    public int getGws() {
        return _gws;
    }

    public int getWgs() {
        return _wgs;
    }

    public int getMaxTSRow() {
        return _max_ts_row;
    }

    public int getMaxTSCol() {
        return _max_ts_col;
    }

    public int getMaxTSCom(){
        return _max_ts_com;
    }

    public int getMaxWPTRow() {
        return _max_wpt_row;
    }

    public int getMaxWPTCol() {
        return _max_wpt_col;
    }

    public int getCounter() {
        return _counter;
    }

    private int[] _global;
    private int _lws;
    private int _gws;
    private int _wgs;

    private int _max_ts_row;
    private int _max_ts_col;
    private int _max_ts_com;
    private int _max_wpt_row;
    private int _max_wpt_col;

    public CLContext(
            int lws,
            int rws,
            int com_sze,
            int row_sze,
            int col_sze
    ) {
        int gws = com_sze*col_sze;
        int[] params = DispatchUtility.findBestParams(lws, rws, com_sze, row_sze, col_sze);

        _max_ts_row =  params[0];//   = 128, // ts := tile size
        _max_ts_col =  params[1];//   = 128,
        _max_ts_com =  params[2];//   = 16,
        _max_wpt_row = params[3];//  = 8,   // wpt := work per thread
        _max_wpt_col = params[4]; // = 8,

        assertInt((double)gws / (double)lws);
        int wgs = gws / lws;
        _lws = lws;
        _gws = gws;
        _wgs = wgs;

        _local =  new int[]{ _max_ts_row / _max_wpt_row, _max_ts_col / _max_wpt_col}; // Or { RTSM, RTSN };
        _global = new int[]{ row_sze/ _max_wpt_row, col_sze/ _max_wpt_col};

    }

    private int _counter;

    int get_local_id( int dimension ) {
        int i = _counter % _lws;

        if ( dimension == 0 ) return i % _local[0];
        else return i / _local[1];
    }

    int get_group_id( int dimension ) {
        return _counter / _lws;
    }

    public void increment(){
        _counter++;
    }

    private void assertInt(double num){
        assert ((num == Math.floor(num)) && !Double.isInfinite(num));
    }

}
