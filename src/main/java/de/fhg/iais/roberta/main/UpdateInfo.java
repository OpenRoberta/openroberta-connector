package de.fhg.iais.roberta.main;

/**
 * Container for information about updates.
 */
public class UpdateInfo {
    /**
     * Used for the state of the update.
     */
    public enum Status {
        NEWER_VERSION,
        SAME_VERSION,
        OLDER_VERSION,
        NOT_OK,
        TIMEOUT
    }

    private final Status status;
    private final String name;
    private final String url;

    /**
     * Simple constructor used for failed update requests.
     * @param status the status of the update
     */
    public UpdateInfo(Status status) {
        if ( (status == Status.NEWER_VERSION) || (status == Status.SAME_VERSION) || (status == Status.OLDER_VERSION) ) {
            throw new IllegalArgumentException("Only use this constructor for failed requests");
        }
        this.status = status;
        this.name = "";
        this.url = "";
    }

    /**
     * Constructor for update info.
     * @param status the status of the update
     * @param name the name of the update
     * @param url the website url of the update
     */
    public UpdateInfo(Status status, String name, String url) {
        this.status = status;
        this.name = name;
        this.url = url;
    }

    public Status getStatus() {
        return this.status;
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

    @Override
    public String toString() {
        return "UpdateInfo{" + "status=" + this.status + ", name='" + this.name + '\'' + ", url='" + this.url + '\'' + '}';
    }
}
