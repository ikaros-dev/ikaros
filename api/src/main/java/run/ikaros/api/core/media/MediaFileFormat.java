package run.ikaros.api.core.media;

import java.util.Set;

/**
 * 媒体文件真实格式的权威定义.
 */
public enum MediaFileFormat {
    PNG(MediaFileCategory.IMAGE, "image/png", "png"),
    JPEG(MediaFileCategory.IMAGE, "image/jpeg", "jpg", "jpeg", "jpe", "jfif"),
    GIF(MediaFileCategory.IMAGE, "image/gif", "gif"),
    WEBP(MediaFileCategory.IMAGE, "image/webp", "webp"),
    BMP(MediaFileCategory.IMAGE, "image/bmp", "bmp", "dib"),
    AVIF(MediaFileCategory.IMAGE, "image/avif", "avif"),
    HEIF(MediaFileCategory.IMAGE, "image/heif", "heic", "heif"),
    SVG(MediaFileCategory.IMAGE, "image/svg+xml", "svg"),
    TIFF(MediaFileCategory.IMAGE, "image/tiff", "tiff", "tif"),
    EPS(MediaFileCategory.IMAGE, "application/postscript", "eps"),
    ICO(MediaFileCategory.IMAGE, "image/x-icon", "ico", "cur"),
    TGA(MediaFileCategory.IMAGE, "image/x-tga", "tga"),
    PCX(MediaFileCategory.IMAGE, "image/x-pcx", "pcx"),
    WMF(MediaFileCategory.IMAGE, "image/wmf", "wmf"),
    EMF(MediaFileCategory.IMAGE, "image/emf", "emf"),
    DDS(MediaFileCategory.IMAGE, "image/vnd-ms.dds", "dds"),
    HDR(MediaFileCategory.IMAGE, "image/vnd.radiance", "hdr"),
    EXR(MediaFileCategory.IMAGE, "image/x-exr", "exr"),
    NETPBM(MediaFileCategory.IMAGE, "image/x-portable-anymap", "pbm", "pgm", "ppm", "pnm"),
    FITS(MediaFileCategory.IMAGE, "image/fits", "fits", "fit"),
    JPEG_2000(MediaFileCategory.IMAGE, "image/jp2", "jp2", "j2k", "jpf", "jpm", "jpg2"),
    JPEG_XL(MediaFileCategory.IMAGE, "image/jxl", "jxl"),
    IFF(MediaFileCategory.IMAGE, "image/x-iff", "iff", "lbm"),
    PICT(MediaFileCategory.IMAGE, "image/x-pict", "mac", "pct", "pict"),
    XBM(MediaFileCategory.IMAGE, "image/x-xbitmap", "xbm"),
    XPM(MediaFileCategory.IMAGE, "image/x-xpixmap", "xpm"),
    CGM(MediaFileCategory.IMAGE, "image/cgm", "cgm"),
    FPX(MediaFileCategory.IMAGE, "image/vnd.fpx", "fpx"),
    WBMP(MediaFileCategory.IMAGE, "image/vnd.wap.wbmp", "wbm", "wbmp"),
    PHOTO_CD(MediaFileCategory.IMAGE, "image/x-photo-cd", "pcd"),
    SUN_RASTER(MediaFileCategory.IMAGE, "image/x-sun-raster", "ras", "sun"),

    MP4(MediaFileCategory.VIDEO, "video/mp4", "mp4", "m4v", "f4v", "3gp", "3g2", "m4p"),
    QUICKTIME(MediaFileCategory.VIDEO, "video/quicktime", "mov", "qt"),
    MATROSKA(MediaFileCategory.VIDEO, "video/x-matroska", "mkv"),
    WEBM(MediaFileCategory.VIDEO, "video/webm", "webm"),
    AVI(MediaFileCategory.VIDEO, "video/x-msvideo", "avi"),
    FLV(MediaFileCategory.VIDEO, "video/x-flv", "flv"),
    REALMEDIA(MediaFileCategory.VIDEO, "application/vnd.rn-realmedia", "rmvb", "rm"),
    MPEG_TS(MediaFileCategory.VIDEO, "video/mp2t", "ts", "m2ts", "mts", "tp", "trp"),
    MPEG_VIDEO(MediaFileCategory.VIDEO, "video/mpeg", "vob", "dat", "mpg", "mpeg", "mpe",
        "m1v", "m2v"),
    WMV(MediaFileCategory.VIDEO, "video/x-ms-wmv", "wmv", "asf", "wtv", "dvr-ms"),
    OGV(MediaFileCategory.VIDEO, "video/ogg", "ogv", "ogg"),
    SWF(MediaFileCategory.VIDEO, "application/x-shockwave-flash", "swf"),
    MXF(MediaFileCategory.VIDEO, "application/mxf", "mxf"),
    AMV(MediaFileCategory.VIDEO, "video/x-amv", "amv"),
    VIVO(MediaFileCategory.VIDEO, "video/vnd.vivo", "viv"),
    IVF(MediaFileCategory.VIDEO, "video/x-ivf", "ivf"),
    FLI(MediaFileCategory.VIDEO, "video/x-fli", "flc", "fli"),
    MJPEG(MediaFileCategory.VIDEO, "video/x-motion-jpeg", "mjpeg"),

    MP3(MediaFileCategory.AUDIO, "audio/mpeg", "mp3"),
    AAC(MediaFileCategory.AUDIO, "audio/aac", "aac"),
    M4A(MediaFileCategory.AUDIO, "audio/mp4", "m4a", "m4r", "m4b", "m4p"),
    FLAC(MediaFileCategory.AUDIO, "audio/flac", "flac"),
    OGG(MediaFileCategory.AUDIO, "audio/ogg", "ogg", "oga"),
    OPUS(MediaFileCategory.AUDIO, "audio/opus", "opus"),
    WAV(MediaFileCategory.AUDIO, "audio/wav", "wav"),
    WMA(MediaFileCategory.AUDIO, "audio/x-ms-wma", "wma"),
    APE(MediaFileCategory.AUDIO, "audio/ape", "ape"),
    ALAC(MediaFileCategory.AUDIO, "audio/alac", "alac"),
    AIFF(MediaFileCategory.AUDIO, "audio/aiff", "aiff", "aif", "aifc"),
    DSF(MediaFileCategory.AUDIO, "audio/dsf", "dsf"),
    DFF(MediaFileCategory.AUDIO, "audio/dff", "dff"),
    AMR(MediaFileCategory.AUDIO, "audio/amr", "amr", "awb"),
    MIDI(MediaFileCategory.AUDIO, "audio/midi", "mid", "midi"),
    REALAUDIO(MediaFileCategory.AUDIO, "audio/vnd.rn-realaudio", "ra"),
    MP2(MediaFileCategory.AUDIO, "audio/mpeg", "mp2"),
    MP1(MediaFileCategory.AUDIO, "audio/mpeg", "mp1"),
    WAVPACK(MediaFileCategory.AUDIO, "audio/wavpack", "wv"),
    MUSEPACK(MediaFileCategory.AUDIO, "audio/musepack", "mpc"),
    MATROSKA_AUDIO(MediaFileCategory.AUDIO, "audio/x-matroska", "mka"),
    TAK(MediaFileCategory.AUDIO, "audio/x-tak", "tak"),
    TTA(MediaFileCategory.AUDIO, "audio/x-tta", "tta"),
    SHORTEN(MediaFileCategory.AUDIO, "audio/x-shorten", "shn"),
    GSM(MediaFileCategory.AUDIO, "audio/gsm", "gsm"),
    AU(MediaFileCategory.AUDIO, "audio/basic", "au", "snd"),
    VOX(MediaFileCategory.AUDIO, "audio/x-voxware", "vox"),
    QCP(MediaFileCategory.AUDIO, "audio/qcelp", "qcp"),
    OMA(MediaFileCategory.AUDIO, "audio/oma", "oma"),

    SRT(MediaFileCategory.SUBTITLE, "application/x-subrip", "srt"),
    ASS(MediaFileCategory.SUBTITLE, "text/x-ssa", "ass"),
    SSA(MediaFileCategory.SUBTITLE, "text/x-ssa", "ssa"),
    VTT(MediaFileCategory.SUBTITLE, "text/vtt", "vtt"),
    MICRODVD(MediaFileCategory.SUBTITLE, "text/plain", "sub"),
    VOBSUB(MediaFileCategory.SUBTITLE, "application/octet-stream", "sub"),
    IDX(MediaFileCategory.SUBTITLE, "text/plain", "idx"),
    TTML(MediaFileCategory.SUBTITLE, "application/ttml+xml", "ttml", "dfxp"),
    SBV(MediaFileCategory.SUBTITLE, "text/plain", "sbv"),
    PGS(MediaFileCategory.SUBTITLE, "application/octet-stream", "sup", "pgs"),
    SAMI(MediaFileCategory.SUBTITLE, "application/x-sami", "smi", "sami"),
    SMIL(MediaFileCategory.SUBTITLE, "application/smil+xml", "smil"),
    SCC(MediaFileCategory.SUBTITLE, "text/x-scc", "scc"),
    REALTEXT(MediaFileCategory.SUBTITLE, "text/vnd.rn-realtext", "rt"),
    PJS(MediaFileCategory.SUBTITLE, "text/plain", "pjs"),
    CAP(MediaFileCategory.SUBTITLE, "application/octet-stream", "cap"),
    STL(MediaFileCategory.SUBTITLE, "application/octet-stream", "stl"),
    TDS(MediaFileCategory.SUBTITLE, "application/octet-stream", "tds"),
    TTXT(MediaFileCategory.SUBTITLE, "application/ttml+xml", "ttxt"),
    USF(MediaFileCategory.SUBTITLE, "application/xml", "usf"),
    AQT(MediaFileCategory.SUBTITLE, "text/plain", "aqt"),
    JSS(MediaFileCategory.SUBTITLE, "text/plain", "jss"),
    LRC(MediaFileCategory.LYRICS, "text/plain", "lrc"),
    KRC(MediaFileCategory.LYRICS, "application/octet-stream", "krc"),
    QRC(MediaFileCategory.LYRICS, "application/octet-stream", "qrc"),
    TRC(MediaFileCategory.LYRICS, "text/plain", "trc"),
    KSC(MediaFileCategory.LYRICS, "text/plain", "ksc"),
    KAJ(MediaFileCategory.LYRICS, "text/plain", "kaj"),
    TLRC(MediaFileCategory.LYRICS, "text/plain", "tlrc"),
    SKRC(MediaFileCategory.LYRICS, "text/plain", "skrc");

    /** 媒体文件的真实类别. */
    private final MediaFileCategory category;

    /** 媒体文件的规范 MIME. */
    private final String mimeType;

    /** 允许进入真实格式检测的扩展名. */
    private final Set<String> extensions;

    MediaFileFormat(MediaFileCategory category, String mimeType, String... extensions) {
        this.category = category;
        this.mimeType = mimeType;
        this.extensions = Set.of(extensions);
    }

    public MediaFileCategory category() {
        return category;
    }

    public String mimeType() {
        return mimeType;
    }

    public Set<String> extensions() {
        return extensions;
    }
}
