package ConferenceTask;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 24.12.13
 * Time: 23:10
 * To change this template use File | Settings | File Templates.
 */
public class Schedule implements Serializable {
    public static final int sections = Parser.sections;
    public static final int reportsCount = Parser.reports;
    public static final int reportsInSections = reportsCount / sections;

    public ArrayList<Report> reports;

    public Schedule()
    {
        reports = new ArrayList<Report>();
    }

    @Override
    public Schedule clone()
    {
        Schedule newSchedule = new Schedule ();
        newSchedule.reports = (ArrayList<Report>) reports.clone ();
        return newSchedule;
    }
}
