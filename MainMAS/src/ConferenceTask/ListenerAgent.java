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
    private ContentManager mManager = (ContentManager) getContentManager();
    private Codec mCodec = new SLCodec();
    private Ontology mOntology = ScheduleOntology.getInstance();

    private final int mMinBound = Schedule.reportsCount / 2;
    private int[] mRatings = new int[Schedule.reportsCount];
    private int mMinRatingThreshold;

    private Schedule mCoalitionsSchedule;
    private Report[][] mAnalysedSchedule = new Report[Schedule.reportsInSections][Schedule.sections];
    private ArrayList<AID> mCoalition = new ArrayList<AID>();
    private double mQuorum = 0.67;
    private int mVoteYes = 0;
    private int mVotesNo = 0;
    private boolean mNowVoting = false;
    private boolean mIsFinish = false;
    private ArrayList<ACLMessage> mQueueMessages = new ArrayList<ACLMessage>();
    public static String prefixName = "agent_";
    private int mMyNumber;

    private boolean mAmBoss;
    private int mRejected;

    private AID mOrganizerAID = new AID();

    protected void setup ()
    {
        mManager.registerLanguage(mCodec);
        mManager.registerOntology(mOntology);

        Object[] args = getArguments();

        if (args != null && args.length > 0)
        {
            mMyNumber = (Integer) args[0];
            mRatings = (int[]) args[1];
            mMinRatingThreshold = calculateMinThreshold();
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
                        MessageContent content = (MessageContent) mManager.extractContent(msg);

                        if (content.getMessage().equals(VOTING))
                        {
                            vote(msg);
                        }

                        else if (mNowVoting)
                        {
                            if (content.getMessage().equals(VOTE_YES) || content.getMessage().equals(VOTE_NO))
                            {
                                updateVotes(content.getMessage());
                            }
                            else
                            {
                                mQueueMessages.add(msg);
                            }
                        }
                        else
                        {
                            if (mQueueMessages.size() == 0)
                            {
                                handleMessage(msg);
                            }
                            else
                            {
                                mQueueMessages.add(msg);
                            }
                        }
                    }
                    else
                    {
                        if (! mIsFinish && mAmBoss && mCoalition.size() + mRejected == Generator.listeners)
                        {
                            System.out.println(FINISH +": in coalition " + mCoalition.size() + " in opposition " + mRejected);
                            sayOrganizerAboutFinish();
                            mIsFinish = true;
                        }
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
            report.setId(reportId);
            report.setSection(reportId % schedule.sections);
            report.setPositionInSection(reportId / schedule.sections);
            schedule.add(report);
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
        sortedArray = mRatings.clone();
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

        return sortedArray[mMinBound];
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
            mVoteYes++;
        }
        else
        {
            mVotesNo++;
        }

        System.out.println("Yes " + mVoteYes + " No " + mVotesNo + " coalition: " + mCoalition.size());
        if (mVoteYes + mVotesNo == mCoalition.size())
        {
            summarizeVoting();
        }
    }

    private void summarizeVoting () throws Codec.CodecException, UnreadableException, OntologyException
    {
        ACLMessage msg = mQueueMessages.get(0);
        mQueueMessages.remove(0);

        if (mVoteYes >= mQuorum * mCoalition.size())
        {
            acceptNewSchedule(msg);
            acceptAgent(msg);
        }
        else
        {
            rejectAgent(msg);
        }
        mNowVoting = false;
        mVoteYes = 0;
        mVotesNo = 0;

        System.out.println("VOTING ENDS!");
        handleQueueMessages();
    }

    /**
     * Adds new agent in coalition and updates the schedule
     *
     * @throws UnreadableException
     */
    private void acceptNewSchedule (ACLMessage msg) throws UnreadableException, Codec.CodecException, OntologyException
    {
        mCoalitionsSchedule = new Schedule((MessageContent) mManager.extractContent(msg));
    }

    /**
     * Sends to agent message about it is accepted
     */
    private void acceptAgent (ACLMessage msg) throws Codec.CodecException, OntologyException
    {
        System.out.println(msg.getSender().getLocalName() + " was accepted");
        mCoalition.add(msg.getSender());
        ACLMessage reply = msg.createReply();
        mManager.fillContent(reply, createMessage(ACCEPT_AGENT));
        send(reply);
    }

    /**
     * Sends to agent message about it is rejected
     */
    private void rejectAgent (ACLMessage msg) throws Codec.CodecException, OntologyException
    {
        System.out.println(msg.getSender().getLocalName() + " was rejected");

        ACLMessage reply = msg.createReply();
        mManager.fillContent(reply, createMessage(REJECT_AGENT));
        send(reply);
        mRejected++;
    }

    private void handleMessage (ACLMessage msg) throws Exception
    {
        String content = ((MessageContent) mManager.extractContent(msg)).getMessage();

        if (content.equals(NEW_ROUND))
        {
            clear();
        }
        else if (content.equals(SAY_RATING))
        {
            //            System.out.println(getLocalName() + " received " + content + " from " + msg.getSender().getLocalName());
            sayRating(msg);
        }
        else if (content.equals(YOU_ARE_BOSS))
        {
            becomeBoss(msg);
        }
        else if (content.equals(I_AM_BOSS))
        {
            writeToBoss(msg);
        }
        else if (content.equals(I_AM_NEW))
        {
            sendCurrentSchedule(msg);
        }
        else if (content.equals(SCHEDULE))
        {
            thinkAboutSchedule(msg);
        }
        else if (content.equals(IT_IS_GOOD_SCHEDULE))
        {
            acceptAgent(msg);
        }
        else if (content.equals(ALTERNATIVE_SCHEDULE))
        {
            createVoting(msg);
        }
        else if (content.equals(ACCEPT_AGENT) || content.equals(REJECT_AGENT))
        {
            //            mQueueMessages.remove(0);
        }
        else
        {
            System.out.println(getLocalName() + " unrecognized type " + content + " now voting " + mNowVoting + " amBoss " + mAmBoss);
            System.out.println("from " + msg.getSender().getLocalName());
            throw new Exception("Cry");
        }


    }

    private void clear ()
    {
        mAmBoss = false;
        mCoalition = new ArrayList<AID>();
        mRejected = 0;
        mVotesNo = 0;
        mVoteYes = 0;
        mQueueMessages = new ArrayList<ACLMessage>();
        mOrganizerAID = new AID();
        mIsFinish = false;
    }

    private void handleQueueMessages () throws Codec.CodecException, OntologyException
    {
        while (mQueueMessages.size() > 0)
        {
            ACLMessage msg = mQueueMessages.get(0);
            mQueueMessages.remove(0);
            ACLMessage reply = msg.createReply();
            mManager.fillContent(reply, createMessage(SCHEDULE, mCoalitionsSchedule.getReports()));
            send(reply);
        }
    }

    private void becomeBoss (ACLMessage msg) throws Codec.CodecException, OntologyException, ControllerException
    {
        mOrganizerAID = msg.getSender();
        mAmBoss = true;
        mCoalition.add(getAID());
        mCoalitionsSchedule = createFirstSchedule();

        jade.wrapper.AgentContainer ac = getContainerController();
        ACLMessage firstBossMsg = new ACLMessage(ACLMessage.INFORM);
        for (int i = 0; i < Generator.listeners; i++)
        {
            if (i == mMyNumber) continue;
            AgentController agent = ac.getAgent(prefixName + i);
            firstBossMsg.addReceiver(new AID(agent.getName(), AID.ISGUID));
        }
        firstBossMsg.setLanguage(mCodec.getName());
        firstBossMsg.setOntology(mOntology.getName());
        mManager.fillContent(firstBossMsg, createMessage(I_AM_BOSS));

        send(firstBossMsg);
    }

    private void writeToBoss (ACLMessage msg) throws OntologyException, Codec.CodecException
    {
        ACLMessage reply = msg.createReply();
        mManager.fillContent(reply, createMessage(I_AM_NEW));
        send(reply);
    }

    /**
     * sends to new agent current schedule
     *
     * @throws IOException
     */
    private void sendCurrentSchedule (ACLMessage msg) throws IOException, Codec.CodecException, OntologyException
    {
        ACLMessage reply = msg.createReply();

        reply.setLanguage(mCodec.getName());
        reply.setOntology(mOntology.getName());

        mManager.fillContent(reply, createMessage(SCHEDULE, mCoalitionsSchedule.getReports()));
        send(reply);
    }

    private MessageContent createMessage (String type)
    {
        return createMessage(type, null, - 1);
    }

    private MessageContent createMessage (String type, jade.util.leap.ArrayList reports)
    {
        return createMessage(type, reports, - 1);
    }

    private MessageContent createMessage (String type, int rating)
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
        if (rating > - 1)
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
    private void thinkAboutSchedule (ACLMessage msg) throws UnreadableException, IOException
    {
        try
        {
            MessageContent schedulePredicate = (MessageContent) mManager.extractContent(msg);
            Schedule schedule = new Schedule(schedulePredicate);
            analiseSchedule(schedule);

            ACLMessage reply = msg.createReply();
            MessageContent content;

            if (isGoodSchedule())
            {
                content = createMessage(IT_IS_GOOD_SCHEDULE);
            }
            else
            {
                schedule = getAlternativeSchedule(schedule);
                content = createMessage(ALTERNATIVE_SCHEDULE, schedule.getReports());
            }
            mManager.fillContent(reply, content);
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

    private void sayOrganizerAboutFinish () throws Codec.CodecException, OntologyException, ControllerException
    {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setLanguage(mCodec.getName());
        msg.setOntology(mOntology.getName());

        msg.addReceiver(mOrganizerAID);
        mManager.fillContent(msg, createMessage(FINISH, mCoalitionsSchedule.getReports()));
        send(msg);
    }

    private void sayRating (ACLMessage msg) throws OntologyException, Codec.CodecException
    {
        Schedule schedule = new Schedule((MessageContent) mManager.extractContent(msg));
        analiseSchedule(schedule);
        int ratingOfSchedule = calcRatingOfSchedule();

        ACLMessage reply = msg.createReply();
        mManager.fillContent(reply, createMessage(RATING_OF_SCHEDULE, ratingOfSchedule));
        send(reply);
        System.out.println(getLocalName() + " sends rating " + ratingOfSchedule);
    }

    private int calcRatingOfSchedule ()
    {
        int sum = 0;
        for (int i = 0; i < Schedule.reportsInSections; i++)
        {
            sum += mRatings[mAnalysedSchedule[i][0].getId()];
        }
        return sum;
    }

    /**
     * creates voting between agents from coalition
     *
     * @throws UnreadableException
     * @throws IOException
     */
    private void createVoting (ACLMessage msg) throws UnreadableException, IOException, OntologyException, Codec.CodecException
    {
        mQueueMessages.add(msg);
        System.out.println("NEW VOTING! because of " + msg.getSender().getLocalName());

        ACLMessage voteMsg = new ACLMessage(ACLMessage.INFORM);
        voteMsg.setLanguage(mCodec.getName());
        voteMsg.setOntology(mOntology.getName());

        for (AID aid : mCoalition)
        {
            voteMsg.addReceiver(aid);
        }

        MessageContent pred = (MessageContent) mManager.extractContent(msg);
        MessageContent content = createMessage(VOTING, pred.getReports());

        mManager.fillContent(voteMsg, content);
        send(voteMsg);
        mNowVoting = true;
    }

    /**
     * Agent from coalition votes yes or no
     */
    private void vote (ACLMessage msg) throws UnreadableException, OntologyException, Codec.CodecException
    {
        MessageContent pred = (MessageContent) mManager.extractContent(msg);
        Schedule altSchedule = new Schedule(pred);

        analiseSchedule(altSchedule);

        ACLMessage reply = msg.createReply();
        MessageContent content;
        content = isGoodSchedule() ? createMessage(VOTE_YES) : createMessage(VOTE_NO);

        mManager.fillContent(reply, content);
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
                if (mRatings[reports[i].getId()] < mRatings[reports[j].getId()])
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
        for (int i = 0; i < schedule.getReports().size(); i++)
        {
            Report report;
            report = (Report) schedule.getReports().get(i);
            mAnalysedSchedule[report.getPositionInSection()][report.getSection()] = report.clone();
        }

        for (int i = 0; i < Schedule.reportsInSections; i++)
        {
            mAnalysedSchedule[i] = sortByDescending(mAnalysedSchedule[i]);
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
     * Checks if maximal rating in the time is greater than minimal bound
     *
     * @param time
     * @return
     */
    private boolean isGoodTime (int time)
    {
        Report report = mAnalysedSchedule[time][0];
        return mRatings[report.getId()] >= mMinRatingThreshold;
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
            Report report = mAnalysedSchedule[i][1];
            if (mRatings[report.getId()] > max)
            {
                max = mRatings[report.getId()];
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

        Report badReport = mAnalysedSchedule[badTime][2];
        Report goodReport = mAnalysedSchedule[index][1];

        schedule.remove(badReport);
        schedule.remove(goodReport);

        // do magic
        int tempBadNumbInSect = badReport.getPositionInSection();
        int tempBadSecNumb = badReport.getSection();

        badReport.setPositionInSection(goodReport.getPositionInSection());
        badReport.setSection(goodReport.getSection());

        goodReport.setPositionInSection(tempBadNumbInSect);
        goodReport.setSection(tempBadSecNumb);
        // end do magic

        schedule.add(badReport);
        schedule.add(goodReport);

        // replace two reports in analysed schedule
        mAnalysedSchedule[badTime][2] = goodReport;
        mAnalysedSchedule[index][1] = badReport;
    }
}