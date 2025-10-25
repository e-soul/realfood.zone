package zone.realfood;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

import zone.realfood.db.DynamoDbTools;

public final class Fixtures {
    
    private Fixtures() {
        // no instances
    }

    public static boolean isDynamoDbLocalRunning() {
        URI endpoint = URI.create(System.getProperty(DynamoDbTools.DYNAMODB_ENDPOINT_SYS_PROP));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
