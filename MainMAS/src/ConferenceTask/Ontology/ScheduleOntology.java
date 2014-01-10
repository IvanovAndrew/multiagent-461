package ConferenceTask.Ontology;

import ConferenceTask.Report;
import ConferenceTask.Schedule;
import jade.content.onto.*;
import jade.content.schema.*;
/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 10.01.14
 * Time: 20:04
 * To change this template use File | Settings | File Templates.
 */
public class ScheduleOntology extends BeanOntology
{
    // The name identifying this ontology
    public static final String ONTOLOGY_NAME = "Schedule-ontology";
    // VOCABULARY

    // schedule class
    public static final String SCHEDULE = "Schedule";
    public static final String SCHEDULE_SECTIONS = "sections";
    public static final String SCHEDULE_REPORTSCOUNT = "reportsCount";
    public static final String SCHEDULE_REPORTSINSECTIONS = "reportsInSections";
    public static final String SCHEDULE_REPORTS = "reports";

    // Report class
    public static final String REPORT = "Report";
    public static final String REPORT_ID = "id";
    public static final String REPORT_SECTION = "section";
    public static final String REPORT_POSITIONINSECTION = "positionInSection";

    // The singleton instance of this ontology
    private static Ontology theInstance = new ScheduleOntology ();

    // This is the method to access the singleton music shop ontology object
    public static Ontology getInstance ()
    {
        return theInstance;
    }

    private ScheduleOntology ()
    {
        super(ONTOLOGY_NAME);
        try
        {
            add (new ConceptSchema (SCHEDULE), Schedule.class);
            add (new ConceptSchema (REPORT), Report.class);

            // Structure of the schema for the Item concept
            ConceptSchema cs = (ConceptSchema) getSchema(SCHEDULE);
            cs.add(SCHEDULE_SECTIONS, (PrimitiveSchema) getSchema(BasicOntology.INTEGER),ObjectSchema.MANDATORY);
            cs.add (SCHEDULE_REPORTSCOUNT, (PrimitiveSchema) getSchema (BasicOntology.INTEGER), ObjectSchema.MANDATORY);
            cs.add (SCHEDULE_REPORTSINSECTIONS, (PrimitiveSchema) getSchema (BasicOntology.INTEGER), ObjectSchema.MANDATORY);
            //cs.add (SCHEDULE_REPORTS, ObjectSchema.getSchema ());

        }
        catch (OntologyException e)
        {
            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
