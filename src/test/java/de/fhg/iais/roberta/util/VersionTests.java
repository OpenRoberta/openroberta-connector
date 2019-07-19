package de.fhg.iais.roberta.util;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VersionTests {

    @Test
    void valueOf_ShouldReturnCorrectVersion_WhenValidKeyIsGiven() {
        Version v0 = Version.valueOf("v1.0.0");
        assertThat(v0.major, is(1));
        assertThat(v0.minor, is(0));
        assertThat(v0.patch, is(0));
        assertThat(v0.preReleaseName, emptyString());
        assertThat(v0.preReleaseVersion, is(-1));
        assertThat(v0.isSnapshot, is(false));

        Version v1 = Version.valueOf("v123.456.7890");
        assertThat(v1.major, is(123));
        assertThat(v1.minor, is(456));
        assertThat(v1.patch, is(7890));
        assertThat(v1.preReleaseName, emptyString());
        assertThat(v1.preReleaseVersion, is(-1));
        assertThat(v1.isSnapshot, is(false));

        Version v2 = Version.valueOf("v1.0.0-beta.1");
        assertThat(v2.major, is(1));
        assertThat(v2.minor, is(0));
        assertThat(v2.patch, is(0));
        assertThat(v2.preReleaseName, is("beta"));
        assertThat(v2.preReleaseVersion, is(1));
        assertThat(v2.isSnapshot, is(false));

        Version v3 = Version.valueOf("v1.0.0-beta.151");
        assertThat(v3.major, is(1));
        assertThat(v3.minor, is(0));
        assertThat(v3.patch, is(0));
        assertThat(v3.preReleaseName, is("beta"));
        assertThat(v3.preReleaseVersion, is(151));
        assertThat(v3.isSnapshot, is(false));

        Version v4 = Version.valueOf("v1.0.0-beta.151-SNAPSHOT");
        assertThat(v4.major, is(1));
        assertThat(v4.minor, is(0));
        assertThat(v4.patch, is(0));
        assertThat(v4.preReleaseName, is("beta"));
        assertThat(v4.preReleaseVersion, is(151));
        assertThat(v4.isSnapshot, is(true));
    }

    @Test
    void valueOf_ShouldThrowIllegalArgument_WhenInvalidKeyIsGiven() {
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("1.0.0-"));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("x1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("v1.0"));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("v1.0."));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("v1.0.0+test"));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("v1.0.0-ae0..."));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("v1.0.0+test"));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("v...."));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("1.0.0-beta"));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("1.0.0-beta."));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("v1.0.0-beta.1-141251"));
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("v1.0.0-beta-SNAPSHOT.1"));
    }

    @Test
    void compareTo_ShouldOrderCorrectly_WhenMultipleVersionsAreGiven() {
        Set<Version> versions = new TreeSet<>();

        versions.add(Version.valueOf("v6.0.0-beta.2"));
        versions.add(Version.valueOf("v6.0.0-beta.1"));
        versions.add(Version.valueOf("v1.2.1"));
        versions.add(Version.valueOf("v1.1.1"));
        versions.add(Version.valueOf("v6.0.0-beta.2-SNAPSHOT"));
        versions.add(Version.valueOf("v3.0.0-alpha.1"));
        versions.add(Version.valueOf("v1.1.2"));
        versions.add(Version.valueOf("v2.1.1"));
        versions.add(Version.valueOf("v3.0.0-alpha.2"));
        versions.add(Version.valueOf("v1.0.0"));
        versions.add(Version.valueOf("v1.1.0"));
        versions.add(Version.valueOf("v3.0.0-beta.2"));
        versions.add(Version.valueOf("v3.0.0"));
        versions.add(Version.valueOf("v0.0.0"));
        versions.add(Version.valueOf("v3.0.0-beta.1"));
        versions.add(Version.valueOf("v3.1.0-beta.1"));
        versions.add(Version.valueOf("v3.0.4-beta.1"));
        versions.add(Version.valueOf("v3.1.0-beta.168"));
        versions.add(Version.valueOf("v3.1.0-cet.168"));
        versions.add(Version.valueOf("v3.1.0-zot.168"));
        versions.add(Version.valueOf("v3.1.0-bot.158"));
        versions.add(Version.valueOf("v3.0.7-beta.1"));
        versions.add(Version.valueOf("v3.0.7-beta.1-SNAPSHOT"));

        assertThat(versions, containsInRelativeOrder(
            Version.valueOf("v0.0.0"),
            Version.valueOf("v1.0.0"),
            Version.valueOf("v1.1.0"),
            Version.valueOf("v1.1.1"),
            Version.valueOf("v1.1.2"),
            Version.valueOf("v1.2.1"),
            Version.valueOf("v2.1.1"),
            Version.valueOf("v3.0.0-alpha.1"), // shorter prerelease names are preferred, has nothing to with beta/alpha
            Version.valueOf("v3.0.0-alpha.2"),
            Version.valueOf("v3.0.0-beta.1"),
            Version.valueOf("v3.0.0-beta.2"),
            Version.valueOf("v3.0.0"),
            Version.valueOf("v3.0.4-beta.1"),
            Version.valueOf("v3.0.7-beta.1-SNAPSHOT"),
            Version.valueOf("v3.0.7-beta.1"),
            Version.valueOf("v3.1.0-beta.1"),
            Version.valueOf("v3.1.0-beta.168"),
            Version.valueOf("v3.1.0-cet.168"),
            Version.valueOf("v3.1.0-zot.168"),
            Version.valueOf("v3.1.0-bot.158"),
            Version.valueOf("v6.0.0-beta.1"),
            Version.valueOf("v6.0.0-beta.2-SNAPSHOT"),
            Version.valueOf("v6.0.0-beta.2")));
    }

    @Test
    void toString_ShouldGenerateValidVersion_WhenVersionIsGiven() {
        Version v0 = Version.valueOf(Version.valueOf("v1.0.0").toString());
        v0 = Version.valueOf(v0.toString());

        assertThat(v0.major, is(1));
        assertThat(v0.minor, is(0));
        assertThat(v0.patch, is(0));
        assertThat(v0.preReleaseName, emptyString());
        assertThat(v0.preReleaseVersion, is(-1));
        assertThat(v0.isSnapshot, is(false));

        Version v1 = Version.valueOf(Version.valueOf("v1.0.0-beta.1").toString());
        assertThat(v1.major, is(1));
        assertThat(v1.minor, is(0));
        assertThat(v1.patch, is(0));
        assertThat(v1.preReleaseName, is("beta"));
        assertThat(v1.preReleaseVersion, is(1));
        assertThat(v1.isSnapshot, is(false));

        Version v2 = Version.valueOf(Version.valueOf("v1975.1861.21571-alphacentauri.1-SNAPSHOT").toString());
        assertThat(v2.major, is(1975));
        assertThat(v2.minor, is(1861));
        assertThat(v2.patch, is(21571));
        assertThat(v2.preReleaseName, is("alphacentauri"));
        assertThat(v2.preReleaseVersion, is(1));
        assertThat(v2.isSnapshot, is(true));
    }
}
