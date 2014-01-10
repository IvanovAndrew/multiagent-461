package ConferenceTask;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 02.01.14
 * Time: 22:13
 * To change this template use File | Settings | File Templates.
 */
public class Report implements Serializable
{
    public int id;
    public int section;
    public int positionInSection;

    @Override
    public Report clone()
    {
        Report newReport = new Report ();

        newReport.id = id;
        newReport.section = section;
        newReport.positionInSection = positionInSection;

        return newReport;
    }
}