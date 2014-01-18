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
public class ListenerAgent extends Agent implements MessageType
{
    private ContentManager manager = (ContentManager) getContentManager();
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
    private int myNumber;

    boolean amBoss = false;
    private int rejected;

    private AID organisatorAID = new AID();

    /**
     * Setup the agent.
     */
    protected void setup ()
    {
        System.out.println("Hello, I'm listener " + getLocalName());
        manager.registerLanguage(codec);
        manager.registerOntology(ontology);

        Object[] args = getArguments();

        if (args != null && args.length > 0)
        {
            myNumber = (Integer) args[0];
            ratings = (int[]) args[1];
            minRatingThreshold = calculateMinThreshold();
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
                        MessageContent content = (MessageContent) manager.extractContent(msg);
                        System.out.println(getLocalName() + " received " + content.message + " from " + msg.getSender().getLocalName());
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
                    else
                    {
                        block();
                    }
                }
                catch (Exception e1)
                {
                    e1.printStackTrace();
                }
            }
        });
    }

    private Schedule createFirstSchedule ()
    {
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
        coalitionsSchedule = new Schedule((MessageContent) manager.extractContent(oldMessage));
    }

    /**
     * Sends to agent message about it is accepted
     */
    private void acceptAgent () throws Codec.CodecException, OntologyException
    {
        ACLMessage oldMessage = queue.get(0);
        queue.remove(0);

        System.out.println(oldMessage.getSender() + " was accepted");
        coalition.add(oldMessage.getSender());
        ACLMessage reply = oldMessage.createReply();
        manager.fillContent(reply, createMessage(ACCEPT_AGENT));
        send(reply);
    }

    /**
     * Sends to agent message about it is rejected
     */
    private void rejectAgent () throws Codec.CodecException, OntologyException
    {
        ACLMessage oldMessage = queue.get(0);
        queue.remove(0);
        System.out.println(oldMessage.getSender() + " was rejected");

        ACLMessage reply = oldMessage.createReply();
        manager.fillContent(reply, createMessage(REJECT_AGENT));
        send(reply);
        rejected++;
    }

    private void handleMessage () throws IOException, UnreadableException, Codec.CodecException, OntologyException, ControllerException
    {
        while (! nowVoting && queue.size() > 0)
        {
//            System.out.println(getLocalName() + " while-true");
            ACLMessage msg = queue.get(0);
            String content = ((MessageContent) manager.extractContent(msg)).message;

            if (content.equals(NEW_ROUND))
            {
                amBoss = false;
                coalition = new ArrayList<AID>();
                rejected = 0;
                votesNo = 0;
                voteYes = 0;
                queue = new ArrayList<ACLMessage>();
                organisatorAID = new AID();
            }
            else if (content.equals(SAY_RATING))
            {
                sayRating();
            }
            else if (content.equals(YOU_ARE_BOSS))
            {
                becomeBoss();
            }
            else if (content.equals(I_AM_BOSS))
            {
                writeToBoss();
            }
            else if (content.equals(I_AM_NEW))
            {
                sendCurrentSchedule();
            }
            else if (content.equals(SCHEDULE))
            {
                thinkAboutSchedule();
            }
            else if (content.equals(IT_IS_GOOD_SCHEDULE))
            {
                acceptAgent();
            }
            else if (content.equals(ALTERNATIVE_SCHEDULE))
            {
                createVoting();
            }
            else if (content.equals(ACCEPT_AGENT) || content.equals(REJECT_AGENT))
            {
                queue.remove(0);
                System.out.println(getLocalName() + " " + content);
            }
            else
            {
                System.out.println("Unrecognized type " + content);
            }
        }
//        System.out.println(getLocalName() + " while-false");
//        if (amBoss) System.out.println("coalition size " + coalition.size());
        if (amBoss && coalition.size() + rejected == Parser.listeners)
        {
//            System.out.println("Finish");
            sayOrganisatorAboutFinish();
        }
    }

    private void becomeBoss () throws Codec.CodecException, OntologyException, ControllerException
    {
        organisatorAID = queue.get(0).getSender();
        queue.remove(0);
        amBoss = true;
        coalition.add(getAID());
        coalitionsSchedule = createFirstSchedule();

        jade.wrapper.AgentContainer ac = getContainerController();
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        for (int i = 0; i < Parser.listeners; i++)
        {
            if (i == myNumber) continue;
            AgentController agent = ac.getAgent(prefixName + i);
            msg.addReceiver(new AID(agent.getName(), AID.ISGUID));
        }
        msg.setLanguage(codec.getName());
        msg.setOntology(ontology.getName());
        manager.fillContent(msg, createMessage(I_AM_BOSS));

        send(msg);
    }

    private void writeToBoss () throws OntologyException, Codec.CodecException
    {
        ACLMessage msg = queue.get(0);
        queue.remove(0);
        //String content = ((MessageContent) manager.extractContent(msg)).message;

        ACLMessage reply = msg.createReply();
        manager.fillContent(reply, createMessage(I_AM_NEW));
        send(reply);
        System.out.println(getLocalName() + " wrote to boss");
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

    private MessageContent createMessage (String type)
    {
        return createMessage(type, null, -1);
    }

    private MessageContent createMessage(String type, jade.util.leap.ArrayList reports)
    {
        return createMessage(type, reports, -1);
    }

    private MessageContent createMessage(String type, int rating)
    {
        return createMessage(type, null, rating);
    }

    private MessageContent createMessage (String type, jade.util.leap.ArrayList reports, int rating)
    {
        MessageContent content = new MessageContent();
        content.setMessage(type);
        if (reports != null)
        {
            content.setReports(reports);
        }
        if (rating > -1)
        {
            content.setRating(rating);
        }
        return content;
    }

    /**
     * agent analyzes the schedule and tells it like it or not
     *
     * @throws UnreadableException
     * @throws IOException
     */
    private void thinkAboutSchedule () throws UnreadableException, IOException
    {
        System.out.println(getLocalName() + " thinks about schedule");
        ACLMessage msg = queue.get(0);
        queue.remove(0);

        try
        {
            MessageContent schedulePredicate = (MessageContent) manager.extractContent(msg);
            Schedule schedule = new Schedule(schedulePredicate);
            analiseSchedule(schedule);

            ACLMessage reply = msg.createReply();
            MessageContent content;

            if (isGoodSchedule())
            {
                content = createMessage(IT_IS_GOOD_SCHEDULE);
                System.out.println(getLocalName() + " thinks that " + IT_IS_GOOD_SCHEDULE);
            }
            else
            {
                System.out.println(getLocalName() + " creates alternative schedule");
                schedule = getAlternativeSchedule(schedule);
                content = createMessage(ALTERNATIVE_SCHEDULE, schedule.reports);
                System.out.println(getLocalName() + " " + ALTERNATIVE_SCHEDULE);
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

    private void sayOrganisatorAboutFinish () throws Codec.CodecException, OntologyException, ControllerException
    {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setLanguage(codec.getName());
        msg.setOntology(ontology.getName());

        msg.addReceiver(organisatorAID);
        manager.fillContent(msg, createMessage(FINISH, coalitionsSchedule.reports));
    }

    private void sayRating() throws OntologyException, Codec.CodecException
    {
        ACLMessage msg = queue.get(0);
        queue.remove(0);
        Schedule schedule = new Schedule((MessageContent)manager.extractContent(msg));
        analiseSchedule(schedule);
        int ratingOfSchedule = calcRatingOfSchedule();

        ACLMessage reply = msg.createReply();
        manager.fillContent(reply, createMessage(RATING_OF_SCHEDULE, ratingOfSchedule));
    }

    private int calcRatingOfSchedule ()
    {
        int sum = 0;
        for (int i = 0; i < Schedule.reportsInSections; i++)
        {
            sum += ratings[analysedSchedule[i][0].id];
        }
        return sum;
    }

    /**
     * creates voting between agents from coalition
     *
     * @throws UnreadableException
     * @throws IOException
     */
    private void createVoting () throws UnreadableException, IOException, OntologyException, Codec.CodecException
    {
        ACLMessage msg = queue.get(0);
        MessageContent pred = (MessageContent) manager.extractContent(msg);

        MessageContent content = createMessage(VOTING, pred.reports);
        ACLMessage voteMsg = new ACLMessage(ACLMessage.INFORM);

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
     */
    private void vote () throws UnreadableException, OntologyException, Codec.CodecException
    {
        ACLMessage msg = queue.get(0);
        queue.remove(0);

        MessageContent pred = (MessageContent) manager.extractContent(msg);
        Schedule schedule = new Schedule(pred);

        analiseSchedule(schedule);

        ACLMessage reply = msg.createReply();
        MessageContent content;
        content = isGoodSchedule() ? createMessage(VOTE_YES) : createMessage(VOTE_NO);

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
