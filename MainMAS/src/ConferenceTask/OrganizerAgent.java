package ConferenceTask;

import ConferenceTask.Ontology.*;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.*;
import jade.core.Runtime;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 02.01.14
 * Time: 22:37
 * To change this template use File | Settings | File Templates.
 */
public class OrganizerAgent extends Agent implements MessageType
{
    private ContentManager mManager = (ContentManager) getContentManager();
    private Codec mCodec = new SLCodec();
    private Ontology mOntology = ScheduleOntology.getInstance();

    private ArrayList<AgentController> mAgents = new ArrayList<AgentController>();

    private int mPolledAgentCount = 0;
    private int mCurrentRating = - 1;
    private int mMaxRating = - 1;
    private int mIterationNumber = 0;

    private Queue mLastBoss = new PriorityQueue();
    private int mUpperBound = 2 * Generator.listeners / 3;

    private ArrayList<Integer> mCoalitionSizes = new ArrayList<Integer>();

    protected void setup ()
    {
        mManager.registerLanguage(mCodec);
        mManager.registerOntology(mOntology);

        Object[] args = getArguments();
        String fileName = (args != null && args.length > 0) ? (String) args[0] : null;

        jade.core.Runtime rt = Runtime.instance();
        ProfileImpl p = new ProfileImpl(false);
        AgentContainer allListeners = rt.createAgentContainer(p);

        try
        {
            int[][] matrix;
            matrix = Generator.readMatrixFromFile(fileName);

            for (int listener = 0; listener < Generator.listeners; listener++)
            {
                int[] ratings = new int[Schedule.reportsCount];
                for (int report = 0; report < Generator.reports; report++)
                {
                    ratings[report] = matrix[report][listener];
                }

                String name = ListenerAgent.prefixName + listener;
                Object[] array = {listener, ratings};
                AgentController newAgent = allListeners.createNewAgent(name, "ConferenceTask.ListenerAgent", array);
                newAgent.start();
                mAgents.add(newAgent);
            }

            organise();
        }
        catch (IOException e)
        {
            System.out.println("Incorrect name of file");
        }
        catch (StaleProxyException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (ControllerException e)
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

        addBehaviour(new CyclicBehaviour(this)
        {
            public void action ()
            {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                try
                {
                    if (msg != null)
                    {
                        String content;
                        content = ((MessageContent) mManager.extractContent(msg)).getMessage();
                        if (content.equals(FINISH))
                        {
                            poll(msg);
                        }
                        else if (content.equals(RATING_OF_SCHEDULE))
                        {
                            updatePoll(msg);
                        }
                    }
                    else
                    {
                        block();
                    }
                }
                catch (Codec.CodecException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                catch (OntologyException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                catch (StaleProxyException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                catch (ControllerException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                catch (IOException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        });
    }

    private void organise () throws IOException, ControllerException, Codec.CodecException, OntologyException
    {
        clearAgentsMemory();

        int idAgent = getNewBoss();
        System.out.println("BOSS IS " + idAgent);
        AgentController boss = mAgents.get(idAgent);

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setLanguage(mCodec.getName());
        msg.setOntology(mOntology.getName());
        msg.addReceiver(new AID(boss.getName(), AID.ISGUID));

        MessageContent content = new MessageContent();
        content.setMessage(YOU_ARE_BOSS);
        mManager.fillContent(msg, content);
        send(msg);
    }

    private int getNewBoss ()
    {
        Random random = new Random();

        int idAgent = random.nextInt(mAgents.size());

        if (mLastBoss.contains(idAgent))
        {
            idAgent = random.nextInt(mAgents.size());
        }
        if (mLastBoss.size() > mUpperBound)
        {
            mLastBoss.element();
            mLastBoss.add(idAgent);
        }
        return idAgent;
    }

    private void clearAgentsMemory () throws Codec.CodecException, OntologyException, StaleProxyException
    {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setLanguage(mCodec.getName());
        msg.setOntology(mOntology.getName());

        for (AgentController agent : mAgents)
        {
            msg.addReceiver(new AID(agent.getName(), AID.ISGUID));
        }

        MessageContent msgContent = new MessageContent();
        msgContent.setMessage(NEW_ROUND);

        mManager.fillContent(msg, msgContent);
        send(msg);
    }

    private void poll (ACLMessage msg) throws OntologyException, Codec.CodecException, StaleProxyException
    {
        MessageContent content = (MessageContent) mManager.extractContent(msg);
        mCoalitionSizes.add(content.getCoalitionSize());
        System.out.println(getLocalName() + " tries poll");
        mCurrentRating = 0;
        ACLMessage pollMsg = new ACLMessage(ACLMessage.INFORM);
        pollMsg.setLanguage(mCodec.getName());
        pollMsg.setOntology(mOntology.getName());
        for (AgentController agent : mAgents)
        {
            pollMsg.addReceiver(new AID(agent.getName(), AID.ISGUID));
        }

        MessageContent msgContent = new MessageContent();
        msgContent.setMessage(SAY_RATING);
        jade.util.leap.ArrayList reports;
        reports = ((MessageContent) mManager.extractContent(msg)).getReports();
        msgContent.setReports(reports);

        mManager.fillContent(pollMsg, msgContent);
        send(pollMsg);
    }

    private void updatePoll (ACLMessage vote) throws OntologyException, Codec.CodecException, IOException, ControllerException
    {
        mCurrentRating += ((MessageContent) mManager.extractContent(vote)).getRating();
        mPolledAgentCount++;

        System.out.println(getLocalName() + " updates poll");
        System.out.println("polled: " + mPolledAgentCount);

        if (mPolledAgentCount == mAgents.size()) compareSchedules();
    }

    private void compareSchedules () throws OntologyException, Codec.CodecException, ControllerException, IOException
    {
        if (mMaxRating < mCurrentRating)
        {
            System.out.println(getLocalName() + ": new schedule is better!");
            mMaxRating = mCurrentRating;
            mIterationNumber = 0;
        }
        else
        {
            System.out.println(getLocalName() + " old schedule isn't worse! ");
            mIterationNumber++;
        }
        mPolledAgentCount = 0;
        mCurrentRating = 0;
        System.out.println(getLocalName() + " №iteration " + mIterationNumber);

        if (mIterationNumber < 5) organise();
        else announceResult();
    }

    private void announceResult()
    {
        System.out.println("Coalition sizes:");
        for (int size : mCoalitionSizes)
        {
            System.out.print(size + " ");
        }
        System.out.println("\nSum of happy: " + mMaxRating);
    }
}