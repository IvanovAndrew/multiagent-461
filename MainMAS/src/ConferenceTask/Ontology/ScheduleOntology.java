package ConferenceTask.Ontology;

import jade.content.onto.*;
import jade.content.schema.*;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 10.01.14
 * Time: 20:04
 * To change this template use File | Settings | File Templates.
 */
public class ScheduleOntology extends Ontology
{
    // The name identifying this ontology
    public static final String ONTOLOGY_NAME = "Schedule-ontology";
    // VOCABULARY

    // schedule class
//    public static final String SCHEDULE = "Schedule";
//    public static final String SCHEDULE_SECTIONS = "sections";
//    public static final String SCHEDULE_REPORTSCOUNT = "reportsCount";
//    public static final String SCHEDULE_REPORTSINSECTIONS = "reportsInSections";
//    public static final String SCHEDULE_REPORTS = "reports";

    // Report class
    public static final String REPORT = "Report";
    public static final String REPORT_ID = "id";
    public static final String REPORT_SECTION = "section";
    public static final String REPORT_POSITION_IN_SECTION = "positionInSection";

    //schedulePredicate class
    public static final String SCHEDULE_PREDICATE = "Schedule predicate";
    public static final String REPORTS = "reports";

    // The singleton instance of this ontology
    private static Ontology theInstance = new ScheduleOntology(BasicOntology.getInstance());

    // This is the method to access the singleton music shop ontology object
    public static Ontology getInstance ()
    {
        return theInstance;
    }

    private ScheduleOntology (Ontology base)
    {
        super(ONTOLOGY_NAME, base, new ReflectiveIntrospector());
        try
        {
            PrimitiveSchema integerSchema = (PrimitiveSchema) getSchema(BasicOntology.INTEGER);

            AggregateSchema arrayReportsSchema = new AggregateSchema(BasicOntology.SEQUENCE);

            // Structure of the schema for the schedule concept
//            ConceptSchema scheduleSchema = new ConceptSchema(SCHEDULE);
//            scheduleSchema.add(SCHEDULE_SECTIONS, integerSchema, ObjectSchema.MANDATORY);
//            scheduleSchema.add(SCHEDULE_REPORTSCOUNT, integerSchema, ObjectSchema.MANDATORY);
//            scheduleSchema.add(SCHEDULE_REPORTSINSECTIONS, integerSchema, ObjectSchema.MANDATORY);
//            scheduleSchema.add(SCHEDULE_REPORTS, arrayReportsSchema, ObjectSchema.MANDATORY);

            // Structure of the schema for report concept
            ConceptSchema reportSchema = new ConceptSchema(REPORT);
            reportSchema.add(REPORT_ID, integerSchema, ObjectSchema.MANDATORY);
            reportSchema.add(REPORT_SECTION, integerSchema, ObjectSchema.MANDATORY);
            reportSchema.add(REPORT_POSITION_IN_SECTION, integerSchema, ObjectSchema.MANDATORY);

//            add(scheduleSchema, Schedule.class);
            add(reportSchema, Report.class);

            PredicateSchema schedulePredicate = new PredicateSchema(SCHEDULE_PREDICATE);
            schedulePredicate.add(REPORTS, arrayReportsSchema);

            add(schedulePredicate, SchedulePredicate.class);
        }
        catch (OntologyException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}