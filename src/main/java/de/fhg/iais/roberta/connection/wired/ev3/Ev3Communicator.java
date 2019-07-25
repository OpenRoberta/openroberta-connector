package de.fhg.iais.roberta.connection.wired.ev3;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;

import de.fhg.iais.roberta.connection.IConnector;

/**
 * The EV3 is running an http server. We initialise the connection to the robot by the connector, because of possible firewall issues.
 *
 * @author dpyka
 */
class Ev3Communicator {

    private static final int CONNECT_TIMEOUT = 3000;

    private final String brickInfo;
    private final String brickProgram;
    private final String brickFirmware;

    private final CloseableHttpClient httpClient;

    /**
     * @param brickIp is 10.0.1.1 for leJOS
     */
    Ev3Communicator(String brickIp) {
        this.brickInfo = brickIp + "/brickinfo";
        this.brickProgram = brickIp + "/program";
        this.brickFirmware = brickIp + "/firmware";

        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(CONNECT_TIMEOUT).build();
        this.httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    }

    /**
     * Send a push request to the http server on the EV3. On EV3 side this will also control the connection icon and sound when connected successfully to Open
     * Roberta. This using http POST.
     *
     * @param command String CMD_REGISTER or CMD_REPEAT
     * @return JSONObject Information about the EV3 like brickname, versions, battery, ...
     * @throws IOException should only occur if you disconnect the cable
     */
    JSONObject pushToBrick(String command) throws IOException {
        JSONObject request = new JSONObject();
        request.put(IConnector.KEY_CMD, command);
        HttpPost post = new HttpPost("http://" + this.brickInfo);
        HttpEntity jsonContent = new StringEntity(request.toString(), ContentType.create("application/json", "UTF-8"));
        post.setEntity(jsonContent);

        try(CloseableHttpResponse response = this.httpClient.execute(post)) {
            HttpEntity entity = response.getEntity();
            return new JSONObject(EntityUtils.toString(entity));
        }
    }

    /**
     * Upload a binary user program to the EV3. It uses http POST.
     *
     * @param binaryFile the binary file to be uploaded
     * @param filename the filename it should have on the brick
     * @return the result of the upload
     * @throws IOException should only occur if you disconnect the cable
     */
    JSONObject uploadProgram(byte[] binaryFile, String filename) throws IOException {
        HttpPost post = new HttpPost("http://" + this.brickProgram);
        return this.uploadBinary(post, binaryFile, filename);
    }

    /**
     * Upload a binary system file to the EV3. It uses http POST.
     *
     * @param binaryFile the binary file to be uploaded
     * @param filename the filename it should have on the brick
     * @return the result of the upload
     * @throws IOException should only occur if you disconnect the cable
     */
    JSONObject uploadFirmwareFile(byte[] binaryFile, String filename) throws IOException {
        HttpPost post = new HttpPost("http://" + this.brickFirmware);
        return this.uploadBinary(post, binaryFile, filename);
    }

    private JSONObject uploadBinary(HttpPost post, byte[] binaryFile, String filename) throws IOException {
        HttpEntity content = new ByteArrayEntity(binaryFile);
        post.setEntity(content);
        post.setHeader("Filename", filename);

        try(CloseableHttpResponse response = this.httpClient.execute(post)) {
            HttpEntity entity = response.getEntity();
            return new JSONObject(EntityUtils.toString(entity));
        }
    }

    /**
     * Send a command to the EV3 to restart the menu after updating the system libraries.
     *
     * @throws IOException should only occur if you disconnect the cable
     */
    void restartBrick() throws IOException {
        this.pushToBrick("update");
    }

    /**
     * Shut down the connection to the EV3.
     */
    void shutdown() {
        try {
            this.httpClient.close();
        } catch ( IOException e ) {
            // ok
        }
    }

    /**
     * Check if a program is currently running on the EV3.
     *
     * @return true (as string) if a program is running, false (as string) if no program is running.
     * @throws IOException should only occur if you disconnect the cable
     */
    boolean isRunning() throws IOException {
        return this.pushToBrick(IConnector.CMD_ISRUNNING).getString("isrunning").equals("true");
    }
}
