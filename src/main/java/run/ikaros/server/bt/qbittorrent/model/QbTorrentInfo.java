package run.ikaros.server.bt.qbittorrent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author li-guohao
 * @link <a href="https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)#get-torrent-list">WebUI-API-(qBittorrent-4.1)#get-torrent-list</a>
 */
public class QbTorrentInfo {
    /**
     * Time (Unix Epoch) when the torrent was added to the client
     */
    @JsonProperty("added_on")
    private Long addedOn;

    /**
     * Amount of data left to download (bytes)
     */
    @JsonProperty("amount_left")
    private Double amountLeft;

    /**
     * Whether this torrent is managed by Automatic Torrent Management
     */
    @JsonProperty("auto_tmm")
    private Boolean autoTmm;

    /**
     * Percentage of file pieces currently available
     */
    private Float availability;

    /**
     * Category of the torrent
     */
    private String category;

    /**
     * Amount of transfer data completed (bytes)
     */
    private Long completed;

    /**
     * Time (Unix Epoch) when the torrent completed
     */
    @JsonProperty("completion_on")
    private Long completionOn;

    /**
     * Absolute path of torrent content (root path for multifile torrents,
     * absolute file path for singlefile torrents)
     * <br/>
     * 单种子多文件时，此为对应的下载的目录；单种子单文件时，此为文件的绝对路径
     */
    @JsonProperty("content_path")
    private String contentPath;

    /**
     * Torrent download speed limit (bytes/s). -1 if ulimited.
     */
    @JsonProperty("dl_limit")
    private Long dlLimit;

    /**
     * Torrent download speed (bytes/s)
     */
    private Long dlspeed;

    /**
     * Amount of data downloaded
     */
    private Long downloaded;

    /**
     * Amount of data downloaded this session
     */
    @JsonProperty("downloaded_session")
    private Long downloadedSession;

    /**
     * Torrent ETA (seconds)
     */
    private Long eta;

    /**
     * True if first last piece are prioritized
     */
    @JsonProperty("f_l_piece_prio")
    private Boolean firstLastPieceArePrioritized;

    /**
     * True if force start is enabled for this torrent
     */
    @JsonProperty("force_start")
    private Boolean forceStart;

    /**
     * Torrent hash
     */
    private String hash;

    /**
     * Last time (Unix Epoch) when a chunk was downloaded/uploaded
     */
    @JsonProperty("last_activity")
    private Long lastActivity;

    /**
     * Magnet URI corresponding to this torrent
     */
    @JsonProperty("magnet_uri")
    private String magnetUri;

    /**
     * Maximum share ratio until torrent is stopped from seeding/uploading
     */
    @JsonProperty("max_ratio")
    private Float maxRatio;

    /**
     * Maximum seeding time (seconds) until torrent is stopped from seeding
     */
    @JsonProperty("max_seeding_time")
    private Long maxSeedingTime;

    /**
     * Torrent name
     */
    private String name;

    /**
     * Number of seeds in the swarm
     */
    @JsonProperty("num_complete")
    private Long numComplete;

    /**
     * Number of leechers in the swarm
     */
    @JsonProperty("num_incomplete")
    private Long numIncomplete;

    /**
     * Number of leechers connected to
     */
    @JsonProperty("num_leechs")
    private Long numLeechs;

    /**
     * Number of seeds connected to
     */
    @JsonProperty("num_seeds")
    private Long numSeeds;

    /**
     * Torrent priority. Returns -1 if queuing is disabled or torrent is in seed mode
     */
    private Long priority;

    /**
     * Torrent progress (percentage/100)
     */
    private Float progress;

    /**
     * Torrent share ratio. Max ratio value: 9999.
     */
    private Float ratio;

    /**
     * Path where this torrent's data is stored
     */
    @JsonProperty("save_path")
    private String savePath;

    /**
     * Torrent elapsed time while complete (seconds)
     */
    @JsonProperty("seeding_time")
    private Long seedingTime;

    /**
     * Time (Unix Epoch) when this torrent was last seen complete
     */
    @JsonProperty("seen_complete")
    private Long seenComplete;

    /**
     * True if sequential download is enabled
     */
    @JsonProperty("seq_dl")
    private Boolean seqDl;

    /**
     * Total size (bytes) of files selected for download
     */
    private Long size;

    /**
     * Torrent state. See table here below for the possible values
     * <p>Possible values of state:</p>
     * <table>
     *     <tr>
     *         <th>Value</th>
     *         <th>Description</th>
     *     </tr>
     *     <tr>
     *         <td>error</td>
     *         <td>Some error occurred, applies to paused torrents</td>
     *     </tr>
     *     <tr>
     *         <td>missingFiles</td>
     *         <td>Torrent data files is missing</td>
     *     </tr>
     *     <tr>
     *         <td>uploading</td>
     *         <td>Torrent is being seeded and data is being transferred</td>
     *     </tr>
     *     <tr>
     *         <td>pausedUP</td>
     *         <td>Torrent is paused and has finished downloading</td>
     *     </tr>
     *     <tr>
     *         <td>queuedUP</td>
     *         <td>Queuing is enabled and torrent is queued for upload</td>
     *     </tr>
     *     <tr>
     *         <td>stalledUP</td>
     *         <td>Torrent is being seeded, but no connection were made</td>
     *     </tr>
     *     <tr>
     *         <td>checkingUP</td>
     *         <td>Torrent has finished downloading and is being checked</td>
     *     </tr>
     *     <tr>
     *         <td>forcedUP</td>
     *         <td>Torrent is forced to uploading and ignore queue limit</td>
     *     </tr>
     *     <tr>
     *         <td>allocating</td>
     *         <td>Torrent is allocating disk space for download</td>
     *     </tr>
     *     <tr>
     *         <td>downloading</td>
     *         <td>Torrent is being downloaded and data is being transferred</td>
     *     </tr>
     *     <tr>
     *         <td>metaDL</td>
     *         <td>Torrent has just started downloading and is fetching metadata</td>
     *     </tr>
     *     <tr>
     *         <td>pausedDL</td>
     *         <td>Torrent is paused and has NOT finished downloading</td>
     *     </tr>
     *     <tr>
     *         <td>queuedDL</td>
     *         <td>Queuing is enabled and torrent is queued for download</td>
     *     </tr>
     *     <tr>
     *         <td>stalledDL</td>
     *         <td>Torrent is being downloaded, but no connection were made</td>
     *     </tr>
     *     <tr>
     *         <td>checkingDL</td>
     *         <td>Same as checkingUP, but torrent has NOT finished downloading</td>
     *     </tr>
     *     <tr>
     *         <td>forcedDL</td>
     *         <td>Torrent is forced to downloading to ignore queue limit</td>
     *     </tr>
     *     <tr>
     *         <td>checkingResumeData</td>
     *         <td>Checking resume data on qBt startup</td>
     *     </tr>
     *     <tr>
     *         <td>moving</td>
     *         <td>Torrent is moving to another location</td>
     *     </tr>
     *     <tr>
     *         <td>unknown</td>
     *         <td>Unknown status</td>
     *     </tr>
     * </table>
     */
    private String state;

    /**
     * True if super seeding is enabled
     */
    @JsonProperty("super_seeding")
    private Boolean superSeeding;

    /**
     * Comma-concatenated tag list of the torrent
     */
    private String tags;

    /**
     * Total active time (seconds)
     */
    @JsonProperty("time_active")
    private Long timeActive;

    /**
     * Total size (bytes) of all file in this torrent (including unselected ones)
     */
    @JsonProperty("total_size")
    private Long totalSize;

    /**
     * The first tracker with working status. Returns empty string if no tracker is working.
     */
    private String tracker;

    /**
     * Torrent upload speed limit (bytes/s). -1 if ulimited.
     */
    @JsonProperty("up_limit")
    private Long uploadLimit = -1L;

    /**
     * Amount of data uploaded
     */
    private Long uploaded;

    /**
     * Amount of data uploaded this session
     */
    @JsonProperty("uploaded_session")
    private Long uploadedSession;

    /**
     * Torrent upload speed (bytes/s)
     */
    @JsonProperty("upspeed")
    private Long uploadSpeed;

    public Long getAddedOn() {
        return addedOn;
    }

    public QbTorrentInfo setAddedOn(Long addedOn) {
        this.addedOn = addedOn;
        return this;
    }

    public Double getAmountLeft() {
        return amountLeft;
    }

    public QbTorrentInfo setAmountLeft(Double amountLeft) {
        this.amountLeft = amountLeft;
        return this;
    }

    public Boolean getAutoTmm() {
        return autoTmm;
    }

    public QbTorrentInfo setAutoTmm(Boolean autoTmm) {
        this.autoTmm = autoTmm;
        return this;
    }

    public Float getAvailability() {
        return availability;
    }

    public QbTorrentInfo setAvailability(Float availability) {
        this.availability = availability;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public QbTorrentInfo setCategory(String category) {
        this.category = category;
        return this;
    }

    public Long getCompleted() {
        return completed;
    }

    public QbTorrentInfo setCompleted(Long completed) {
        this.completed = completed;
        return this;
    }

    public Long getCompletionOn() {
        return completionOn;
    }

    public QbTorrentInfo setCompletionOn(Long completionOn) {
        this.completionOn = completionOn;
        return this;
    }

    public String getContentPath() {
        return contentPath;
    }

    public QbTorrentInfo setContentPath(String contentPath) {
        this.contentPath = contentPath;
        return this;
    }

    public Long getDlLimit() {
        return dlLimit;
    }

    public QbTorrentInfo setDlLimit(Long dlLimit) {
        this.dlLimit = dlLimit;
        return this;
    }

    public Long getDlspeed() {
        return dlspeed;
    }

    public QbTorrentInfo setDlspeed(Long dlspeed) {
        this.dlspeed = dlspeed;
        return this;
    }

    public Long getDownloaded() {
        return downloaded;
    }

    public QbTorrentInfo setDownloaded(Long downloaded) {
        this.downloaded = downloaded;
        return this;
    }

    public Long getDownloadedSession() {
        return downloadedSession;
    }

    public QbTorrentInfo setDownloadedSession(Long downloadedSession) {
        this.downloadedSession = downloadedSession;
        return this;
    }

    public Long getEta() {
        return eta;
    }

    public QbTorrentInfo setEta(Long eta) {
        this.eta = eta;
        return this;
    }

    public Boolean getFirstLastPieceArePrioritized() {
        return firstLastPieceArePrioritized;
    }

    public QbTorrentInfo setFirstLastPieceArePrioritized(
        Boolean firstLastPieceArePrioritized) {
        this.firstLastPieceArePrioritized = firstLastPieceArePrioritized;
        return this;
    }

    public Boolean getForceStart() {
        return forceStart;
    }

    public QbTorrentInfo setForceStart(Boolean forceStart) {
        this.forceStart = forceStart;
        return this;
    }

    public String getHash() {
        return hash;
    }

    public QbTorrentInfo setHash(String hash) {
        this.hash = hash;
        return this;
    }

    public Long getLastActivity() {
        return lastActivity;
    }

    public QbTorrentInfo setLastActivity(Long lastActivity) {
        this.lastActivity = lastActivity;
        return this;
    }

    public String getMagnetUri() {
        return magnetUri;
    }

    public QbTorrentInfo setMagnetUri(String magnetUri) {
        this.magnetUri = magnetUri;
        return this;
    }

    public Float getMaxRatio() {
        return maxRatio;
    }

    public QbTorrentInfo setMaxRatio(Float maxRatio) {
        this.maxRatio = maxRatio;
        return this;
    }

    public Long getMaxSeedingTime() {
        return maxSeedingTime;
    }

    public QbTorrentInfo setMaxSeedingTime(Long maxSeedingTime) {
        this.maxSeedingTime = maxSeedingTime;
        return this;
    }

    public String getName() {
        return name;
    }

    public QbTorrentInfo setName(String name) {
        this.name = name;
        return this;
    }

    public Long getNumComplete() {
        return numComplete;
    }

    public QbTorrentInfo setNumComplete(Long numComplete) {
        this.numComplete = numComplete;
        return this;
    }

    public Long getNumIncomplete() {
        return numIncomplete;
    }

    public QbTorrentInfo setNumIncomplete(Long numIncomplete) {
        this.numIncomplete = numIncomplete;
        return this;
    }

    public Long getNumLeechs() {
        return numLeechs;
    }

    public QbTorrentInfo setNumLeechs(Long numLeechs) {
        this.numLeechs = numLeechs;
        return this;
    }

    public Long getNumSeeds() {
        return numSeeds;
    }

    public QbTorrentInfo setNumSeeds(Long numSeeds) {
        this.numSeeds = numSeeds;
        return this;
    }

    public Long getPriority() {
        return priority;
    }

    public QbTorrentInfo setPriority(Long priority) {
        this.priority = priority;
        return this;
    }

    public Float getProgress() {
        return progress;
    }

    public QbTorrentInfo setProgress(Float progress) {
        this.progress = progress;
        return this;
    }

    public Float getRatio() {
        return ratio;
    }

    public QbTorrentInfo setRatio(Float ratio) {
        this.ratio = ratio;
        return this;
    }

    public String getSavePath() {
        return savePath;
    }

    public QbTorrentInfo setSavePath(String savePath) {
        this.savePath = savePath;
        return this;
    }

    public Long getSeedingTime() {
        return seedingTime;
    }

    public QbTorrentInfo setSeedingTime(Long seedingTime) {
        this.seedingTime = seedingTime;
        return this;
    }

    public Long getSeenComplete() {
        return seenComplete;
    }

    public QbTorrentInfo setSeenComplete(Long seenComplete) {
        this.seenComplete = seenComplete;
        return this;
    }

    public Boolean getSeqDl() {
        return seqDl;
    }

    public QbTorrentInfo setSeqDl(Boolean seqDl) {
        this.seqDl = seqDl;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public QbTorrentInfo setSize(Long size) {
        this.size = size;
        return this;
    }

    public String getState() {
        return state;
    }

    public QbTorrentInfo setState(String state) {
        this.state = state;
        return this;
    }

    public Boolean getSuperSeeding() {
        return superSeeding;
    }

    public QbTorrentInfo setSuperSeeding(Boolean superSeeding) {
        this.superSeeding = superSeeding;
        return this;
    }

    public String getTags() {
        return tags;
    }

    public QbTorrentInfo setTags(String tags) {
        this.tags = tags;
        return this;
    }

    public Long getTimeActive() {
        return timeActive;
    }

    public QbTorrentInfo setTimeActive(Long timeActive) {
        this.timeActive = timeActive;
        return this;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public QbTorrentInfo setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
        return this;
    }

    public String getTracker() {
        return tracker;
    }

    public QbTorrentInfo setTracker(String tracker) {
        this.tracker = tracker;
        return this;
    }

    public Long getUploadLimit() {
        return uploadLimit;
    }

    public QbTorrentInfo setUploadLimit(Long uploadLimit) {
        this.uploadLimit = uploadLimit;
        return this;
    }

    public Long getUploaded() {
        return uploaded;
    }

    public QbTorrentInfo setUploaded(Long uploaded) {
        this.uploaded = uploaded;
        return this;
    }

    public Long getUploadedSession() {
        return uploadedSession;
    }

    public QbTorrentInfo setUploadedSession(Long uploadedSession) {
        this.uploadedSession = uploadedSession;
        return this;
    }

    public Long getUploadSpeed() {
        return uploadSpeed;
    }

    public QbTorrentInfo setUploadSpeed(Long uploadSpeed) {
        this.uploadSpeed = uploadSpeed;
        return this;
    }
}
