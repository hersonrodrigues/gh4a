/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gh4a;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.gh4a.fragment.SettingsFragment;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CollaboratorService;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DownloadService;
import org.eclipse.egit.github.core.service.EventService;
import org.eclipse.egit.github.core.service.GistService;
import org.eclipse.egit.github.core.service.GitHubService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.LabelService;
import org.eclipse.egit.github.core.service.MarkdownService;
import org.eclipse.egit.github.core.service.MilestoneService;
import org.eclipse.egit.github.core.service.NotificationService;
import org.eclipse.egit.github.core.service.OrganizationService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.StarService;
import org.eclipse.egit.github.core.service.UserService;
import org.eclipse.egit.github.core.service.WatcherService;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.HashMap;

import io.fabric.sdk.android.Fabric;

/**
 * The Class Gh4Application.
 */
public class Gh4Application extends Application implements OnSharedPreferenceChangeListener {
    public static final String LOG_TAG = "Gh4a";
    public static int THEME = R.style.LightTheme;

    public static final String COLLAB_SERVICE = "github.collaborator";
    public static final String COMMIT_SERVICE = "github.commit";
    public static final String CONTENTS_SERVICE = "github.contents";
    public static final String DOWNLOAD_SERVICE = "github.download";
    public static final String EVENT_SERVICE = "github.event";
    public static final String GIST_SERVICE = "github.gist";
    public static final String ISSUE_SERVICE = "github.issue";
    public static final String LABEL_SERVICE = "github.label";
    public static final String MARKDOWN_SERVICE = "github.markdown";
    public static final String MILESTONE_SERVICE = "github.milestone";
    public static final String NOTIFICATION_SERVICE = "github.notification";
    public static final String ORG_SERVICE = "github.organization";
    public static final String PULL_SERVICE = "github.pullrequest";
    public static final String REPO_SERVICE = "github.repository";
    public static final String STAR_SERVICE = "github.star";
    public static final String USER_SERVICE = "github.user";
    public static final String WATCHER_SERVICE = "github.watcher";

    private static Gh4Application sInstance;
    private GitHubClient mClient;
    private HashMap<String, GitHubService> mServices;
    private PrettyTime mPt;

    private static final int THEME_DARK = 0;
    private static final int THEME_LIGHT = 1;

    private static final String KEY_LOGIN = "USER_LOGIN";
    private static final String KEY_TOKEN = "Token";

    private static final int MAX_TRACKED_URLS = 5;
    private static int sNextUrlTrackingPosition = 0;
    private static boolean sHasCrashlytics;

    /*
     * (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        SharedPreferences prefs = getPrefs();
        selectTheme(prefs.getInt(SettingsFragment.KEY_THEME, THEME_LIGHT));
        prefs.registerOnSharedPreferenceChangeListener(this);

        boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        sHasCrashlytics = !isDebuggable && !TextUtils.equals(Build.DEVICE, "sdk");
        if (sHasCrashlytics) {
            Fabric.with(this, new Crashlytics());
        }

        mPt = new PrettyTime();

        mClient = new DefaultClient();
        mClient.setOAuth2Token(getAuthToken());

        mServices = new HashMap<>();
        mServices.put(COLLAB_SERVICE, new CollaboratorService(mClient));
        mServices.put(COMMIT_SERVICE, new CommitService(mClient));
        mServices.put(CONTENTS_SERVICE, new ContentsService(mClient));
        mServices.put(DOWNLOAD_SERVICE, new DownloadService(mClient));
        mServices.put(EVENT_SERVICE, new EventService(mClient));
        mServices.put(GIST_SERVICE, new GistService(mClient));
        mServices.put(ISSUE_SERVICE, new IssueService(mClient));
        mServices.put(LABEL_SERVICE, new LabelService(mClient));
        mServices.put(MARKDOWN_SERVICE, new MarkdownService(mClient));
        mServices.put(MILESTONE_SERVICE, new MilestoneService(mClient));
        mServices.put(NOTIFICATION_SERVICE, new NotificationService(mClient));
        mServices.put(ORG_SERVICE, new OrganizationService(mClient));
        mServices.put(PULL_SERVICE, new PullRequestService(mClient));
        mServices.put(REPO_SERVICE, new RepositoryService(mClient));
        mServices.put(STAR_SERVICE, new StarService(mClient));
        mServices.put(USER_SERVICE, new UserService(mClient));
        mServices.put(WATCHER_SERVICE, new WatcherService(mClient));
    }

    public GitHubService getService(String name) {
        return mServices.get(name);
    }

    private void selectTheme(int theme) {
        switch (theme) {
            case THEME_DARK:
                THEME = R.style.DarkTheme;
                break;
            case THEME_LIGHT:
            case 2: /* for backwards compat with old settings, was light-dark theme */
                THEME = R.style.LightTheme;
                break;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mPt = new PrettyTime(newConfig.getLocales().get(0));
        } else {
            mPt = new PrettyTime(newConfig.locale);
        }
    }

    /* package */ static void trackVisitedUrl(String url) {
        synchronized (Gh4Application.class) {
            if (sHasCrashlytics) {
                Crashlytics.setString("github-url-" + sNextUrlTrackingPosition, url);
                Crashlytics.setInt("last-url-position", sNextUrlTrackingPosition);
                if (++sNextUrlTrackingPosition >= MAX_TRACKED_URLS) {
                    sNextUrlTrackingPosition = 0;
                }
            }
        }
    }

    public PrettyTime getPrettyTimeInstance() {
        return mPt;
    }

    public String getAuthLogin() {
        return getPrefs().getString(KEY_LOGIN, null);
    }

    public String getAuthToken() {
        return getPrefs().getString(KEY_TOKEN, null);
    }

    public void setLogin(String login, String token) {
        getPrefs().edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_LOGIN, login)
                .apply();
    }

    public void logout() {
        setLogin(null, null);
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(SettingsFragment.PREF_NAME, MODE_PRIVATE);
    }

    public static Gh4Application get() {
        return sInstance;
    }

    public boolean isAuthorized() {
        return getAuthLogin() != null && getAuthToken() != null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_TOKEN)) {
            mClient.setOAuth2Token(getAuthToken());
        } else if (key.equals(SettingsFragment.KEY_THEME)) {
            selectTheme(sharedPreferences.getInt(key, THEME_LIGHT));
        }
    }
}
