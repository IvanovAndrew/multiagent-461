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

    public int getId()
    {
        return id;
    }

    public void setId(int newId)
    {
        id = newId;
    }

    public int getSection()
    {
        return section;
    }

    public void setSection(int newSection)
    {
        section = newSection;
    }

    public int getPositionInSection()
    {
        return positionInSection;
    }

    public void setPositionInSection(int newPositionInSection)
    {
        positionInSection = newPositionInSection;
    }
}