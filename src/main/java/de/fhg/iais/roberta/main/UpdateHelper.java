package de.fhg.iais.roberta.main;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import de.fhg.iais.roberta.main.UpdateInfo.Status;
import de.fhg.iais.roberta.util.PropertyHelper;
import de.fhg.iais.roberta.util.Version;

public final class UpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateHelper.class);

    private static final String GITHUB_API = "https://api.github.com/repos/";
    private static final String LATEST_RELEASE = "releases/latest";

    private UpdateHelper() {
    }

    public static UpdateInfo checkForUpdates() {
        Version currentVersion = Version.valueOf('v' + PropertyHelper.getInstance().getProperty("version"));
        LOG.info("Current version: {}", currentVersion);

        try (AsyncHttpClient client = Dsl.asyncHttpClient()) {
            ListenableFuture<UpdateInfo>
                repository =
                client.prepareGet(GITHUB_API + PropertyHelper.getInstance().getProperty("repository") + LATEST_RELEASE)
                      .execute(new UpdateInfoAsyncCompletionHandler(currentVersion));
            return repository.get();
        } catch ( IOException e ) {
            LOG.error("Could not close async client: {}", e.getMessage());
        } catch ( InterruptedException e ) {
            LOG.error("Update request was interrupted: {}", e.getMessage());
        } catch ( ExecutionException e ) {
            LOG.error("Something went wrong when checking for updates: {}", e.getMessage());
        }

        return new UpdateInfo(Status.TIMEOUT);
    }

    private static class UpdateInfoAsyncCompletionHandler extends AsyncCompletionHandler<UpdateInfo> {
        private final Version currentVersion;

        UpdateInfoAsyncCompletionHandler(Version currentVersion) {
            this.currentVersion = currentVersion;
        }

        @Override
        public UpdateInfo onCompleted(Response response) {
            if ( response.getStatusCode() == 200 ) {
                JSONObject jsonObject = new JSONObject(response.getResponseBody());

                String tagName = jsonObject.getString("tag_name");
                Version availableVersion = Version.valueOf(tagName);

                Status status;
                if ( this.currentVersion.compareTo(availableVersion) < 0 ) {
                    status = Status.NEWER_VERSION;
                    LOG.info("A newer version is available");
                } else if ( this.currentVersion.compareTo(availableVersion) > 0 ) {
                    status = Status.OLDER_VERSION;
                    LOG.info("This version is newer than the newest available");
                } else {
                    status = Status.SAME_VERSION;
                    LOG.info("The newest version is installed");
                }
                String name = jsonObject.getString("name");
                String htmlUrl = jsonObject.getString("html_url");

                return new UpdateInfo(status, name, htmlUrl);
            } else {
                return new UpdateInfo(Status.NOT_OK);
            }
        }
    }
}
