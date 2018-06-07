import com.google.gson.Gson;
import utility.DbConnection;
import utility.TextualNormalization;
import utility.Utility;
import vsm.VSM;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.PropertyVetoException;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SearchServlet extends HttpServlet
{
    public void init() {
//        try {
//            initializeFiles();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        System.out.println("Ready!");
    }

    // todo: also comments
    private void initializeFiles() throws IOException {
        System.out.println("Initializing files...");

        PrintWriter writer = new PrintWriter("./data/cleaned_messages.csv", "UTF-8");

        try(BufferedReader br = new BufferedReader(new FileReader("./data/messages.csv"))) {
            for(String line; (line = br.readLine()) != null; ) {
                // split on the first comma, so we separate id from message
                String data[] = line.split(",", 2);
                if (data.length != 2 || data[1].length() < 2) {
                    continue;
                }

                long id = Long.parseLong(data[0]);
                String message = data[1];

                String normalized = TextualNormalization.normalizeText(message, false, false, true, false, false, false);
                writer.println(id + "," + normalized);
            }
        }

        System.out.println("Done!");
    }

    // todo: also comments
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        response.setContentType("text/json");
        PrintWriter out = response.getWriter();


        TextualNormalization textualNormalization = new TextualNormalization();
        String query = URLDecoder.decode(request.getParameter("query"), "UTF-8");
        double score = Double.parseDouble(URLDecoder.decode(request.getParameter("accuracy"), "UTF-8"));
        int max = Integer.parseInt(URLDecoder.decode(request.getParameter("max"), "UTF-8"));

        if (max <= 0 || score <= 0) {
            out.println("{error: \"Invalid parameters!\nPlease, review your settings.\"}");
            return;
        }

        if (query == null || query == "") {
            out.println("{error: \"Invalid query!\"}");
            return;
        }

        Map<String, Double> mapQuery = textualNormalization.performTextNormalization(query, false, false, true, false, false, false);
        HashMap<Long, Double> mapScore = new HashMap<>();

        File corpus = new File("../webapps/servlet/WEB-INF/data/corpus.csv");
        HashMap<Long, Double> results = new HashMap<>();
        ArrayList<Long> ids = new ArrayList<>();

        try(BufferedReader br = new BufferedReader(new FileReader(corpus))) {
            for(String line; (line = br.readLine()) != null; ) {
                // split on the first comma, so we separate id from message
                String data[] = line.split(",", 2);
                if (data.length != 2 || data[1].length() < 2) {
                    continue;
                }

                long id = Long.parseLong(data[0]);
                String message = data[1];

                Map<String, Double> mapCorpusDocument = Utility.convertStringToMap(message);
                double res = VSM.vsm(mapQuery, mapCorpusDocument);
                if (res > score)
                {
                    ids.add(id);
                    mapScore.put(id, res);
                }

            }
        }

        mapScore = updateScoreWithVotes(mapScore, query);

        ArrayList<DbRow> resultRows = new ArrayList<>();

        if (ids.size() <= 0) {
            out.println("{error:\"No results!\"}");
            return;
        }

        try {
            resultRows = getDataByIds(ids, mapScore);
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }

        if (resultRows.size() <= 0) {
            out.println("{error:\"No results!\"}");
            return;
        }

        // TODO: filter max num row?
        Collections.sort(resultRows, new Comparator<DbRow>() {
            @Override
            public int compare(DbRow row1, DbRow row2)
            {
                return row2.getScore().compareTo(row1.getScore());
            }
        });

        if (max > resultRows.size()) {
            max = resultRows.size();
        }

        resultRows = new ArrayList<DbRow>(resultRows.subList(0, max));

        out.println(getResultString(resultRows));
        out.close();

    }

    private HashMap<Long, Double> updateScoreWithVotes(HashMap<Long, Double> mapScore, String query) {

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {

            connection = DbConnection.getInstance().getConnection();
            statement = connection.createStatement();

            String q = "SELECT id, value FROM git_code.votes WHERE query LIKE \"" + query + "\";";
            resultSet = statement.executeQuery(q);

            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                int value = resultSet.getInt("value");
                if (mapScore.containsKey(id)) {
                    mapScore.put(id, mapScore.get(id) + 0.1 * value);
                }
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (resultSet != null)
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (statement != null)
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (connection != null)
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }


        return mapScore;
    }

    private String getResultString(ArrayList<DbRow> rows) {

        Gson gson = new Gson();
        String json = gson.toJson(rows);

        return json;
    }

    private ArrayList<DbRow> getDataByIds(ArrayList<Long> ids, HashMap<Long, Double> scores) throws IOException, PropertyVetoException {

        ArrayList<DbRow> retrievedRows = new ArrayList<>();

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {

            connection = DbConnection.getInstance().getConnection();
            statement = connection.createStatement();

            String stringIds = ids.toString();
            stringIds = stringIds.substring(1, stringIds.length()-1);

            String query = "SELECT * FROM git_code.data WHERE id IN (" + stringIds + ");";

            resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                DbRow temp = new DbRow(resultSet.getInt("id"));
                temp.setMessage(resultSet.getString("message"));
                temp.setCode(resultSet.getString("code"));
                temp.setComments(resultSet.getString("comments"));
                temp.setHash(resultSet.getString("hash"));
                temp.setUrl(resultSet.getString("url"));
                temp.setName(resultSet.getString("name"));
                temp.setScore(scores.get(temp.getId()));
                retrievedRows.add(temp);
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        } finally {
            if (resultSet != null)
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (statement != null)
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (connection != null)
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }

        return retrievedRows;
    }


    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        String id = URLDecoder.decode(request.getParameter("id"), "UTF-8");
        String action = URLDecoder.decode(request.getParameter("action"), "UTF-8");
        String query = URLDecoder.decode(request.getParameter("query"), "UTF-8");
        PrintWriter out = response.getWriter();

        if (id == "" || id == null) {
            out.println("{error:\"Invalid id!\"}");
            out.close();
            return;
        }

        if (action == "" || action == null) {
            out.println("{error:\"Invalid action!\"}");
            out.close();
            return;
        }

        if (query == "" || query == null) {
            out.println("{error:\"Invalid query!\"}");
            out.close();
            return;
        }

        Connection connection = null;
        Statement statement = null;
        int result;

        try {

            connection = DbConnection.getInstance().getConnection();
            statement = connection.createStatement();

            String q = "UPDATE git_code.votes SET value = value + " + action + " WHERE id=" + id + " AND query LIKE \"" + query + "\";";

            result = statement.executeUpdate(q);

            if (result == 0) {
                q = "INSERT INTO git_code.votes VALUES (" + id + ", \"" + query + "\", " + action + ");";
                result = statement.executeUpdate(q);
            }

            if (result < 0) {
                System.out.println("We have a problem: result < 0");
                System.out.println("Query: " + query);
            }


        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
            out.println("{error:\"" + sqlException.getMessage() +"\"}");
            out.close();
        } catch (PropertyVetoException e) {
            e.printStackTrace();
            out.println("{error:\"" + e.getMessage() + "\"}");
            out.close();
        } finally {
            if (statement != null)
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (connection != null)
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }

        out.println("{error:\"null\"}");
        out.close();
    }

    public String getServletInfo()
    {
        return "Name: Git Code\nAuthor: Riccardo Gabriele\nInfo: 2018 - Bachelor Thesis at USI Informatics";
    }
} 