package controllers;

import java.util.*;

import highscore.*;
import models.Category;
import models.JeopardyDAO;
import models.JeopardyGame;
import models.JeopardyUser;
import play.Logger;
import play.cache.Cache;
import play.data.DynamicForm;
import play.data.DynamicForm.Dynamic;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import twitter.ITwitterClient;
import twitter.TwitterClient;
import twitter.TwitterStatusMessage;
import views.html.jeopardy;
import views.html.question;
import views.html.winner;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

@Security.Authenticated(Secured.class)
public class GameController extends Controller {
	
	protected static final int CATEGORY_LIMIT = 5;
	private static String userKey = "3ke93-gue34-dkeu9";
	
	@Transactional
	public static Result index() {
		return redirect(routes.GameController.playGame());
	}
	
	@play.db.jpa.Transactional(readOnly = true)
	private static JeopardyGame createNewGame(String userName) {
		return createNewGame(JeopardyDAO.INSTANCE.findByUserName(userName));
	}
	
	@play.db.jpa.Transactional(readOnly = true)
	private static JeopardyGame createNewGame(JeopardyUser user) {
		if(user == null) // name still stored in session, but database dropped
			return null;

		Logger.info("[" + user + "] Creating a new game.");
		List<Category> allCategories = JeopardyDAO.INSTANCE.findEntities(Category.class);
		
		if(allCategories.size() > CATEGORY_LIMIT) {
			// select 5 categories randomly (simple)
			Collections.shuffle(allCategories);
			allCategories = allCategories.subList(0, CATEGORY_LIMIT);
		}
		Logger.info("Start game with " + allCategories.size() + " categories.");
		JeopardyGame game = new JeopardyGame(user, allCategories);
		cacheGame(game);
		return game;
	}
	
	private static void cacheGame(JeopardyGame game) {
		Cache.set(gameId(), game, 3600);
	}
	
	private static JeopardyGame cachedGame(String userName) {
		Object game = Cache.get(gameId());
		if(game instanceof JeopardyGame)
			return (JeopardyGame) game;
		return createNewGame(userName);
	}
	
	private static String gameId() {
		return "game." + uuid();
	}

	private static String uuid() {
		String uuid = session("uuid");
		if (uuid == null) {
			uuid = UUID.randomUUID().toString();
			session("uuid", uuid);
		}
		return uuid;
	}
	
	@Transactional
	public static Result newGame() {
		Logger.info("[" + request().username() + "] Start new game.");
		JeopardyGame game = createNewGame(request().username());
		return ok(jeopardy.render(game));
	}
	
	@Transactional
	public static Result playGame() {
		Logger.info("[" + request().username() + "] Play the game.");
		JeopardyGame game = cachedGame(request().username());
		if(game == null) // e.g., username still in session, but db dropped
			return redirect(routes.Authentication.login());
		if(game.isAnswerPending()) {
			Logger.info("[" + request().username() + "] Answer pending... redirect");
			return ok(question.render(game));
		} else if(game.isGameOver()) {
			Logger.info("[" + request().username() + "] Game over... redirect");
			return ok(winner.render(game));
		}			
		return ok(jeopardy.render(game));
	}
	
	@play.db.jpa.Transactional(readOnly = true)
	public static Result questionSelected() {
		JeopardyGame game = cachedGame(request().username());
		if(game == null || !game.isRoundStart())
			return redirect(routes.GameController.playGame());
		
		Logger.info("[" + request().username() + "] Questions selected.");		
		DynamicForm form = Form.form().bindFromRequest();
		
		String questionSelection = form.get("question_selection");
		
		if(questionSelection == null || questionSelection.equals("") || !game.isRoundStart()) {
			return badRequest(jeopardy.render(game));
		}
		
		game.chooseHumanQuestion(Long.parseLong(questionSelection));
		
		return ok(question.render(game));
	}
	
	@play.db.jpa.Transactional(readOnly = true)
	public static Result submitAnswers() {
		JeopardyGame game = cachedGame(request().username());
		if(game == null || !game.isAnswerPending())
			return redirect(routes.GameController.playGame());
		
		Logger.info("[" + request().username() + "] Answers submitted.");
		Dynamic form = Form.form().bindFromRequest().get();
		
		@SuppressWarnings("unchecked")
		Map<String,String> data = form.getData();
		List<Long> answerIds = new ArrayList<>();
		
		for(String key : data.keySet()) {
			if(key.startsWith("answers[")) {
				answerIds.add(Long.parseLong(data.get(key)));
			}
		}
		game.answerHumanQuestion(answerIds);
		if(game.isGameOver()) {
			return redirect(routes.GameController.gameOver());
		} else {
			return ok(jeopardy.render(game));
		}
	}
	
	@play.db.jpa.Transactional(readOnly = true)
	public static Result gameOver() {
		JeopardyGame game = cachedGame(request().username());
		if(game == null || !game.isGameOver())
			return redirect(routes.GameController.playGame());

		ObjectFactory objectFactory = new ObjectFactory();
		HighScoreRequestType requestType = objectFactory.createHighScoreRequestType();
		requestType.setUserKey("3ke93-gue34-dkeu9");
		requestType.setUserData(objectFactory.createUserDataType());
		UserDataType players = requestType.getUserData();

		players.setWinner(objectFactory.createUserType());
		UserType win = players.getWinner();
		win.setFirstName(game.getWinner().getUser().getFirstName());
		win.setLastName(game.getWinner().getUser().getLastName());
		win.setPassword("");
		win.setPoints(game.getWinner().getProfit());
		win.setGender(GenderType.fromValue(game.getWinner().getUser().getGender().name()));

		players.setLoser(objectFactory.createUserType());
		UserType loser = players.getLoser();
		loser.setFirstName(game.getLoser().getUser().getFirstName());
		loser.setLastName(game.getLoser().getUser().getLastName());
		loser.setPassword("");
		loser.setPoints(game.getLoser().getProfit());
		loser.setGender(GenderType.fromValue(game.getLoser().getUser().getGender().name()));

		GregorianCalendar winnerBithDateGC = new GregorianCalendar();
		winnerBithDateGC.setTime(game.getWinner().getUser().getBirthDate());
		XMLGregorianCalendar winnerBirthDate = null;
		try {
			winnerBirthDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(winnerBithDateGC);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		GregorianCalendar loserBithDateGC = new GregorianCalendar();
		loserBithDateGC.setTime(game.getLoser().getUser().getBirthDate());
		XMLGregorianCalendar loserBirthDate = null;
		try {
			loserBirthDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(loserBithDateGC);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		win.setBirthDate(winnerBirthDate);
		loser.setBirthDate(loserBirthDate);

		PublishHighScoreEndpoint endpoint = new PublishHighScoreService().getPublishHighScorePort();

		String uuid = null;
		try {
			uuid = endpoint.publishHighScore(requestType);
			Logger.info("UUID: " + uuid);
		} catch (Failure failure) {
			failure.printStackTrace();
		}
		TwitterClient twitter =  new TwitterClient();
		// TwitterStatusMessage(String from, String uuid, Date dateTime)
		if(uuid != null) {
			try {
				twitter.publishUuid(new TwitterStatusMessage(game.getHumanPlayer().getUser().getName(), uuid, new Date()));
				Logger.info("Der Text wurde erfolgreich auf Twitter veroeffentlicht!" );
			} catch (Exception e) {
				Logger.error("Es ist ein Fehler beim veroeffentlichen auf Twitter aufgetreten!" );
				e.printStackTrace();
			}
		}
		Logger.info("[" + request().username() + "] Game over.");
		return ok(winner.render(game));
	}
}
