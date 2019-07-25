package de.fhg.iais.roberta.connection.wired;

import org.apache.http.entity.ContentType;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wired.ev3.Ev3;
import de.fhg.iais.roberta.util.PropertyHelper;

import static de.fhg.iais.roberta.connection.IConnector.CMD_REGISTER;
import static de.fhg.iais.roberta.connection.IConnector.KEY_CMD;

public class RndisDetector implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(RndisDetector.class);

    private static final Map<String, Class<? extends AbstractWiredRobot>> DEVICES = new HashMap<>(1);
    static {
        DEVICES.put(PropertyHelper.getInstance().getProperty("brickIp"), Ev3.class);
    }

    @Override
    public List<IRobot> detectRobots() {
        try (AsyncHttpClient client = Dsl.asyncHttpClient()) {
            JSONObject request = new JSONObject();
            request.put(KEY_CMD, CMD_REGISTER);

            List<IRobot> robots = new ArrayList<>(5);
            for ( Entry<String, Class<? extends AbstractWiredRobot>> entry : DEVICES.entrySet() ) {
                ListenableFuture<String>
                    name =
                    client.preparePost("http://" + entry.getKey() + "/brickinfo")
                          .addHeader("ContentType", ContentType.APPLICATION_JSON)
                          .setBody(request.toString())
                          .execute(new StringAsyncCompletionHandler());
                robots.add(entry.getValue().getConstructor(String.class).newInstance(name.get()));
            }
            return robots;
        } catch ( IOException e ) {
            LOG.error("Could not close async client: {}", e.getMessage());
        } catch ( InterruptedException e ) {
            LOG.error("Robot request was interrupted: {}", e.getMessage());
        } catch ( ExecutionException e ) {
            LOG.info("Could not find RNDIS robot: {}", e.getMessage());
        } catch ( InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e ) {
            LOG.error("Robot not implemented: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    private static class StringAsyncCompletionHandler extends AsyncCompletionHandler<String> {
        @Override
        public String onCompleted(Response response) {
            JSONObject jsonObject = new JSONObject(response.getResponseBody());
            return jsonObject.getString("brickname");
        }
    }
}
