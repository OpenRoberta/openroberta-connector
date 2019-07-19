package de.fhg.iais.roberta.util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for version comparison.
 * Somewhat based on Semantic Versioning: https://semver.org/
 * Shorter prerelease names are preferred.
 * For prerelease names with the same length the first one is preferred.
 */
public final class Version implements Comparable<Version> {

    public final int major;
    public final int minor;
    public final int patch;

    public final String preReleaseName;
    public final int preReleaseVersion;
    public final boolean isSnapshot;

    private Version(int major, int minor, int patch, String preReleaseName, int preReleaseVersion, boolean isSnapshot) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preReleaseName = preReleaseName;
        this.preReleaseVersion = preReleaseVersion;
        this.isSnapshot = isSnapshot;
    }

    /**
     * Returns a version object based on the input string. The string must be in the
     * format vX.X.X[-prerelease.X] where the X are integers and prerelease is optional.
     *
     * @param s the version tag in the correct format
     * @return a version object
     * @throws IllegalArgumentException if the version string is invalid
     */
    public static Version valueOf(String s) {
        Matcher matcher = Pattern.compile("v(\\d+)\\.(\\d+)\\.(\\d+)(-(\\w*)\\.(\\d+))?(-SNAPSHOT$|$)").matcher(s);
        if ( matcher.find() ) {

            int major = Integer.valueOf(matcher.group(1));
            int minor = Integer.valueOf(matcher.group(2));
            int patch = Integer.valueOf(matcher.group(3));
            String preReleaseName = (matcher.group(4) == null) ? "" : matcher.group(5);
            int preReleaseVersion = (matcher.group(4) == null) ? -1 : Integer.valueOf(matcher.group(6));
            boolean isSnapshot = !matcher.group(7).isEmpty();

            return new Version(major, minor, patch, preReleaseName, preReleaseVersion, isSnapshot);
        } else {
            throw new IllegalArgumentException("Version string is invalid.");
        }
    }

    @Override
    public int compareTo(Version t) {
        if ( this.major > t.major ) { // major is larger
            return 1;
        } else if ( this.major < t.major ) {
            return -1;
        } else {
            if ( this.minor > t.minor ) { // minor is larger
                return 1;
            } else if ( this.minor < t.minor ) {
                return -1;
            } else {
                if ( this.patch > t.patch ) { // patch is larger
                    return 1;
                } else if ( this.patch < t.patch ) {
                    return -1;
                } else {
                    if ( this.preReleaseName.equals(t.preReleaseName) ) { // only compare same prereleases
                        if ( this.preReleaseVersion > t.preReleaseVersion ) { // prerelease is larger
                            return 1;
                        } else if ( this.preReleaseVersion < t.preReleaseVersion ) {
                            return -1;
                        }
                    } else if ( this.preReleaseName.length() < t.preReleaseName.length() ) { // prefer shorter or no prerelease
                        return 1;
                    } else if ( this.preReleaseName.length() > t.preReleaseName.length() ) {
                        return -1;
                    } else {
                        return 1; // just prefer by insertion order for same prerelease name lengths
                    }
                }
            }
        }
        if (!this.isSnapshot && t.isSnapshot) {
            return 1;
        } else if (this.isSnapshot && !t.isSnapshot) {
            return -1;
        }

        return 0; // if nothing else applies they are equal
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( (obj == null) || (this.getClass() != obj.getClass()) ) {
            return false;
        }
        Version version = (Version) obj;
        return (this.major == version.major)
               && (this.minor == version.minor)
               && (this.patch == version.patch)
               && (this.preReleaseVersion
                   == version.preReleaseVersion)
               && (this.isSnapshot == version.isSnapshot)
               && Objects.equals(this.preReleaseName, version.preReleaseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.major, this.minor, this.patch, this.preReleaseName, this.preReleaseVersion, this.isSnapshot);
    }

    @Override
    public String toString() {
        String s = "v" + this.major + '.' + this.minor + '.' + this.patch;
        if (!this.preReleaseName.isEmpty()) {
            s += '-' + this.preReleaseName + '.' + this.preReleaseVersion;
        }
        if (this.isSnapshot) {
            s+= "-SNAPSHOT";
        }
        return s;
    }
}
