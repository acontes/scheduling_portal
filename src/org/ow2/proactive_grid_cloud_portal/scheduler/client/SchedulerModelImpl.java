/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive_grid_cloud_portal.scheduler.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ow2.proactive_grid_cloud_portal.common.client.Listeners.LogListener;
import org.ow2.proactive_grid_cloud_portal.common.client.Listeners.StatsListener;
import org.ow2.proactive_grid_cloud_portal.common.client.Model.StatHistory.Range;
import org.ow2.proactive_grid_cloud_portal.scheduler.client.SchedulerListeners.JobOutputListener;
import org.ow2.proactive_grid_cloud_portal.scheduler.client.SchedulerListeners.JobSelectedListener;
import org.ow2.proactive_grid_cloud_portal.scheduler.client.SchedulerListeners.JobsUpdatedListener;
import org.ow2.proactive_grid_cloud_portal.scheduler.client.SchedulerListeners.RemoteHintListener;
import org.ow2.proactive_grid_cloud_portal.scheduler.client.SchedulerListeners.SchedulerStatusListener;
import org.ow2.proactive_grid_cloud_portal.scheduler.client.SchedulerListeners.StatisticsListener;
import org.ow2.proactive_grid_cloud_portal.scheduler.client.SchedulerListeners.TasksUpdatedListener;
import org.ow2.proactive_grid_cloud_portal.scheduler.client.SchedulerListeners.UsersListener;
import org.ow2.proactive_grid_cloud_portal.scheduler.client.SchedulerListeners.VisualizationListener;
import org.ow2.proactive_grid_cloud_portal.scheduler.shared.JobVisuMap;
import org.ow2.proactive_grid_cloud_portal.scheduler.shared.SchedulerConfig;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;


/**
 * Writable Model, should only be used by the Controller
 *
 *
 * @author mschnoor
 *
 */
public class SchedulerModelImpl extends SchedulerModel implements SchedulerEventDispatcher {

    private boolean logged = false;
    private String login = null;
    private String sessionId = null;
    private SchedulerStatus schedulerStatus = SchedulerStatus.STARTED;
    private Map<Integer, Job> jobs = null;
    private long jobsRev = -1;
    private Job selectedJob = null;
    private List<Task> selectedTasks = null;
    private boolean tasksDirty = false;
    private HashMap<Integer, JobOutput> output = null;
    private HashSet<String> isLiveOutput = null;
    private List<RemoteHint> remoteHints = null;
    private HashMap<String, StringBuffer> liveOutput = null;
    private boolean fetchMyJobsOnly = false;
    private boolean fetchPending = true;
    private boolean fetchRunning = true;
    private boolean fetchFinished = true;
    private int currentJobPage = 0;
    private List<SchedulerUser> users = null;
    private HashMap<String, String> schedulerStats = null;
    private HashMap<String, String> accountStats = null;
    private Map<String, String> imagePath = null;
    private Map<String, JobVisuMap> visuMap = null;
    private Map<String, StatHistory> statistics = null;
    private Map<String, Range> requestedStatRange = null;

    private ArrayList<JobsUpdatedListener> jobsUpdatedListeners = null;
    private ArrayList<JobSelectedListener> jobSelectedListeners = null;
    private ArrayList<TasksUpdatedListener> tasksUpdatedListeners = null;
    private ArrayList<SchedulerStatusListener> schedulerStateListeners = null;
    private ArrayList<JobOutputListener> jobOutputListeners = null;
    private ArrayList<LogListener> logListeners = null;
    private ArrayList<UsersListener> usersListeners = null;
    private ArrayList<StatisticsListener> statisticsListeners = null;
    private ArrayList<RemoteHintListener> remoteHintListeners = null;
    private ArrayList<VisualizationListener> visuListeners = null;
    private ArrayList<StatsListener> statsListeners = null;

    SchedulerModelImpl() {
        super();

        this.output = new HashMap<Integer, JobOutput>();
        this.isLiveOutput = new HashSet<String>();
        this.remoteHints = new ArrayList<RemoteHint>();
        this.liveOutput = new HashMap<String, StringBuffer>();
        this.jobsUpdatedListeners = new ArrayList<JobsUpdatedListener>();
        this.jobSelectedListeners = new ArrayList<JobSelectedListener>();
        this.tasksUpdatedListeners = new ArrayList<TasksUpdatedListener>();
        this.schedulerStateListeners = new ArrayList<SchedulerStatusListener>();
        this.jobOutputListeners = new ArrayList<JobOutputListener>();
        this.logListeners = new ArrayList<LogListener>();
        this.usersListeners = new ArrayList<UsersListener>();
        this.statisticsListeners = new ArrayList<StatisticsListener>();
        this.remoteHintListeners = new ArrayList<RemoteHintListener>();
        this.visuListeners = new ArrayList<VisualizationListener>();
        this.statsListeners = new ArrayList<StatsListener>();
        this.imagePath = new HashMap<String, String>();
        this.visuMap = new HashMap<String, JobVisuMap>();
        this.requestedStatRange = new HashMap<String, Range>();
    }

    @Override
    public boolean isLoggedIn() {
        return this.logged;
    }

    void setLoggedIn(boolean loggedIn) {
        this.logged = loggedIn;
        this.sessionId = null;
    }

    @Override
    public String getLogin() {
        return this.login;
    }

    void setLogin(String login) {
        this.login = login;
    }

    @Override
    public String getSessionId() {
        return this.sessionId;
    }

    void setSessionId(String id) {
        this.sessionId = id;
    }

    @Override
    public SchedulerStatus getSchedulerStatus() {
        return this.schedulerStatus;
    }

    /**
     * Set a new scheduler status,
     * notify listeners
     * 
     * @param status the new scheduler status
     */
    void setSchedulerStatus(SchedulerStatus status) {
        this.schedulerStatus = status;

        for (SchedulerStatusListener listener : this.schedulerStateListeners) {
            listener.statusChanged(this.schedulerStatus);
        }
    }

    @Override
    public Map<Integer, Job> getJobs() {
        return this.jobs;
    }

    /**
     * Modifies the local joblist
     * triggers {@link JobsUpdatedListener#jobsUpdated(JobSet)},
     * or {@link JobsUpdatedListener#jobsUpdating()} if <code>jobs</code> was null
     * 
     * @param jobs a jobset, or null
     * @param rev the revision of this jobset
     */
    void setJobs(Map<Integer, Job> jobs, long rev) {
        this.jobs = jobs;
        this.jobsRev = rev;
        boolean empty = false;

        if (jobs == null) {
            empty = true;
            this.jobs = new HashMap<Integer, Job>();
        }

        for (JobsUpdatedListener listener : this.jobsUpdatedListeners) {
            listener.jobsUpdated(this.jobs);
            if (empty)
                listener.jobsUpdating();
        }
    }

    void jobsUpdated() {
        for (JobsUpdatedListener listener : this.jobsUpdatedListeners) {
            listener.jobsUpdated(this.jobs);
        }
    }

    void jobSubmitted(Job j) {
        for (JobsUpdatedListener listener : this.jobsUpdatedListeners) {
            listener.jobSubmitted(j);
        }
    }

    void jobsUpdating() {
        for (JobsUpdatedListener listener : this.jobsUpdatedListeners) {
            listener.jobsUpdating();
        }
    }

    /**
     * Modifies the Job selection,
     * triggers a JobSelected event
     *
     * @param jobId
     */
    void selectJob(int jobId) {
        Job j = null;
        // find the job
        for (Job it : this.jobs.values()) {
            if (it.getId() == jobId) {
                j = it;
            }
        }
        boolean selChanged = (this.selectedJob == null) ? true : !this.selectedJob.equals(j);
        this.selectedJob = j;

        // notify job selection listeners
        for (JobSelectedListener listener : this.jobSelectedListeners) {
            if (j == null)
                listener.jobUnselected();
            else
                listener.jobSelected(j);
        }

        // tasks list will change, notify tasks listeners
        for (TasksUpdatedListener list : this.tasksUpdatedListeners) {
            if (j == null)
                list.tasksUpdated(new ArrayList<Task>());
            else
                list.tasksUpdating(selChanged);
        }
    }

    @Override
    public Job getSelectedJob() {
        return this.selectedJob;
    }

    @Override
    public Job getJob(int jobId) {
        for (Job j : this.jobs.values()) {
            if (j.getId() == jobId) {
                return j;
            }
        }
        return null;
    }

    @Override
    public long getJobsRevision() {
        return this.jobsRev;
    }

    @Override
    public int getJobPageSize() {
        return SchedulerConfig.get().getJobsPageSize();
    }

    @Override
    public int getJobPage() {
        return this.currentJobPage;
    }

    /**
     * change current page
     * 
     * @param page new page number
     */
    void setJobPage(int page) {
        this.currentJobPage = page;
    }

    /**
     * Modifies the tasks set
     * triggers TasksUpdated event
     * 
     * @param tasks the new TaskSet
     */
    void setTasks(List<Task> tasks) {
        this.selectedTasks = tasks;
        for (TasksUpdatedListener list : this.tasksUpdatedListeners) {
            list.tasksUpdated(tasks);
        }
    }

    /**
     * Notify task updated listeners that updating failed
     * 
     * @param message the error message
     */
    void taskUpdateError(String message) {
        this.selectedTasks = new ArrayList<Task>();
        for (TasksUpdatedListener list : this.tasksUpdatedListeners) {
            list.tasksUpdatedFailure(message);
        }
    }

    @Override
    public List<Task> getTasks() {
        return this.selectedTasks;
    }

    @Override
    public boolean isTasksDirty() {
        return this.tasksDirty;
    }

    void setTasksDirty(boolean b) {
        this.tasksDirty = b;
    }

    @Override
    public JobOutput getJobOutput(int jobId) {
        JobOutput ret = this.output.get(jobId);
        return ret;
    }

    /**
     * Set the output for a given task in a given job
     * 
     * notify listeners
     * 
     * @param jobId
     * @param taskId
     * @param output
     */
    void setTaskOutput(int jobId, long finishedTime, String output) {
        JobStatus stat = null;
        for (Job j : this.jobs.values()) {
            if (jobId == j.getId())
                stat = j.getStatus();
        }
        if (stat == null) {
            throw new IllegalStateException("Trying to set output for a task in job " + jobId +
                " for which there is no local representation");
        }

        Map<Long, String> tasks = new HashMap<Long, String>();
        tasks.put(finishedTime, output);

        List<String> lines = new ArrayList<String>();

        for (String line : output.split("\n")) {
            if (line.contains("PA_REMOTE_CONNECTION")) {
                this.addRemoteHint(line);
            }
            if (line.matches("\\[.*\\].*")) {
                line = line.replaceFirst("]", "]</span>");
                line = "<nobr><span style='color:gray;'>" + line + "</nobr><br>";
            }

            if (line.trim().length() > 0) {
                lines.add(line);
            }
        }

        if (this.output.get(jobId) == null) {
            JobOutput jo = new JobOutput(jobId);
            jo.update(finishedTime, lines);
            this.output.put(jobId, jo);
        } else {
            this.output.get(jobId).update(finishedTime, lines);
        }

        this.updateOutput(jobId);
    }

    /**
     * Add a remote hint
     * will notify listeners if it is well formed
     * 
     * @param remoteHint a string containing PA_REMOTE_CONNECTION
     */
    void addRemoteHint(String remoteHint) {
        String[] expl = remoteHint.split("PA_REMOTE_CONNECTION");
        if (expl.length < 2)
            return;

        expl = expl[1].split(";");
        if (expl.length < 4)
            return;

        RemoteHint rh = new RemoteHint();
        rh.taskId = expl[1];
        rh.type = expl[2];
        rh.argument = expl[3];

        this.remoteHints.add(rh);

        for (RemoteHintListener rhl : this.remoteHintListeners) {
            rhl.remoteHintRead(rh);
        }
    }

    /**
     * Notify listeners that the output of a given job has changed
     * 
     * @param jobId the job for which the output changed
     */
    void updateOutput(int jobId) {
        if (this.output.get(jobId) == null) {
            JobOutput jo = new JobOutput(jobId);
            this.output.put(jobId, jo);
        }

        for (JobOutputListener listener : this.jobOutputListeners) {
            listener.jobOutputUpdated(this.output.get(jobId));
        }
    }

    /**
     * Append a job output fragment to the stored live output
     * @param jobId id of the job to which this fragment belongs
     * @param out job output fragment
     */
    void appendLiveOutput(String jobId, String out) {
        String[] expl = out.split("\n");
        out = "";
        for (String str : expl) {
            if (str.contains("PA_REMOTE_CONNECTION")) {
                this.addRemoteHint(str);
            }
            if (str.matches("\\[.*\\].*")) {
                str = str.replaceFirst("]", "]</span>");
                out += "<nobr><span style='color:gray;'>" + str + "</nobr><br>";
            }
        }

        StringBuffer buf = this.liveOutput.get(jobId);
        if (buf == null) {
            buf = new StringBuffer();
            this.liveOutput.put(jobId, buf);
        }
        buf.append(out);

        for (JobOutputListener list : this.jobOutputListeners) {
            list.liveOutputUpdated(jobId, buf.toString());
        }
    }

    @Override
    public String getLiveOutput(String jobId) {
        StringBuffer buf = this.liveOutput.get(jobId);
        if (buf == null) {
            return "";
        } else {
            return buf.toString();
        }
    }

    @Override
    public boolean isLiveOutput(String jobId) {
        return this.isLiveOutput.contains(jobId);
    }

    /**
     * The output for this job should be fetched live
     * @param jobId id of the job
     * @param isLiveOutput true to live fetch
     */
    void setLiveOutput(String jobId, boolean isLiveOutput) {
        if (isLiveOutput) {
            this.isLiveOutput.add(jobId);
            this.liveOutput.put(jobId, new StringBuffer());
        } else {
            this.isLiveOutput.remove(jobId);
        }
    }

    @Override
    public List<RemoteHint> getRemoteHints() {
        return this.remoteHints;
    }

    @Override
    public boolean isFetchMyJobsOnly() {
        return fetchMyJobsOnly;
    }

    void fetchMyJobsOnly(boolean b) {
        this.fetchMyJobsOnly = b;
    }

    @Override
    public boolean isFetchPendingJobs() {
        return this.fetchPending;
    }

    void fetchPending(boolean f) {
        this.fetchPending = f;
    }

    @Override
    public boolean isFetchRunningJobs() {
        return this.fetchRunning;
    }

    void fetchRunning(boolean f) {
        this.fetchRunning = f;
    }

    @Override
    public boolean isFetchFinishedJobs() {
        return this.fetchFinished;
    }

    void fetchFinished(boolean f) {
        this.fetchFinished = f;
    }

    @Override
    public String getJobImagePath(String jobId) {
        return this.imagePath.get(jobId);
    }

    void setJobImagePath(String jobId, String path) {
        this.imagePath.put(jobId, path);

        for (VisualizationListener list : this.visuListeners) {
            list.imageUpdated(jobId, path);
        }
    }

    void visuUnavailable(String jobId) {
        for (VisualizationListener list : visuListeners) {
            list.visualizationUnavailable(jobId);
        }
    }

    @Override
    public JobVisuMap getJobVisuMap(String jobId) {
        return this.visuMap.get(jobId);
    }

    void setJobVisuMap(String jobId, JobVisuMap map) {
        this.visuMap.put(jobId, map);

        for (VisualizationListener list : this.visuListeners) {
            list.mapUpdated(jobId, map);
        }
    }

    @Override
    public List<SchedulerUser> getSchedulerUsers() {
        return this.users;
    }

    /**
     * Change the local users list, notify listeners
     * 
     * @param users new users
     */
    void setSchedulerUsers(List<SchedulerUser> users) {
        this.users = users;

        for (UsersListener list : this.usersListeners) {
            list.usersUpdated(this.users);
        }
    }

    /**
     * Set local model, notify listeners
     * 
     * @param stats
     */
    void setAccountStatistics(HashMap<String, String> stats) {
        this.accountStats = stats;
        for (StatisticsListener list : this.statisticsListeners) {
            list.accountStatsUpdated(stats);
        }
    }

    @Override
    public HashMap<String, String> getAccountStatistics() {
        return this.accountStats;
    }

    void setSchedulerStatistics(HashMap<String, String> stats) {
        this.schedulerStats = stats;
        for (StatisticsListener list : this.statisticsListeners) {
            list.schedulerStatsUpdated(stats);
        }
    }

    @Override
    public HashMap<String, String> getSchedulerStatistics() {
        return this.schedulerStats;
    }

    @Override
    public StatHistory getStatHistory(String source) {
        return this.statistics.get(source);
    }

    @Override
    public Map<String, StatHistory> getStatHistory() {
        return this.statistics;
    }

    void setStatistics(Map<String, StatHistory> values) {
        this.statistics = values;
        for (StatsListener list : this.statsListeners) {
            list.statsUpdated(values);
        }
    }

    @Override
    public Range getRequestedStatHistoryRange(String source) {
        Range r = this.requestedStatRange.get(source);
        if (r == null)
            return Range.MINUTE_1;
        return r;
    }

    void setRequestedStatisticsRange(String source, Range r) {
        this.requestedStatRange.put(source, r);
    }

    @Override
    public void logMessage(String message) {
        for (LogListener list : this.logListeners) {
            list.logMessage(getLogStamp() + message);
        }
    }

    @Override
    public void logImportantMessage(String error) {
        for (LogListener list : this.logListeners) {
            list.logImportantMessage(getLogStamp() + "<span style='color:#8f7601;'>" + error + "</span>");
        }
    }

    @Override
    public void logCriticalMessage(String error) {
        for (LogListener list : this.logListeners) {
            list.logCriticalMessage(getLogStamp() + "<span style='color:red;'>" + error + "</span>");
        }
    }

    private String getLogStamp() {
        String date = DateTimeFormat.getFormat(PredefinedFormat.TIME_LONG).format(new Date());
        return "<span style='color:gray'>" + date + "</span> ";
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.client.EventDispatcher#addJobsUpdatedListener(org.ow2.proactive_grid_cloud_portal.client.Listeners.JobsUpdatedListener)
     */
    public void addJobsUpdatedListener(JobsUpdatedListener listener) {
        this.jobsUpdatedListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.client.EventDispatcher#addJobSelectedListener(org.ow2.proactive_grid_cloud_portal.client.Listeners.JobSelectedListener)
     */
    public void addJobSelectedListener(JobSelectedListener listener) {
        this.jobSelectedListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.client.EventDispatcher#addTasksUpdatedListener(org.ow2.proactive_grid_cloud_portal.client.Listeners.TasksUpdatedListener)
     */
    public void addTasksUpdatedListener(TasksUpdatedListener listener) {
        this.tasksUpdatedListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.client.EventDispatcher#addSchedulerStateListener(org.ow2.proactive_grid_cloud_portal.client.Listeners.SchedulerStateListener)
     */
    public void addSchedulerStatusListener(SchedulerStatusListener listener) {
        this.schedulerStateListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.client.EventDispatcher#addJobOutputListener(org.ow2.proactive_grid_cloud_portal.client.Listeners.JobOutputListener)
     */
    public void addJobOutputListener(JobOutputListener listener) {
        this.jobOutputListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.client.EventDispatcher#addLogListener(org.ow2.proactive_grid_cloud_portal.client.Listeners.LogListener)
     */
    public void addLogListener(LogListener listener) {
        this.logListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.client.EventDispatcher#addUsersListener(org.ow2.proactive_grid_cloud_portal.client.Listeners.UsersListener)
     */
    public void addUsersListener(UsersListener listener) {
        this.usersListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.client.EventDispatcher#addStatisticsListener(org.ow2.proactive_grid_cloud_portal.client.Listeners.StatisticsListener)
     */
    public void addStatisticsListener(StatisticsListener listener) {
        this.statisticsListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.client.EventDispatcher#addRemoteHintListener(org.ow2.proactive_grid_cloud_portal.client.Listeners.RemoteHintListener)
     */
    public void addRemoteHintListener(RemoteHintListener listener) {
        this.remoteHintListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.ow2.proactive_grid_cloud_portal.client.EventDispatcher#addVisualizationListener(org.ow2.proactive_grid_cloud_portal.client.Listeners.VisualizationListener)
     */
    public void addVisualizationListener(VisualizationListener listener) {
        this.visuListeners.add(listener);
    }

    @Override
    public void addStatsListener(StatsListener listener) {
        this.statsListeners.add(listener);
    }

    /**
     * Nulls references to all known listeners
     */
    void clearListeners() {
        this.jobsUpdatedListeners.clear();
        this.jobSelectedListeners.clear();
        this.tasksUpdatedListeners.clear();
        this.schedulerStateListeners.clear();
        this.jobOutputListeners.clear();
        this.logListeners.clear();
        this.usersListeners.clear();
        this.remoteHintListeners.clear();
        this.visuListeners.clear();
        this.statsListeners.clear();
    }
}
