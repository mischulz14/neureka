package neureka.devices.file.handles;

import neureka.Tsr;

public class PNGHandle extends AbstractImageFileHandle<PNGHandle>
{
    public PNGHandle( String fileName ) { this(null, fileName); }

    public PNGHandle( Tsr<Number> t, String filename ) { super("png", t, filename); }

    @Override
    public String extension() {
        return "png";
    }
}
