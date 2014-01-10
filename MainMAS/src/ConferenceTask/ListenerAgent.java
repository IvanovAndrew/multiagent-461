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
    public static final String YOU_ARE_BOSS = "You are boss";
    public static final String BOSS_NAME = "Boss_name";
    public static final String I_AM_NEW = "I am new";
    public static final String SCHEDULE = "Schedule";
    public static final String ALTERNATIVE_SCHEDULE = "Alternative schedule";
    public static final String VOTING = "Voting";
    public static final String ACCEPTAGENT = "Accept agentT";
    public static final String REJECTAGENT = "Reject";
    public static final String VOTE_YES = "YES";
    public static final String VOTE_NO = "NO";
    public static final String HELLO = "HELLO";

    private final int minBound = Schedule.reportsCount / 2;
    private int[] ratings = new int[Schedule.reportsCount];
    private int minRatingThreshold;

    private Schedule coalitionsSchedule;
    private Report[][] analisedShedule = new Report[Schedule.reportsInSections][Schedule.sections];
    private ArrayList<AID> coalition = new ArrayList<AID> ();
    private double quorum = 0.67;
    private int voteYes = 0;
    private int votesNo = 0;
    private boolean nowVoting = false;
    private Queue<ACLMessage> queue = new PriorityQueue<ACLMessage> ();
    public static String prefixName = "agent_";

    /**
     * Setup the agent.  Registers with the DF, and adds a behaviour to
     * process incoming messages.
     */
    protected void setup ()
    {
        System.out.println ("Hello, I'm listener " + getLocalName ());

        Object[] args = getArguments ();
        int bossId = 0;
        boolean amBoss = false;
        if (args != null && args.length > 0)
        {
            bossId = (Integer) args[1];
            amBoss = (Integer) args[0] == bossId;
            ratings = (int[]) args[2];
        }

        minRatingThreshold = calculateMinThreshold ();

        if (!amBoss)
        {
            try
            {
                jade.wrapper.AgentContainer ac = getContainerController ();
                AgentController agent = ac.getAgent (prefixName + bossId);
                ACLMessage msg = new ACLMessage (ACLMessage.INFORM);
                msg.setContent (I_AM_NEW);
                msg.addReceiver (new AID(agent.getName (), AID.ISGUID));
                send (msg);
            }
            catch (ControllerException e)
            {
                e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        else
        {
            System.out.println (getLocalName () + " IS BOSS");
        }


        addBehaviour (new CyclicBehaviour (this)
        {
            public void action ()
            {
                ACLMessage msg = receive (MessageTemplate.MatchPerformative (ACLMessage.INFORM));
                /*if (queue.size() == 0)
                {
                    msg = receive (MessageTemplate.MatchPerformative (ACLMessage.INFORM));
                }
                else
                {
                    msg = queue.element();
                }*/

                try
                {
                    if (msg != null)
                    {
                        System.out.println (" – " + myAgent.getLocalName () + " received: " + msg.getContent ());
//                        if (null != msg.getContentObject ())
//                        {
//                            System.out.println (":::" + getLocalName () + " received: " + msg.getContentObject ());
//                        }
                        String content = msg.getContent ();

//                        if (HELLO.equals (content))
//                        {
//                            System.out.println ("I received hello message from" + msg.getSender ().getLocalName ());
//                        }
                        /*else if (YOU_ARE_BOSS.equalsIgnoreCase (content))
                        {
                            coalition.add (getAID ());

                            Schedule schedule;
                            //schedule = (Schedule) msg.getContentObject ();
                            analiseSchedule (schedule);

                            coalitionsSchedule = isGoodSchedule () ? schedule : getAlternativeShedule (schedule);
                        }*/
//                        else if (BOSS_NAME.equals (content))
//                        {
//                            AID boss;
//                            System.out.println ((Integer) msg.getContentObject ());
//                            int id = (Integer) msg.getContentObject ();
//
//                            ACLMessage message = new ACLMessage (ACLMessage.INFORM);
//                            message.setContent (I_AM_NEW);
//                            message.addReceiver (new AID (prefixName+id, AID.ISLOCALNAME));
//                            send (message);
//                        }
                        if (I_AM_NEW.equals (content))
                        {
                            // send coalition's schedule
                            ACLMessage reply = msg.createReply ();
                            if (coalitionsSchedule == null)
                            {
                                coalitionsSchedule = createFirstSchedule ();
                            }
                            reply.setContentObject (coalitionsSchedule.clone ());
                            reply.setContent (SCHEDULE);

                            send (reply);
                            System.out.println (getLocalName () + " sends " + coalitionsSchedule);
                        }
                        else if (SCHEDULE.equals (content))
                        {
                            Schedule schedule = new Schedule ();
                            schedule = (Schedule) msg.getContentObject ();
                            analiseSchedule (schedule);

                            if (isGoodSchedule ())
                            {
                                ACLMessage reply = msg.createReply ();
                                reply.setContent (ACCEPTAGENT);
                                send (reply);
                            }
                            else
                            {
                                Schedule alterSchedule;
                                alterSchedule = getAlternativeShedule (schedule);

                                ACLMessage reply = msg.createReply ();
                                reply.setContent (ALTERNATIVE_SCHEDULE);
                                reply.setContentObject (alterSchedule);
                            }
                        }
                        else if (ALTERNATIVE_SCHEDULE.equals (content))
                        {
                            if (queue.size () == 0)
                            {
                                ACLMessage voteMsg = new ACLMessage (ACLMessage.INFORM);
                                voteMsg.setContentObject (msg.getContentObject ());
                                voteMsg.setContent (VOTING);
                                for (AID aid : coalition)
                                {
                                    voteMsg.addReceiver (aid);
                                }
                                send (voteMsg);
                                queue.add (msg);
                                nowVoting = true;
                            }
                            else
                            {
                                queue.add (msg);
                            }
                        }
                        else if (VOTING.equals (content))
                        {
                            Schedule alterSchedule;
                            alterSchedule = (Schedule) msg.getContentObject ();
                            analiseSchedule (alterSchedule);

                            ACLMessage reply = msg.createReply ();
                            if (isGoodSchedule ())
                            {
                                reply.setContent (VOTE_YES);
                            }
                            else
                            {
                                reply.setContent (VOTE_NO);
                            }
                            send (reply);
                        }
                        else if (VOTE_YES.equals (content))
                        {
                            voteYes++;
                        }
                        else if (VOTE_NO.equals (content))
                        {
                            votesNo++;
                        }
                        else if (ACCEPTAGENT.equals (content))
                        {
                            coalition.add (msg.getSender ());
                        }
                        else if (REJECTAGENT.equals (content))
                        {
                            // say sorry
                        }
                        else
                        {
                            System.out.println ("Unrecognized message");
                        }
                    }
                    if (nowVoting && voteYes + votesNo == coalition.size ())
                    {
                        ACLMessage oldMsg;
                        oldMsg = queue.element();
                        ACLMessage reply = oldMsg.createReply();
                        if (voteYes >= coalition.size() * quorum)
                        {
                            reply.setContent (ACCEPTAGENT);
                        }
                        else
                        {
                            reply.setContent (REJECTAGENT);
                        }
                        send (reply);
                        voteYes = 0;
                        votesNo = 0;
                        nowVoting = false;
                    }
                }
                catch (IOException e1)
                {
                    e1.printStackTrace ();
                }
                catch (UnreadableException e1)
                {
                    e1.printStackTrace ();
                }

                block ();
            }
        });
    }

    /**
     * Calculates the minimal threshold
     * @return
     */
    private int calculateMinThreshold ()
    {
        int[] sortedArray;
        sortedArray = ratings.clone ();
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
     * sorts array by descinding order according ratings
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
                    Report temp = reports[i].clone ();
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
            analisedShedule[report.positionInSection][report.section] = report.clone ();
        }

        for (int i = 0; i < Schedule.reportsInSections; i++)
        {
            analisedShedule[i] = sortDyDescending (analisedShedule[i]);
        }
    }

    /**
     *  if agent likes current schedule then returns true
     */
    public boolean isGoodSchedule ()
    {
        for (int i = 0; i < Schedule.reportsInSections; i++)
        {
            if (isGoodTime (i)) continue;
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
        Report report = analisedShedule[time][0];
        return ratings[report.id] >= minRatingThreshold;
    }

    /**
     *  Creates alternative schedule
     * @param schedule
     * @return
     */
    public Schedule getAlternativeShedule (Schedule schedule)
    {
        Schedule newShedule;
        newShedule = schedule;
        for (int time = 0; time < Schedule.reportsInSections; time++)
        {
            if (isGoodTime (time)) continue;

            changeSchedule (time, newShedule);
        }
        return newShedule;
    }

    private int GetSecondTopIndex ()
    {
        int max = - 1;
        int index = - 1;
        for (int i = 0; i < Schedule.reportsInSections; i++)
        {
            Report report = analisedShedule[i][1];
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
        int index = GetSecondTopIndex ();

        Report badReport = analisedShedule[badTime][2];
        Report goodReport = analisedShedule[index][1];

        schedule.reports.remove (badReport);
        schedule.reports.remove (goodReport);

        // do magic
        int tempBadNumbInSect = badReport.positionInSection;
        int tempBadSecNumb = badReport.section;

        badReport.positionInSection = goodReport.positionInSection;
        badReport.section = goodReport.section;

        goodReport.positionInSection = tempBadNumbInSect;
        goodReport.section = tempBadSecNumb;
        // end do magic

        schedule.reports.add (badReport);
        schedule.reports.add (goodReport);

        // replace two reports in analised schedule
        analisedShedule[badTime][2] = goodReport;
        analisedShedule[index][1] = badReport;
    }

    private Schedule createFirstSchedule()
    {
        Schedule schedule = new Schedule ();
        for (int reportId = 0; reportId < schedule.reportsCount; reportId++)
        {
            Report report = new Report ();
            report.id = reportId;
            report.section = reportId % schedule.sections;
            report.positionInSection = reportId / schedule.sections;
            schedule.reports.add (report);
        }

        analiseSchedule (schedule);

        return isGoodSchedule ()? schedule : getAlternativeShedule (schedule);
    }
}
