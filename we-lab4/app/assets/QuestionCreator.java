package assets;

import at.ac.tuwien.big.we.dbpedia.api.DBPediaService;
import at.ac.tuwien.big.we.dbpedia.api.SelectQueryBuilder;
import at.ac.tuwien.big.we.dbpedia.vocabulary.DBPedia;
import at.ac.tuwien.big.we.dbpedia.vocabulary.DBPediaOWL;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import models.Answer;
import models.Category;
import models.Question;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuestionCreator {

    private Category moviesCategory;

    public QuestionCreator(){
        this.moviesCategory = new Category();
        this.moviesCategory.setNameDE("Filme");
        this.moviesCategory.setNameEN("Movies");
    }

    public List<Question> getLinkedOpenDataQuestions(){
        List<Question> questions = new ArrayList<Question>(6);

        questions.add(this.getTimBurtonQuestion());
        questions.add(this.getJohnnyDeppMovies());
        questions.add(this.getMovieGrossingQuestion());
        questions.add(this.getOldMoviesQuestion());
        questions.add(this.getChristianBaleFilms());
        questions.add(this.getHansZimmerMusicFilms());

        return questions;
    }

    public List<Category> getCategories(){
        this.getLinkedOpenDataQuestions();
        
        List<Category> categories = new ArrayList<Category>(1);
        categories.add(this.moviesCategory);

        return categories;
    }

    public Question getTimBurtonQuestion(){
        // Check if DBpedia is available
        if(!DBPediaService.isAvailable()) {
            return null;
        }

// Resource Tim Burton is available at http://dbpedia.org/resource/Tim_Burton
// Load all statements as we need to get the name later
        Resource director = DBPediaService.loadStatements(DBPedia.createResource("Tim_Burton"));
// Resource Johnny Depp is available at http://dbpedia.org/resource/Johnny_Depp
// Load all statements as we need to get the name later
        Resource actor = DBPediaService.loadStatements(DBPedia.createResource("Johnny_Depp"));
// retrieve english and german names, might be used for question text
        String englishDirectorName = DBPediaService.getResourceName(director, Locale.ENGLISH);
        String germanDirectorName = DBPediaService.getResourceName(director, Locale.GERMAN);
        String englishActorName = DBPediaService.getResourceName(actor, Locale.ENGLISH);
        String germanActorName = DBPediaService.getResourceName(actor, Locale.GERMAN);
// build SPARQL-query
        SelectQueryBuilder movieQuery = DBPediaService.createQueryBuilder()
                .setLimit(5) // at most five statements
                .addWhereClause(RDF.type, DBPediaOWL.Film)
                .addPredicateExistsClause(FOAF.name)
                .addWhereClause(DBPediaOWL.director, director)
                .addFilterClause(RDFS.label, Locale.GERMAN)
                .addFilterClause(RDFS.label, Locale.ENGLISH);
// retrieve data from dbpedia
        Model timBurtonMovies = DBPediaService.loadStatements(movieQuery.toQueryString());
// get english and german movie names, e.g., for right choices
        List<String> englishTimBurtonMovieNames =
                DBPediaService.getResourceNames(timBurtonMovies, Locale.ENGLISH);
        List<String> germanTimBurtonMovieNames =
                DBPediaService.getResourceNames(timBurtonMovies, Locale.GERMAN);

// alter query to get movies without tim burton
        movieQuery.removeWhereClause(DBPediaOWL.director, director);
        movieQuery.addMinusClause(DBPediaOWL.director, director);
        movieQuery.setOffset(80);
// retrieve data from dbpedia
        Model noTimBurtonMovies = DBPediaService.loadStatements(movieQuery.toQueryString());
// get english and german movie names, e.g., for wrong choices
        List<String> englishNoTimBurtonMovieNames =
                DBPediaService.getResourceNames(noTimBurtonMovies, Locale.ENGLISH);
        List<String> germanNoTimBurtonMovieNames =
                DBPediaService.getResourceNames(noTimBurtonMovies, Locale.GERMAN);

        return this.createQuestion(this.moviesCategory, 10, "In diesen Filmen war " + germanDirectorName + " Director",
                "These movies were directed by " + englishDirectorName,
                germanTimBurtonMovieNames, englishTimBurtonMovieNames, germanNoTimBurtonMovieNames, englishNoTimBurtonMovieNames);
    }

    public Question getJohnnyDeppMovies(){

        if(!DBPediaService.isAvailable()) {
            return null;
        }

        List<String> germanJohnnyMovies, englishJohnnyMovies, germanNoJohnnyMovies, englishNoJohnnyMovies;

        Resource johnny = DBPediaService.loadStatements(DBPedia.createResource("Johnny_Depp"));

        String englishJohnnyName = DBPediaService.getResourceName(johnny, Locale.ENGLISH);
        String germanJohnnyName = DBPediaService.getResourceName(johnny, Locale.GERMAN);

        SelectQueryBuilder movieQuery = DBPediaService.createQueryBuilder()
                .setLimit(5)
                .addWhereClause(RDF.type, DBPediaOWL.Film)
                .addPredicateExistsClause(FOAF.name)
                .addWhereClause(DBPediaOWL.starring, johnny)
                .addFilterClause(RDFS.label, Locale.GERMAN)
                .addFilterClause(RDFS.label, Locale.ENGLISH);

        Model johnnyMovies = DBPediaService.loadStatements(movieQuery.toQueryString());

        germanJohnnyMovies = DBPediaService.getResourceNames(johnnyMovies, Locale.GERMAN);
        englishJohnnyMovies = DBPediaService.getResourceNames(johnnyMovies, Locale.ENGLISH);

        movieQuery.removeWhereClause(DBPediaOWL.starring, johnny).addMinusClause(DBPediaOWL.starring, johnny).setOffset(60);

        Model noJohnnyMovies = DBPediaService.loadStatements(movieQuery.toQueryString());

        germanNoJohnnyMovies = DBPediaService.getResourceNames(noJohnnyMovies, Locale.GERMAN);
        englishNoJohnnyMovies = DBPediaService.getResourceNames(noJohnnyMovies, Locale.ENGLISH);

        return this.createQuestion(this.moviesCategory, 20, "In diesen Filmen hat " + germanJohnnyName + " mitgespielt",
                englishJohnnyName + " starred in these movies", germanJohnnyMovies, englishJohnnyMovies, germanNoJohnnyMovies, englishNoJohnnyMovies);
    }

    public Question getMovieGrossingQuestion(){

        if(!DBPediaService.isAvailable()) {
            return null;
        }

        List<String> germanHighMovies, englishHighMovies, germanLowMovies, englishLowMovies;

        String queryString = "SELECT DISTINCT ?subject\n" +
                "WHERE { \n" +
                " ?subject <http://dbpedia.org/ontology/gross> ?gross .\n" +
                " ?subject <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film> .\n" +
                " ?subject <http://www.w3.org/2000/01/rdf-schema#label> ?var3 .\n" +
                " ?subject <http://www.w3.org/2000/01/rdf-schema#label> ?var4 .\n" +
                "\n" +
                " FILTER (datatype(?gross) = <http://dbpedia.org/datatype/usDollar>) .\n" +
                " BIND (<http://www.w3.org/2001/XMLSchema#integer>(?gross) as ?intgross) .\n" +
                " FILTER (?intgross > 100000000) .\n" +
                " FILTER EXISTS { ?subject <http://dbpedia.org/ontology/gross> ?gross } .\n" +
                " FILTER EXISTS { ?subject <http://xmlns.com/foaf/0.1/name> ?var0 } .\n" +
                " FILTER langMatches( lang(?var3), 'de') .\n" +
                " FILTER langMatches( lang(?var4), 'en') .\n" +
                "\n" +
                "} LIMIT 5 OFFSET 40\n";

        Model highMovies = DBPediaService.loadStatements(queryString);

        germanHighMovies = DBPediaService.getResourceNames(highMovies, Locale.GERMAN);
        englishHighMovies = DBPediaService.getResourceNames(highMovies, Locale.ENGLISH);

        queryString = "SELECT DISTINCT ?subject\n" +
                "WHERE { \n" +
                " ?subject <http://dbpedia.org/ontology/gross> ?gross .\n" +
                " ?subject <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film> .\n" +
                " ?subject <http://www.w3.org/2000/01/rdf-schema#label> ?var3 .\n" +
                " ?subject <http://www.w3.org/2000/01/rdf-schema#label> ?var4 .\n" +
                "\n" +
                " FILTER (datatype(?gross) = <http://dbpedia.org/datatype/usDollar>) .\n" +
                " BIND (<http://www.w3.org/2001/XMLSchema#integer>(?gross) as ?intgross) .\n" +
                " FILTER (?intgross < 100000000) .\n" +
                " FILTER EXISTS { ?subject <http://dbpedia.org/ontology/gross> ?gross } .\n" +
                " FILTER EXISTS { ?subject <http://xmlns.com/foaf/0.1/name> ?var0 } .\n" +
                " FILTER langMatches( lang(?var3), 'de') .\n" +
                " FILTER langMatches( lang(?var4), 'en') .\n" +
                "\n" +
                "} LIMIT 5 OFFSET 30\n";

        Model lowMovies = DBPediaService.loadStatements(queryString);

        germanLowMovies = DBPediaService.getResourceNames(lowMovies, Locale.GERMAN);
        englishLowMovies = DBPediaService.getResourceNames(lowMovies, Locale.ENGLISH);

        return this.createQuestion(this.moviesCategory, 50, "Diese Filme haben mehr als 100.000.000 USD eingespielt",
                "These movies have grossed more than 100,000,000 USD",
                germanHighMovies, englishHighMovies, germanLowMovies, englishLowMovies);
    }

    public Question getOldMoviesQuestion(){
        if(!DBPediaService.isAvailable()){
            return null;
        }

        List<String> germanOldMovies, englishOldMovies, germanNewMovies, englishNewMovies;

        String queryString = "SELECT DISTINCT ?subject\n" +
                "WHERE {\n" +
                " ?subject <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film> .\n" +
                " ?subject <http://www.w3.org/2000/01/rdf-schema#label> ?var2 .\n" +
                " ?subject <http://www.w3.org/2000/01/rdf-schema#label> ?var3 .\n" +
                " ?subject <http://dbpedia.org/ontology/releaseDate> ?date .\n" +
                " FILTER (?date < <http://www.w3.org/2001/XMLSchema#date>('1990-01-01')) .\n" +
                " FILTER EXISTS { ?subject <http://xmlns.com/foaf/0.1/name> ?var0 } .\n" +
                " FILTER langMatches( lang(?var2), 'de') .\n" +
                " FILTER langMatches( lang(?var3), 'en') .\n" +
                "} LIMIT 5 OFFSET 0";

        Model oldMovies = DBPediaService.loadStatements(queryString);

        germanOldMovies = DBPediaService.getResourceNames(oldMovies, Locale.GERMAN);
        englishOldMovies= DBPediaService.getResourceNames(oldMovies, Locale.ENGLISH);

        queryString = "SELECT DISTINCT ?subject\n" +
                "WHERE {\n" +
                " ?subject <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film> .\n" +
                " ?subject <http://www.w3.org/2000/01/rdf-schema#label> ?var2 .\n" +
                " ?subject <http://www.w3.org/2000/01/rdf-schema#label> ?var3 .\n" +
                " ?subject <http://dbpedia.org/ontology/releaseDate> ?date .\n" +
                " FILTER (?date >= <http://www.w3.org/2001/XMLSchema#date>('1991-01-01')) .\n" +
                " FILTER EXISTS { ?subject <http://xmlns.com/foaf/0.1/name> ?var0 } .\n" +
                " FILTER langMatches( lang(?var2), 'de') .\n" +
                " FILTER langMatches( lang(?var3), 'en') .\n" +
                "} LIMIT 5 OFFSET 20";

        Model newMovies = DBPediaService.loadStatements(queryString);

        germanNewMovies = DBPediaService.getResourceNames(newMovies, Locale.GERMAN);
        englishNewMovies= DBPediaService.getResourceNames(newMovies, Locale.ENGLISH);

        return this.createQuestion(this.moviesCategory, 30, "Diese Filme sind vor 1990 veröffentlicht worden",
                "These movies were released before 1990",
                germanOldMovies, englishOldMovies, germanNewMovies, englishNewMovies);
    }

    public Question getChristianBaleFilms() {

        if(!DBPediaService.isAvailable()) {
            return null;
        }

        List<String> germanChristianMovies, englishChristianMovies, germanNoChristianMovies, englishNoChristianMovies;

        Resource christian = DBPediaService.loadStatements(DBPedia.createResource("Christian_Bale"));

        String englishChristianName = DBPediaService.getResourceName(christian, Locale.ENGLISH);
        String germanChristianName = DBPediaService.getResourceName(christian, Locale.GERMAN);

        SelectQueryBuilder movieQuery = DBPediaService.createQueryBuilder()
                .setLimit(5)
                .addWhereClause(RDF.type, DBPediaOWL.Film)
                .addPredicateExistsClause(FOAF.name)
                .addWhereClause(DBPediaOWL.starring, christian)
                .addFilterClause(RDFS.label, Locale.GERMAN)
                .addFilterClause(RDFS.label, Locale.ENGLISH);

        Model christianMovies = DBPediaService.loadStatements(movieQuery.toQueryString());

        germanChristianMovies = DBPediaService.getResourceNames(christianMovies, Locale.GERMAN);
        englishChristianMovies = DBPediaService.getResourceNames(christianMovies, Locale.ENGLISH);

        movieQuery.removeWhereClause(DBPediaOWL.starring, christian).addMinusClause(DBPediaOWL.starring, christian).setOffset(10);

        Model noChristianMovies = DBPediaService.loadStatements(movieQuery.toQueryString());

        germanNoChristianMovies = DBPediaService.getResourceNames(noChristianMovies, Locale.GERMAN);
        englishNoChristianMovies = DBPediaService.getResourceNames(noChristianMovies, Locale.ENGLISH);

        return this.createQuestion(this.moviesCategory, 40, "In diesen Filmen hat " + germanChristianName + " mitgespielt",
                englishChristianName + " starred in these movies", germanChristianMovies, englishChristianMovies, germanNoChristianMovies, englishNoChristianMovies);
    }

    public Question getHansZimmerMusicFilms() {
        if (!DBPediaService.isAvailable()) {
            return null;
        }

        List<String> germanHansMovies, englishHansMovies, germanNoHansMovies, englishNoHansMovies;

        Resource hansZimmer = DBPediaService.loadStatements(DBPedia.createResource("Hans_Zimmer"));

        String englishHansName = DBPediaService.getResourceName(hansZimmer, Locale.ENGLISH);
        String germanHansName = DBPediaService.getResourceName(hansZimmer, Locale.GERMAN);

        SelectQueryBuilder movieQuery = DBPediaService.createQueryBuilder()
                .setLimit(5)
                .addWhereClause(RDF.type, DBPediaOWL.Film)
                .addPredicateExistsClause(FOAF.name)
                .addWhereClause(DBPediaOWL.musicComposer, hansZimmer)
                .addFilterClause(RDFS.label, Locale.GERMAN)
                .addFilterClause(RDFS.label, Locale.ENGLISH);

        Model hansMovies = DBPediaService.loadStatements(movieQuery.toQueryString());

        germanHansMovies = DBPediaService.getResourceNames(hansMovies, Locale.GERMAN);
        englishHansMovies = DBPediaService.getResourceNames(hansMovies, Locale.ENGLISH);

        movieQuery.removeWhereClause(DBPediaOWL.musicComposer, hansZimmer).addMinusClause(DBPediaOWL.musicBy, hansZimmer).setOffset(90);

        Model noHansMovies = DBPediaService.loadStatements(movieQuery.toQueryString());

        germanNoHansMovies = DBPediaService.getResourceNames(noHansMovies, Locale.GERMAN);
        englishNoHansMovies = DBPediaService.getResourceNames(noHansMovies, Locale.ENGLISH);

        return this.createQuestion(this.moviesCategory, 20, germanHansName + " hat fuer diese Filme die Musik komponiert",
                englishHansName + " composed the music for these movies", germanHansMovies, englishHansMovies,
                germanNoHansMovies, englishNoHansMovies);
    }

    /**
     * Creates a new Question from the specified parameters
     *
     * @param category The category of the question
     * @param value The value of the question
     * @param textDE the German Question Text
     * @param textEN the English Question Text
     * @param correctAnswersDE The correct Answers in German
     * @param correctAnswersEN The correct Answers in English
     * @param wrongAnswersDE The wrong Answers in German
     * @param wrongAnswersEN The wrong Answers in English
     * @return A Question with the set Answers in German and English
     */
    private Question createQuestion(Category category, int value, String textDE, String textEN, List<String> correctAnswersDE, List<String> correctAnswersEN, List<String> wrongAnswersDE, List<String> wrongAnswersEN){
        Question ret = new Question();

        if ((textDE == null) || (textEN == null) || (correctAnswersDE == null) || (correctAnswersEN == null) ||
                (wrongAnswersDE == null) || (wrongAnswersEN == null)) {

            return null;
        }

        if ((!correctAnswersDE.isEmpty()) && (!correctAnswersEN.isEmpty()) && (correctAnswersDE.size() == correctAnswersEN.size())
                && (!wrongAnswersDE.isEmpty()) && (!wrongAnswersEN.isEmpty()) && (wrongAnswersDE.size() == wrongAnswersEN.size())
                && (!textDE.isEmpty()) && (!textEN.isEmpty())) {

            ret.setTextDE(textDE);
            ret.setTextEN(textEN);

            // add correct answers
            for(int i = 0; i < correctAnswersDE.size(); i++){
                Answer ans = new Answer();
                ans.setCorrectAnswer(true);
                ans.setQuestion(ret);
                ans.setTextDE(correctAnswersDE.get(i));
                ans.setTextEN(correctAnswersEN.get(i));

                ret.addRightAnswer(ans);
            }

            // add wrong answers
            for(int i = 0; i < wrongAnswersDE.size(); i++){
                Answer ans = new Answer();
                ans.setCorrectAnswer(false);
                ans.setQuestion(ret);
                ans.setTextDE(wrongAnswersDE.get(i));
                ans.setTextEN(wrongAnswersEN.get(i));

                ret.addWrongAnswer(ans);
            }
        }

        ret.setValue(value);

        ret.setCategory(category);

        List<Question> questions = category.getQuestions();
        questions.add(ret);
        category.setQuestions(questions);

        return ret;
    }
}