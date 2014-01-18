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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 02.01.14
 * Time: 22:37
 * To change this template use File | Settings | File Templates.
 */
public class OrganisatorAgent extends Agent implements MessageType
{
    public static String organisatorName = "organisator";

    private ContentManager manager = (ContentManager) getContentManager();
    private Codec codec = new SLCodec();
    private Ontology ontology = ScheduleOntology.getInstance();

    private ArrayList<AgentController> agents = new ArrayList<AgentController>();

    int polledAgentCount = 0;
    int finalRating = - 1;
    int maxRating = - 1;
    int iterationNumber = 0;

    protected void setup ()
    {
        manager.registerLanguage(codec);
        manager.registerOntology(ontology);
        System.out.println(getLocalName() + " and AID " + getAID());
        Object[] args = getArguments();
        String fileName = (args != null && args.length > 0) ? (String) args[0] : "input.txt";

        int[][] matrix;

        jade.core.Runtime rt = Runtime.instance();
        ProfileImpl p = new ProfileImpl(false);
        AgentContainer allListeners = rt.createAgentContainer(p);

        try
        {
            matrix = Parser.parse(new BufferedReader(new FileReader(fileName)));
            //            Random random = new Random ();
            //            int bossId = random.nextInt (Parser.listeners);

            for (int listener = 0; listener < Parser.listeners; listener++)
            {
                int[] ratings = new int[Schedule.reportsCount];
                for (int report = 0; report < Parser.reports; report++)
                {
                    ratings[report] = matrix[report][listener];
                }

                String name = ListenerAgent.prefixName + listener;
                Object[] array = {listener, Object.class.cast(ratings)};
                AgentController newAgent = allListeners.createNewAgent(name, "ConferenceTask.ListenerAgent", array);
                newAgent.start();
                agents.add(newAgent);
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
                String content = null;
                try
                {
                    if (msg != null)
                    {
                        content = ((MessageContent) manager.extractContent(msg)).getMessage();
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

        Random random = new Random();
        int idAgent = random.nextInt(agents.size());
        System.out.println("BOSS IS " + idAgent);
        AgentController boss = agents.get(idAgent);

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setLanguage(codec.getName());
        msg.setOntology(ontology.getName());
        msg.addReceiver(new AID(boss.getName(), AID.ISGUID));

        MessageContent content = new MessageContent();
        content.setMessage(YOU_ARE_BOSS);
        manager.fillContent(msg, content);
        send(msg);
    }

    private void clearAgentsMemory () throws Codec.CodecException, OntologyException, StaleProxyException
    {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setLanguage(codec.getName());
        msg.setOntology(ontology.getName());

        for (AgentController agent : agents)
        {
            msg.addReceiver(new AID(agent.getName(), AID.ISGUID));
        }

        MessageContent msgContent = new MessageContent();
        msgContent.setMessage(NEW_ROUND);

        manager.fillContent(msg, msgContent);
        send(msg);
    }

    private void poll (ACLMessage msg) throws OntologyException, Codec.CodecException, StaleProxyException
    {
        finalRating = 0;
        ACLMessage pollMsg = new ACLMessage(ACLMessage.INFORM);

        for (AgentController agent : agents)
        {
            pollMsg.addReceiver(new AID(agent.getName(), AID.ISGUID));
        }

        MessageContent msgContent = new MessageContent();
        msgContent.setMessage(SAY_RATING);
        jade.util.leap.ArrayList reports;
        reports = ((MessageContent) manager.extractContent(msg)).getReports();
        msgContent.setReports(reports);

        manager.fillContent(pollMsg, msgContent);
        send(pollMsg);
    }

    private void updatePoll (ACLMessage vote) throws OntologyException, Codec.CodecException, IOException, ControllerException
    {
        finalRating += ((MessageContent) manager.extractContent(vote)).getRating();
        polledAgentCount++;
        if (polledAgentCount == agents.size())
        {
            compareSchedules();
        }
    }

    private void compareSchedules () throws OntologyException, Codec.CodecException, ControllerException, IOException
    {
        if (maxRating < finalRating)
        {
            maxRating = finalRating;
            iterationNumber = 0;
        }
        else
        {
            iterationNumber++;
        }
        polledAgentCount = 0;
        finalRating = 0;
        if (iterationNumber < 5)
        {
            organise();
        }
    }
}