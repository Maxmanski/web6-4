package highscore;

import models.JeopardyGame;

import javax.xml.soap.*;

public class HighscoreService {

    private static SOAPConnectionFactory soapConnectionFactory;
    private static MessageFactory messageFactory;
    private static String highscoreURL = "http://playground.big.tuwien.ac.at:8080/highscoreservice/";
    private static String highscoreWebService = "http://playground.big.tuwien.ac.at:8080/highscoreservice/PublishHighScoreService?wsdl";
    private static String userKey = "3ke93-gue34-dkeu9";
    private static String dataNamespace = "http://big.tuwien.ac.at/we/highscore/data";

    public HighscoreService() throws Exception {
        try {
            soapConnectionFactory = SOAPConnectionFactory.newInstance();
            messageFactory = MessageFactory.newInstance();
        } catch (SOAPException e) {
            e.printStackTrace();
            throw new Exception("SOAP Failed");
        }
    }

    public static String postScore(JeopardyGame game) throws Exception{
        SOAPConnection connection = null;
        SOAPMessage message = null;
        String UUID = "";

        connection = soapConnectionFactory.createConnection();
        message = messageFactory.createMessage();
        SOAPPart part = message.getSOAPPart();
        SOAPEnvelope env = part.getEnvelope();
        env.addNamespaceDeclaration("data", dataNamespace);
        SOAPBody body = env.getBody();
        fillBody(body,game);
        SOAPMessage response = connection.call(message, highscoreURL);

        try{
            UUID = getUUID(response);
        }catch(Exception e){
            throw new Exception("UUID failed");
        }

        return UUID;
    }
    private static String getUUID(SOAPMessage message){
        return "";
    }
    public static void fillBody(SOAPBody body, JeopardyGame game){

    }
}
