package ConferenceTask;

import ConferenceTask.Ontology.*;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 24.12.13
 * Time: 21:12
 * To change this template use File | Settings | File Templates.
 */
public class ListenerAgent extends Agent implements Message
{
    // We handle contents
    private ContentManager manager = (ContentManager) getContentManager();
    // This agent speaks the SL language
    private Codec codec = new SLCodec();

    private Ontology ontology = ScheduleOntology.getInstance();

    private final int minBound = Schedule.reportsCount / 2;
    private int[] ratings = new int[Schedule.reportsCount];
    private int minRatingThreshold;

    private Schedule coalitionsSchedule;
    private Report[][] analysedSchedule = new Report[Schedule.reportsInSections][Schedule.sections];
    private ArrayList<AID> coalition = new ArrayList<AID>();
    private double quorum = 0.67;
    private int voteYes = 0;
    private int votesNo = 0;
    private boolean nowVoting = false;
    private ArrayList<ACLMessage> queue = new ArrayList<ACLMessage>();
    public static String prefixName = "agent_";

    /**
     * Setup the agent.
     */
    protected void setup ()
    {
//        System.out.println("Hello, I'm listener " + getLocalName());
        manager.registerLanguage(codec);
        manager.registerOntology(ontology);

        Object[] args = getArguments();
        int bossId = 0;
        boolean amBoss = false;
        if (args != null && args.length > 0)
        {
            bossId = (Integer) args[1];
            amBoss = (Integer) args[0] == bossId;
            ratings = (int[]) args[2];
        }

        minRatingThreshold = calculateMinThreshold();

        if (!amBoss)
        {
            try
            {
                jade.wrapper.AgentContainer ac = getContainerController();
                AgentController agent = ac.getAgent(prefixName + bossId);
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID(agent.getName(), AID.ISGUID));
                msg.setLanguage(codec.getName());
                msg.setOntology(ontology.getName());
                manager.fillContent(msg, createMessage(I_AM_NEW));
                send(msg);
            }
            catch (ControllerException e)
            {
                e.printStackTrace();
            }
            catch (Codec.CodecException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            catch (OntologyException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        else
        {
            coalition.add(getAID());
            coalitionsSchedule = createFirstSchedule();
//            System.out.println(getLocalName() + " IS BOSS");
        }

        addBehaviour(new CyclicBehaviour(this)
        {
            public void action ()
            {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                try
                {
                    if (msg != null)
                    {
//                        System.out.println(getLocalName() + " received message " + msg.getContent() + " from " + msg.getSender());
                        SchedulePredicate content = (SchedulePredicate) manager.extractContent(msg);
                        if (nowVoting)
                        {
                            if (content.message.equals(VOTE_YES) || content.message.equals(VOTE_NO))
                            {
                                updateVotes(msg.getContent());
                            }
                            else if (content.message.equals(VOTING))
                            {
                                queue.add(msg);
                                vote();
                            }
                            else
                            {
                                queue.add(msg);
                            }
                        }
                        else
                        {
                            queue.add(msg);
                            handleMessage();
                        }
                    }
                }
                catch (Exception e1)
                {
                    e1.printStackTrace();
                }
                block();
            }
        });
    }

    private Schedule createFirstSchedule ()
    {
//        System.out.println("createFirstSchedule starts");
        Schedule schedule = new Schedule();
        for (int reportId = 0; reportId < schedule.reportsCount; reportId++)
        {
            Report report = new Report();
            report.id = reportId;
            report.section = reportId % schedule.sections;
            report.positionInSection = reportId / schedule.sections;
            schedule.reports.add(report);
        }
        analiseSchedule(schedule);

//        System.out.println("createFirstSchedule ends");
        return isGoodSchedule() ? schedule : getAlternativeSchedule(schedule);
    }

    /**
     * Calculates the minimal threshold
     *
     * @return
     */
    private int calculateMinThreshold ()
    {
        int[] sortedArray;
        sortedArray = ratings.clone();
        for (int i = 0; i < sortedArray.length; i++)
        {
            for (int j = i + 1; j < sortedArray.length; j++)
            {
                if (sortedArray[i] < sortedArray[j])
                {
                    int temp = sortedArray[i];
                    sortedArray[i] = sortedArray[j];
                    sortedArray[j] = temp;
                }
            }
        }

        return sortedArray[minBound];
    }

    /**
     * counts votes.
     * completes the poll, if all agents have voted.
     *
     * @param vote
     * @throws UnreadableException
     */
    private void updateVotes (String vote) throws UnreadableException, Codec.CodecException, OntologyException
    {
        if (vote.equals(VOTE_YES))
        {
            voteYes++;
        }
        else
        {
            votesNo++;
        }

        if (voteYes + votesNo == coalition.size())
        {
            if (voteYes >= quorum * coalition.size())
            {
                acceptNewSchedule();
                acceptAgent();
            }
            else
            {
                rejectAgent();
            }
            nowVoting = false;
        }
    }

    /**
     * Adds new agent in coalition and updates the schedule
     *
     * @throws UnreadableException
     */
    private void acceptNewSchedule () throws UnreadableException, Codec.CodecException, OntologyException
    {
        ACLMessage oldMessage = queue.get(0);
        coalitionsSchedule = new Schedule((SchedulePredicate) manager.extractContent(oldMessage));
        coalition.add(oldMessage.getSender());
    }

    /**
     * Sends to agent message about it is accepted
     */
    private void acceptAgent () throws Codec.CodecException, OntologyException
    {
        ACLMessage oldMessage = queue.get(0);
        queue.remove(0);
        ACLMessage reply = oldMessage.createReply();
        manager.fillContent(reply, createMessage(ACCEPT_AGENT));
        send(reply);
//        System.out.println(getLocalName() + " accepted new agent in coalition");
    }

    /**
     * Sends to agent message about it is rejected
     */
    private void rejectAgent () throws Codec.CodecException, OntologyException
    {
        ACLMessage oldMessage = queue.get(0);
        queue.remove(0);

        ACLMessage reply = oldMessage.createReply();
        manager.fillContent(reply, createMessage(REJECT_AGENT));
        send(reply);
//        System.out.println(getLocalName() + " rejected agent");
    }

    private void handleMessage () throws IOException, UnreadableException, Codec.CodecException, OntologyException
    {
//        System.out.println(getLocalName() + " handle Message");
        while (! nowVoting && queue.size() > 0)
        {
//            System.out.println(getLocalName() + " while-true");
            ACLMessage msg = queue.get(0);
            String content = ((SchedulePredicate) manager.extractContent(msg)).message;

            if (content.equals(I_AM_NEW))
            {
//                System.out.println(getLocalName() + " received message "+ content);
                sendCurrentSchedule();
            }
            else if (content.equals(SCHEDULE))
            {
//                System.out.println(getLocalName() + " received message "+ content);
                createReply();
            }
            else if (content.equals(IT_IS_GOOD_SCHEDULE))
            {
//                System.out.println(getLocalName() + " received message "+ content);
                acceptAgent();
            }
            else if (content.equals(ALTERNATIVE_SCHEDULE))
            {
//                System.out.println(getLocalName() + " received message "+ content);
                createVoting();
            }
            else if (content.equals(ACCEPT_AGENT) || content.equals(REJECT_AGENT))
            {
//                System.out.println(getLocalName() + " received message "+ content);
                queue.remove(0);
            }
            else
            {
                System.out.println("Unrecognized type " + content);
//                createReply();
            }
        }
//        System.out.println(getLocalName() + " While-false");
    }

    /**
     * sends to new agent current schedule
     * @throws IOException
     */
    private void sendCurrentSchedule () throws IOException, Codec.CodecException, OntologyException
    {
        ACLMessage msg = queue.get(0);
        queue.remove(0);
        ACLMessage reply = msg.createReply();

        reply.setLanguage(codec.getName());
        reply.setOntology(ontology.getName());

        manager.fillContent(reply, createMessage(SCHEDULE, coalitionsSchedule.reports));
        send(reply);
    }

    private SchedulePredicate createMessage(String type)
    {
        return createMessage(type, null);
    }

    private SchedulePredicate createMessage(String type, jade.util.leap.ArrayList reports)
    {
        SchedulePredicate schedulePredicate = new SchedulePredicate();
        schedulePredicate.setMessage(type);
        if (reports != null)
        {
            schedulePredicate.setReports(reports);
        }
        return schedulePredicate;
    }

    /**
     * agent analyzes the schedule and tells it like it or not
     * @throws UnreadableException
     * @throws IOException
     */
    private void createReply () throws UnreadableException, IOException
    {
        ACLMessage msg = queue.get(0);
        queue.remove(0);

        try
        {
            SchedulePredicate schedulePredicate = (SchedulePredicate) manager.extractContent(msg);
//            System.out.println(getLocalName() + " received schedule!");
            Schedule schedule = new Schedule(schedulePredicate);
            analiseSchedule(schedule);

            ACLMessage reply = msg.createReply();
            SchedulePredicate content;

            if (isGoodSchedule())
            {
                content = createMessage(IT_IS_GOOD_SCHEDULE);
//                System.out.println(getLocalName() + " says " + IT_IS_GOOD_SCHEDULE);
            }
            else
            {
                schedule = getAlternativeSchedule(schedule);
                content = createMessage(ALTERNATIVE_SCHEDULE, schedule.reports);
//                System.out.println(getLocalName() + " says " + ALTERNATIVE_SCHEDULE);
            }
            manager.fillContent(reply, content);
            send(reply);
        }
        catch (UngroundedException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (Codec.CodecException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (OntologyException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    /**
     * creates voting between agents from coalition
     *
     * @throws UnreadableException
     * @throws IOException
     * @todo
     */
    private void createVoting () throws UnreadableException, IOException, OntologyException, Codec.CodecException
    {
        ACLMessage msg = queue.get(0);
        ACLMessage voteMsg = new ACLMessage(ACLMessage.INFORM);
        SchedulePredicate pred = (SchedulePredicate) manager.extractContent(msg);

        SchedulePredicate content = createMessage(VOTING, pred.reports);

        for (AID aid : coalition)
        {
            voteMsg.addReceiver(aid);
        }
        manager.fillContent(voteMsg, content);
        send(voteMsg);
        nowVoting = true;
    }

    /**
     * Agent from coalition votes yes or no
     * @TODO  smth
     */
    private void vote () throws UnreadableException, OntologyException, Codec.CodecException
    {
        ACLMessage msg = queue.get(0);
        queue.remove(0);

        SchedulePredicate pred = (SchedulePredicate) manager.extractContent(msg);
        Schedule schedule = new Schedule(pred);

        analiseSchedule(schedule);

        ACLMessage reply = msg.createReply();
        SchedulePredicate content;
        if (isGoodSchedule())
        {
            content = createMessage(VOTE_YES);
        }
        else
        {
            content = createMessage(VOTE_NO);
        }

        manager.fillContent(reply, content);
        send(reply);
    }

    /**
     * sorts array by descending order according ratings
     *
     * @param reports
     * @return
     */
    private Report[] sortByDescending (Report[] reports)
    {
        for (int i = 0; i < reports.length; i++)
        {
            for (int j = i + 1; j < reports.length; j++)
            {
                if (ratings[reports[i].id] < ratings[reports[j].id])
                {
                    Report temp = reports[i].clone();
                    reports[i] = reports[j];
                    reports[j] = temp;
                }
            }
        }

        return reports;
    }

    /**
     * Creates schedule which reports are sorted by descending on each line
     */
    private void analiseSchedule (Schedule schedule)
    {
        for (int i = 0; i < schedule.reports.size(); i++)
        {
            Report report;
            report = (Report) schedule.reports.get(i);
            analysedSchedule[report.positionInSection][report.section] = report.clone();
        }

        for (int i = 0; i < Schedule.reportsInSections; i++)
        {
            analysedSchedule[i] = sortByDescending(analysedSchedule[i]);
        }
    }

    /**
     * if agent likes current schedule then returns true
     */
    public boolean isGoodSchedule ()
    {
        for (int i = 0; i < Schedule.reportsInSections; i++)
        {
            if (isGoodTime(i)) continue;
            return false;
        }
        return true;
    }

    /**
     * Checks if maximal rating in the time is greather than minimal bound
     *
     * @param time
     * @return
     */
    private boolean isGoodTime (int time)
    {
        Report report = analysedSchedule[time][0];
        return ratings[report.id] >= minRatingThreshold;
    }

    /**
     * Creates alternative schedule
     *
     * @param schedule
     * @return
     */
    public Schedule getAlternativeSchedule (Schedule schedule)
    {
        Schedule newSchedule;
        newSchedule = schedule;
        for (int time = 0; time < Schedule.reportsInSections; time++)
        {
            if (isGoodTime(time)) continue;

            changeSchedule(time, newSchedule);
        }
        return newSchedule;
    }

    private int GetSecondTopIndex ()
    {
        int max = - 1;
        int index = - 1;
        for (int i = 0; i < Schedule.reportsInSections; i++)
        {
            Report report = analysedSchedule[i][1];
            if (ratings[report.id] > max)
            {
                max = ratings[report.id];
                index = i;
            }
        }
        return index;
    }

    /**
     * Changed one report from "bad" time to "good" report
     *
     * @param badTime
     * @param schedule
     */
    private void changeSchedule (int badTime, Schedule schedule)
    {
        int index = GetSecondTopIndex();

        Report badReport = analysedSchedule[badTime][2];
        Report goodReport = analysedSchedule[index][1];

        schedule.reports.remove(badReport);
        schedule.reports.remove(goodReport);

        // do magic
        int tempBadNumbInSect = badReport.positionInSection;
        int tempBadSecNumb = badReport.section;

        badReport.positionInSection = goodReport.positionInSection;
        badReport.section = goodReport.section;

        goodReport.positionInSection = tempBadNumbInSect;
        goodReport.section = tempBadSecNumb;
        // end do magic

        schedule.reports.add(badReport);
        schedule.reports.add(goodReport);

        // replace two reports in analysed schedule
        analysedSchedule[badTime][2] = goodReport;
        analysedSchedule[index][1] = badReport;
    }
}
