package highscore;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;

public class HighscoreService {

    private static SOAPConnectionFactory soapConnectionFactory;
    private static MessageFactory messageFactory;
    private static String highscoreURL = "http://playground.big.tuwien.ac.at:8080/highscoreservice/";
    private static String highscoreWebService = "http://playground.big.tuwien.ac.at:8080/highscoreservice/PublishHighScoreService?wsdl";
    private static String userKey = "3ke93-gue34-dkeu9";

    public HighscoreService() throws Exception {
        try {
            soapConnectionFactory = SOAPConnectionFactory.newInstance();
            messageFactory = MessageFactory.newInstance();
        } catch (SOAPException e) {
            e.printStackTrace();
            throw new Exception("SOAP Failed");
        }
    }


}
