package ConferenceTask.Ontology;

import jade.content.Concept;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 02.01.14
 * Time: 22:13
 * To change this template use File | Settings | File Templates.
 */
public class Report implements Concept
{
    private int mId;
    private int mSection;
    private int mPositionInSection;

    @Override
    public Report clone()
    {
        Report newReport = new Report ();

        newReport.mId = mId;
        newReport.mSection = mSection;
        newReport.mPositionInSection = mPositionInSection;

        return newReport;
    }

    public int getId()
    {
        return mId;
    }

    public void setId(int newId)
    {
        mId = newId;
    }

    public int getSection()
    {
        return mSection;
    }

    public void setSection(int newSection)
    {
        mSection = newSection;
    }

    public int getPositionInSection()
    {
        return mPositionInSection;
    }

    public void setPositionInSection(int newPositionInSection)
    {
        mPositionInSection = newPositionInSection;
    }

    @Override
    public boolean equals(Object obj)
    {
        System.out.println("equals");
        if (obj.getClass() == Report.class)
        {
            Report report = (Report) obj;
            return report.mId == mId;
        }
        return false;

    }
}