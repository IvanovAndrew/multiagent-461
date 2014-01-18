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
    private String mMessage;
    private ArrayList mReports;
    private int mRating;
    private int mCoalitionSize;

    public String getMessage()
    {
        return mMessage;
    }

    public void setMessage(String message)
    {
        mMessage = message;
    }

    public void setReports(ArrayList reports)
    {
        mReports = reports;
    }

    public ArrayList getReports()
    {
        return mReports;
    }

    public int getRating()
    {
        return mRating;
    }

    public void setRating(int rating)
    {
        mRating = rating;
    }

    public void setCoalitionSize(int coalitionSize)
    {
        mCoalitionSize = coalitionSize;
    }

    public int getCoalitionSize()
    {
        return mCoalitionSize;
    }
}
