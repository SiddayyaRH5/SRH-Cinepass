package com.srh.cinepass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One execution of the daily automation job.
 *
 * <p>Rows are append-only audit records. They store counters and a short
 * message so an administrator can see what the scheduler did, how long it
 * took and whether it failed. No credential, token or customer data is ever
 * stored here.</p>
 */
@Entity
@Table(name = "automation_run")
public class AutomationRun {

    /** Triggered by the scheduled 2 AM IST job. */
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";

    /** Triggered by an administrator through the admin API. */
    public static final String TRIGGER_MANUAL = "MANUAL";

    /** Every phase completed successfully. */
    public static final String OUTCOME_SUCCESS = "SUCCESS";

    /** The run completed but at least one phase reported problems. */
    public static final String OUTCOME_PARTIAL = "PARTIAL";

    /** The run failed before it could finish its work. */
    public static final String OUTCOME_FAILED = "FAILED";

    /** The run did not execute, for example because another instance held the lease. */
    public static final String OUTCOME_SKIPPED = "SKIPPED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trigger_type", length = 20, nullable = false)
    private String triggerType;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "outcome", length = 20, nullable = false)
    private String outcome;

    @Column(name = "pages_processed")
    private Integer pagesProcessed;

    @Column(name = "movies_discovered")
    private Integer moviesDiscovered;

    @Column(name = "movies_inserted")
    private Integer moviesInserted;

    @Column(name = "movies_updated")
    private Integer moviesUpdated;

    @Column(name = "movies_skipped")
    private Integer moviesSkipped;

    @Column(name = "movies_archived")
    private Integer moviesArchived;

    @Column(name = "movies_protected")
    private Integer moviesProtected;

    @Column(name = "shows_created")
    private Integer showsCreated;

    @Column(name = "shows_skipped")
    private Integer showsSkipped;

    @Column(name = "shows_failed")
    private Integer showsFailed;

    @Column(name = "message", length = 1000)
    private String message;

    public AutomationRun() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public Integer getPagesProcessed() { return pagesProcessed; }
    public void setPagesProcessed(Integer pagesProcessed) { this.pagesProcessed = pagesProcessed; }
    public Integer getMoviesDiscovered() { return moviesDiscovered; }
    public void setMoviesDiscovered(Integer moviesDiscovered) { this.moviesDiscovered = moviesDiscovered; }
    public Integer getMoviesInserted() { return moviesInserted; }
    public void setMoviesInserted(Integer moviesInserted) { this.moviesInserted = moviesInserted; }
    public Integer getMoviesUpdated() { return moviesUpdated; }
    public void setMoviesUpdated(Integer moviesUpdated) { this.moviesUpdated = moviesUpdated; }
    public Integer getMoviesSkipped() { return moviesSkipped; }
    public void setMoviesSkipped(Integer moviesSkipped) { this.moviesSkipped = moviesSkipped; }
    public Integer getMoviesArchived() { return moviesArchived; }
    public void setMoviesArchived(Integer moviesArchived) { this.moviesArchived = moviesArchived; }
    public Integer getMoviesProtected() { return moviesProtected; }
    public void setMoviesProtected(Integer moviesProtected) { this.moviesProtected = moviesProtected; }
    public Integer getShowsCreated() { return showsCreated; }
    public void setShowsCreated(Integer showsCreated) { this.showsCreated = showsCreated; }
    public Integer getShowsSkipped() { return showsSkipped; }
    public void setShowsSkipped(Integer showsSkipped) { this.showsSkipped = showsSkipped; }
    public Integer getShowsFailed() { return showsFailed; }
    public void setShowsFailed(Integer showsFailed) { this.showsFailed = showsFailed; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
