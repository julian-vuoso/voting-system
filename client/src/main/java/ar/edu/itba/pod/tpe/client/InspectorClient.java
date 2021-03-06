package ar.edu.itba.pod.tpe.client;

import ar.edu.itba.pod.tpe.client.exceptions.ArgumentException;
import ar.edu.itba.pod.tpe.client.utils.ClientUtils;
import ar.edu.itba.pod.tpe.exceptions.IllegalElectionStateException;
import ar.edu.itba.pod.tpe.interfaces.InspectionService;
import ar.edu.itba.pod.tpe.interfaces.VoteAvailableCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

public class InspectorClient {
    private static Logger logger = LoggerFactory.getLogger(InspectorClient.class);

    private static final int ERROR_STATUS = 1;

    private static final String SERVER_ADDRESS_PARAM = "serverAddress";
    private static final String ID_PARAM = "id";
    private static final String PARTY_PARAM = "party";

    private static final String INSPECTION_SERVICE_NAME = "service";

    private static InetSocketAddress serverAddress;
    private static int tableNumber;
    private static String partyName;

    public static void main(String[] args) {
        logger.info("tpe1-g6 InspectorClient Starting ...");

        try {
            argumentParsing();
        } catch (ArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(ERROR_STATUS);
            return;
        }

        logger.debug("Args: " + serverAddress.getHostName() + " - " + serverAddress.getPort() + " - " + tableNumber + " - " + partyName);

        try {
            clientInspect();
        } catch (RemoteException e) {
            System.err.println("Remote communication failed.");
            System.exit(ERROR_STATUS);
        } catch (NotBoundException e) {
            System.err.println("Server " + INSPECTION_SERVICE_NAME + " has no associated binding.");
            System.exit(ERROR_STATUS);
        }
    }

    private static void clientInspect() throws RemoteException, NotBoundException {
        final Registry registry = LocateRegistry.getRegistry(serverAddress.getHostName(), serverAddress.getPort());
        final InspectionService service = (InspectionService) registry.lookup(INSPECTION_SERVICE_NAME);

        VoteAvailableCallbackHandler handler = new VoteAvailableCallbackHandlerImpl(tableNumber, partyName);
        UnicastRemoteObject.exportObject(handler, 0);

        try {
            service.inspect(tableNumber, partyName, handler);
            System.out.println("Fiscal of " + partyName + " registered on polling place " + tableNumber);
        } catch (IllegalElectionStateException e) {
            System.out.println(e.getMessage());
            UnicastRemoteObject.unexportObject(handler, true);
        }
    }

    private static void argumentParsing() throws ArgumentException {
        // -DserverAddress=xx.xx.xx.xx:yyyy    --> host:port
        // -Did=​ pollingPlaceNumber            --> tableNumber
        // -Dparty=​ partyName                  --> partyName

        Properties properties = System.getProperties();

        try {
            serverAddress = ClientUtils.getInetAddress(properties.getProperty(SERVER_ADDRESS_PARAM));
        } catch (URISyntaxException e) {
            throw new ArgumentException("Server Address must be supplied using -DserverAddress and its format must be xx.xx.xx.xx:yyyy");
        }

        try {
            tableNumber = Integer.parseInt(properties.getProperty(ID_PARAM));
        } catch (NumberFormatException e) {
            throw new ArgumentException("Table ID must be supplied using -Did and it must be a number");
        }

        partyName = properties.getProperty(PARTY_PARAM);
        if (partyName == null) {
            throw new ArgumentException("Party Name must be supplied using -Dparty");
        }
    }
}
