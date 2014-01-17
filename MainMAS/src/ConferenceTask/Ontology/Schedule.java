package ConferenceTask.Ontology;

import ConferenceTask.Parser;
import jade.content.ContentElement;

import jade.util.leap.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 24.12.13
 * Time: 23:10
 * To change this template use File | Settings | File Templates.
 */
public class Schedule implements ContentElement
{
    public static final int sections = Parser.sections;
    public static final int reportsCount = Parser.reports;
    public static final int reportsInSections = reportsCount / sections;

    public ArrayList reports;

    public Schedule()
    {
        reports = new ArrayList();
    }

    @Override
    public Schedule clone()
    {
        Schedule newSchedule = new Schedule ();
        newSchedule.reports = (ArrayList) reports.clone ();
        return newSchedule;
    }

    public void setReports(ArrayList newReports)
    {
        reports = newReports;
    }

    public ArrayList getReports()
    {
        return reports;
    }

    public int getSections()
    {
        return sections;
    }

    public int getReportsCount()
    {
        return reportsCount;
    }

    public int getReportsInSections()
    {
        return reportsInSections;
    }
}
