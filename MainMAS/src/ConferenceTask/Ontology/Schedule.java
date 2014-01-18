package ConferenceTask.Ontology;

import ConferenceTask.Generator;
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
    public static final int sections = Generator.sections;
    public static final int reportsCount = Generator.reports;
    public static final int reportsInSections = reportsCount / sections;

    private ArrayList mReports;

    public Schedule()
    {
        mReports = new ArrayList();
    }

    public Schedule(MessageContent obj)
    {
        mReports = obj.getReports();
    }

    @Override
    public Schedule clone()
    {
        Schedule newSchedule = new Schedule ();
        newSchedule.mReports = (ArrayList) mReports.clone ();
        return newSchedule;
    }

    public void setReports(ArrayList newReports)
    {
        mReports = newReports;
    }

    public ArrayList getReports()
    {
        return mReports;
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

    public void add (Report report)
    {
        mReports.add(report);
    }

    public void remove (Report badReport)
    {
        mReports.remove(badReport);
    }
}
