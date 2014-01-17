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
public class SchedulePredicate implements Predicate
{
    public ArrayList reports;

    public void setReports(ArrayList newReports)
    {
        reports = newReports;
    }

    public ArrayList getReports()
    {
        return reports;
    }
}
