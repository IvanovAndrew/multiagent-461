package ConferenceTask;

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
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 24.12.13
 * Time: 21:12
 * To change this template use File | Settings | File Templates.
 */
public class ListenerAgent extends Agent
{
    public static final String I_AM_NEW = "I am new";
    public static final String SCHEDULE = "Schedule";
    public static final String IT_IS_GOOD_SCHEDULE = "It is good schedule";
    public static final String ALTERNATIVE_SCHEDULE = "Alternative schedule";
    public static final String VOTING = "Voting";
    public static final String ACCEPT_AGENT = "Accept agent";
    public static final String REJECT_AGENT = "Reject";
    public static final String VOTE_YES = "YES";
    public static final String VOTE_NO = "NO";

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
        System.out.println("Hello, I'm listener " + getLocalName());

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

        if (! amBoss)
        {
            try
            {
                jade.wrapper.AgentContainer ac = getContainerController();
                AgentController agent = ac.getAgent(prefixName + bossId);
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(I_AM_NEW);
                msg.addReceiver(new AID(agent.getName(), AID.ISGUID));
                send(msg);
            }
            catch (ControllerException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            coalition.add(getAID());
            coalitionsSchedule = createFirstSchedule();
            System.out.println(getLocalName() + " IS BOSS");
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
                        if (nowVoting)
                        {
                            if (msg.getContent().equals(VOTE_YES) || msg.getContent().equals(VOTE_NO))
                            {
                                updateVotes(msg.getContent());
                            }
                            else if (msg.getContent().equals(VOTING))
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
                catch (IOException e1)
                {
                    e1.printStackTrace();
                }
                catch (UnreadableException e1)
                {
                    e1.printStackTrace();
                }
                block();
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
    private void updateVotes (String vote) throws UnreadableException
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
                rejectNewSchedule();
            }
            nowVoting = false;
        }
    }

    /**
     * Adds new agent in coalition and updates the schedule
     *
     * @throws UnreadableException
     */
    private void acceptNewSchedule () throws UnreadableException
    {
        ACLMessage oldMessage = queue.get(0);
        coalitionsSchedule = (Schedule) oldMessage.getContentObject();
        coalition.add(oldMessage.getSender());
    }

    /**
     * Sends to agent message about it is accepted
     */
    private void acceptAgent ()
    {
        ACLMessage oldMessage = queue.get(0);
        queue.remove(0);
        ACLMessage reply = oldMessage.createReply();
        reply.setContent(ACCEPT_AGENT);
        send(reply);
    }

    /**
     * Sends to agent message about it is rejected
     */
    private void rejectNewSchedule ()
    {
        ACLMessage oldMessage = queue.get(0);
        queue.remove(0);

        ACLMessage reply = oldMessage.createReply();
        reply.setContent(REJECT_AGENT);
        send(reply);
    }

    private void handleMessage () throws IOException, UnreadableException
    {
        while (!nowVoting && queue.size() > 0)
        {
            ACLMessage msg = queue.get(0);

            if (msg.getContent().equals(I_AM_NEW))
            {
                sendCurrentSchedule();
            }
            else if (msg.getContent().equals(SCHEDULE))
            {
                createReply();
            }
            else if (msg.getContent().equals(IT_IS_GOOD_SCHEDULE))
            {
                acceptAgent();
            }
            else if (msg.getContent().equals(ALTERNATIVE_SCHEDULE))
            {
                createVoting();
            }
            else if (msg.getContent().equals(ACCEPT_AGENT) || msg.getContent().equals(REJECT_AGENT))
            {
                queue.remove(0);
            }
        }
    }

    /**
     * sends to new agent current schedule
     *
     * @throws IOException
     */
    private void sendCurrentSchedule () throws IOException
    {
        ACLMessage msg = queue.get(0);
        queue.remove(0);
        ACLMessage reply = msg.createReply();

        reply.setContentObject(coalitionsSchedule.clone());
        reply.setContent(SCHEDULE);

        send(reply);
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
        Schedule schedule;
        schedule = (Schedule) msg.getContentObject();

        analiseSchedule(schedule);

        ACLMessage reply = msg.createReply();
        if (isGoodSchedule())
        {
            reply.setContent(IT_IS_GOOD_SCHEDULE);
        }
        else
        {
            schedule = getAlternativeSchedule(schedule);
            reply.setContentObject(schedule);
            reply.setContent(ALTERNATIVE_SCHEDULE);
        }
        send(reply);
    }

    /**
     * creates voting between agents from coalition
     * @throws UnreadableException
     * @throws IOException
     */
    private void createVoting () throws UnreadableException, IOException
    {
        ACLMessage msg = queue.get(0);
        ACLMessage voteMsg = new ACLMessage(ACLMessage.INFORM);
        Schedule alterSchedule = (Schedule) msg.getContentObject();
        voteMsg.setContentObject(alterSchedule);
        voteMsg.setContent(VOTING);

        analiseSchedule(alterSchedule);

        for (AID aid : coalition)
        {
            voteMsg.addReceiver(aid);
        }
        send(voteMsg);
        nowVoting = true;
    }

    /**
     * Agent from coalition votes yes or no
     */
    private void vote() throws UnreadableException
    {
        ACLMessage msg = queue.get(0);
        queue.remove(0);
        Schedule schedule;
        schedule = (Schedule) msg.getContentObject();

        analiseSchedule(schedule);

        ACLMessage reply = msg.createReply();
        if (isGoodSchedule())
            reply.setContent(VOTE_YES);
        else
            reply.setContent(VOTE_NO);

        send(reply);
    }

    /**
     * sorts array by descending order according ratings
     * @param reports
     * @return
     */
    private Report[] sortDyDescending (Report[] reports)
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
        for (Report report : schedule.reports)
        {
            analysedSchedule[report.positionInSection][report.section] = report.clone();
        }

        for (int i = 0; i < Schedule.reportsInSections; i++)
        {
            analysedSchedule[i] = sortDyDescending(analysedSchedule[i]);
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
