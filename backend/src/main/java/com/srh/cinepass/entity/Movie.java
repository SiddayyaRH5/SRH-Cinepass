package com.srh.cinepass.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A movie in the catalogue.
 *
 * <p>The lifecycle and provenance fields below were added for the TMDB
 * automation work. Every one of them is nullable on purpose: pre-existing rows
 * stay readable and untouched. A {@code null} {@link #status} behaves as
 * {@link MovieStatus#ACTIVE} and a {@code null} {@link #source} behaves as
 * {@link MovieSource#MANUAL}.</p>
 */
@Entity
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tmdbId;
    private String title;
    private String description;
    private String posterUrl;
    private String backdropUrl;
    private LocalDate releaseDate;
    private Double rating;
    private Integer duration;
    private String language;
    private String genre;

    /*
     * ---------------------------------------------------------------
     * Lifecycle and provenance (additive, nullable, backward compatible)
     * ---------------------------------------------------------------
     */

    /**
     * Customer-facing lifecycle state. {@code null} means ACTIVE for rows
     * created before this feature existed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private MovieStatus status;

    /**
     * Whether the row is managed by an administrator or by the TMDB
     * catalogue sync. {@code null} means MANUAL for legacy rows.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20)
    private MovieSource source;

    /**
     * The Indian theatrical release date taken from TMDB. This is the date
     * used for show eligibility and for retirement. It is never a worldwide,
     * digital, streaming, physical or TV release date.
     */
    @Column(name = "india_theatrical_release_date")
    private LocalDate indiaTheatricalReleaseDate;

    /**
     * When {@code true} the scheduler must not create new shows for this
     * movie. The movie stays visible in the catalogue.
     */
    @Column(name = "exclude_from_auto_shows")
    private Boolean excludeFromAutoShows;

    /**
     * Why an administrator excluded this movie from automatic show
     * generation.
     */
    @Column(name = "exclusion_reason", length = 500)
    private String exclusionReason;

    /**
     * When {@code true} the automatic retirement job must never archive this
     * movie.
     */
    @Column(name = "admin_protected")
    private Boolean adminProtected;

    /**
     * Optional administrator-managed end of the active period. While the
     * business date is on or before this date the movie is not retired.
     */
    @Column(name = "active_until")
    private LocalDate activeUntil;

    /**
     * Why the automatic retirement job postponed retiring this movie. It is
     * cleared once the movie is actually archived.
     */
    @Column(name = "retirement_hold_reason", length = 500)
    private String retirementHoldReason;

    /**
     * When the movie was archived. The movie row itself is never deleted.
     */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /**
     * When the movie metadata was last refreshed from TMDB.
     */
    @Column(name = "tmdb_synced_at")
    private LocalDateTime tmdbSyncedAt;

    /**
     * The last error recorded while synchronising this movie. It never
     * contains credentials; it holds a short, safe description only.
     */
    @Column(name = "last_sync_error", length = 1000)
    private String lastSyncError;

    public Movie() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTmdbId() { return tmdbId; }
    public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getBackdropUrl() { return backdropUrl; }
    public void setBackdropUrl(String backdropUrl) { this.backdropUrl = backdropUrl; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    /**
     * The lifecycle state. Exposed to the public catalogue API so the
     * frontend can tell the customer that a deep-linked movie is archived.
     */
    public MovieStatus getStatus() { return status; }
    public void setStatus(MovieStatus status) { this.status = status; }

    @JsonIgnore
    public MovieSource getSource() { return source; }
    public void setSource(MovieSource source) { this.source = source; }

    @JsonIgnore
    public LocalDate getIndiaTheatricalReleaseDate() { return indiaTheatricalReleaseDate; }
    public void setIndiaTheatricalReleaseDate(LocalDate indiaTheatricalReleaseDate) {
        this.indiaTheatricalReleaseDate = indiaTheatricalReleaseDate;
    }

    @JsonIgnore
    public Boolean getExcludeFromAutoShows() { return excludeFromAutoShows; }
    public void setExcludeFromAutoShows(Boolean excludeFromAutoShows) {
        this.excludeFromAutoShows = excludeFromAutoShows;
    }

    @JsonIgnore
    public String getExclusionReason() { return exclusionReason; }
    public void setExclusionReason(String exclusionReason) {
        this.exclusionReason = exclusionReason;
    }

    @JsonIgnore
    public Boolean getAdminProtected() { return adminProtected; }
    public void setAdminProtected(Boolean adminProtected) {
        this.adminProtected = adminProtected;
    }

    @JsonIgnore
    public LocalDate getActiveUntil() { return activeUntil; }
    public void setActiveUntil(LocalDate activeUntil) { this.activeUntil = activeUntil; }

    @JsonIgnore
    public String getRetirementHoldReason() { return retirementHoldReason; }
    public void setRetirementHoldReason(String retirementHoldReason) {
        this.retirementHoldReason = retirementHoldReason;
    }

    @JsonIgnore
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }

    @JsonIgnore
    public LocalDateTime getTmdbSyncedAt() { return tmdbSyncedAt; }
    public void setTmdbSyncedAt(LocalDateTime tmdbSyncedAt) { this.tmdbSyncedAt = tmdbSyncedAt; }

    @JsonIgnore
    public String getLastSyncError() { return lastSyncError; }
    public void setLastSyncError(String lastSyncError) { this.lastSyncError = lastSyncError; }

    // -------------------------------------------------------------------
    // Null-safe helpers used by the services and the admin API.
    // -------------------------------------------------------------------

    @JsonIgnore
    public boolean isArchived() {
        return status == MovieStatus.ARCHIVED;
    }

    @JsonIgnore
    public boolean isActive() {
        return status == null || status == MovieStatus.ACTIVE;
    }

    @JsonIgnore
    public boolean isExcludedFromAutoShows() {
        return Boolean.TRUE.equals(excludeFromAutoShows);
    }

    @JsonIgnore
    public boolean isAdminProtected() {
        return Boolean.TRUE.equals(adminProtected);
    }

    @JsonIgnore
    public boolean isTmdbManaged() {
        return source == MovieSource.TMDB;
    }
}
