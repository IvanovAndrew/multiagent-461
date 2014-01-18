package ConferenceTask.Ontology;

import jade.content.Predicate;
import jade.util.leap.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 17.01.14
 * Time: 21:18
 * To change this template use File | Settings | File Templates.
 */
public class MessageContent implements Predicate
{
    public String message;
    public ArrayList reports;
    public int rating;

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String newMessage)
    {
        message = newMessage;
    }

    public void setReports(ArrayList newReports)
    {
        reports = newReports;
    }

    public ArrayList getReports()
    {
        return reports;
    }

    public int getRating()
    {
        return rating;
    }

    public void setRating(int newRating)
    {
        rating = newRating;
    }
}
