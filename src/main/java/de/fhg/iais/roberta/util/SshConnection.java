package de.fhg.iais.roberta.util;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.xfer.InMemorySourceFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

/**
 * A SSH client, that wraps common SSH functionality for easy usage.
 */
public class SshConnection implements AutoCloseable {

    private static final int SSH_PORT = 22;
    private static final int TIMEOUT = 1000;

    private final SSHClient ssh;

    public SshConnection(InetAddress address, String username, String password) throws UserAuthException, TransportException, IOException {
        this.ssh = new SSHClient();
        this.ssh.addHostKeyVerifier(new PromiscuousVerifier());

        this.ssh.setTimeout(TIMEOUT);

        this.ssh.connect(address, SSH_PORT);
        this.ssh.authPassword(username, password);
    }

    public String command(String command) throws IOException, TransportException, ConnectionException {
        try (Session session = this.ssh.startSession(); Session.Command cmd = session.exec(command)) {

            return IOUtils.readFully(cmd.getInputStream()).toString();
        }
    }

    /**
     * Copy local file to remote. If not successful, throw an exception
     */
    public void copyLocalToRemote(byte[] content, String to, String fileName) throws IOException {
        this.ssh.newSCPFileTransfer().upload(new ProgramInMemorySourceFile(fileName, content), to);
    }

    /**
     * copy local file to remote. If not successful, throw an exception
     */
    public void copyLocalToRemote(String from, String to, String fileName) throws IOException {
        this.ssh.newSCPFileTransfer().upload(from + '/' + fileName, to + '/' + fileName);
    }

    @Override
    public void close() throws IOException {
        this.ssh.close();
    }

    private static class ProgramInMemorySourceFile extends InMemorySourceFile {
        private final String name;
        private final byte[] content;

        ProgramInMemorySourceFile(String fileName, byte[] content) {
            this.name = fileName;
            this.content = content;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public long getLength() {
            return (long) this.content.length;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(this.content);
        }
    }
}
