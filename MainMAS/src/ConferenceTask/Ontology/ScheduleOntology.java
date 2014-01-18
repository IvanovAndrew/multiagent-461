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
    public static final String ONTOLOGY_NAME = "Schedule-ontology";

    public static final String REPORT = "Report";
    public static final String REPORT_ID = "id";
    public static final String REPORT_SECTION = "section";
    public static final String REPORT_POSITION_IN_SECTION = "positionInSection";

    public static final String SCHEDULE_PREDICATE = "Schedule predicate";
    public static final String MESSAGE = "message";
    public static final String REPORTS = "reports";
    public static final String RATING = "rating";

    private static Ontology sTheInstance = new ScheduleOntology(BasicOntology.getInstance());

    public static Ontology getInstance ()
    {
        return sTheInstance;
    }

    private ScheduleOntology (Ontology base)
    {
        super(ONTOLOGY_NAME, base, new ReflectiveIntrospector());
        try
        {
            PrimitiveSchema integerSchema = (PrimitiveSchema) getSchema(BasicOntology.INTEGER);
            PrimitiveSchema stringSchema = (PrimitiveSchema) getSchema(BasicOntology.STRING);
            AggregateSchema arrayReportsSchema = new AggregateSchema(BasicOntology.SEQUENCE);

            ConceptSchema reportSchema = new ConceptSchema(REPORT);
            reportSchema.add(REPORT_ID, integerSchema, ObjectSchema.MANDATORY);
            reportSchema.add(REPORT_SECTION, integerSchema, ObjectSchema.MANDATORY);
            reportSchema.add(REPORT_POSITION_IN_SECTION, integerSchema, ObjectSchema.MANDATORY);

            add(reportSchema, Report.class);

            PredicateSchema schedulePredicate = new PredicateSchema(SCHEDULE_PREDICATE);
            schedulePredicate.add(MESSAGE, stringSchema, ObjectSchema.MANDATORY);
            schedulePredicate.add(RATING, integerSchema, ObjectSchema.OPTIONAL);
            schedulePredicate.add(REPORTS, arrayReportsSchema, ObjectSchema.OPTIONAL);

            add(schedulePredicate, MessageContent.class);
        }
        catch (OntologyException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}