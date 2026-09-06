package run.ikaros.music;
import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record AddMusicAudioSourceRequest(@NotNull UUID attachmentId,String codec,String container,Long durationMillis,Integer sampleRate,Integer bitDepth,Integer channels,Integer bitrate,boolean lossless,int preferenceWeight) {}
