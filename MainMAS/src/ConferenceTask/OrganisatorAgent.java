package ConferenceTask;

import jade.core.*;
import jade.core.Runtime;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
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
public class OrganisatorAgent extends Agent
{
    private final String agentName = "agent_";
    private ArrayList<AgentController> agents = new ArrayList<AgentController> ();

    protected void setup()
    {
        /*try
        {
            // create the agent description of itself
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            // register the description with the DF
            DFService.register (this, dfd);
        }
        catch (FIPAException e)
        {
            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
        }
        System.out.println(getLocalName()+" REGISTERED WITH THE DF");
        System.out.println(getLocalName()+" STARTED");*/
        String fileName = "";
        Object[] args = getArguments();
        if (args != null && args.length > 0)
        {
            fileName = (String) args[0];
        }
        else
        {
            fileName = "input.txt";
        }

        int[][] matrix;
        //AgentContainer allListeners = (AgentContainer)getContainerController();
        jade.core.Runtime rt = Runtime.instance ();
        ProfileImpl p = new ProfileImpl (false);
        AgentContainer allListeners = rt.createAgentContainer (p);

        try
        {
            matrix = Parser.parse (new BufferedReader (new FileReader (fileName)));
            Random random = new Random ();
            int bossId = random.nextInt (Parser.listeners);

            for (int listener = 0; listener < Parser.listeners; listener++)
            {
                int[] ratings = new int [Schedule.reportsCount];
                for (int report = 0; report < Parser.reports; report++)
                {
                    ratings[report] = matrix[report][listener];
                }

                String name = ListenerAgent.prefixName + listener;
                Object[] array = {listener, bossId, Object.class.cast (ratings)};
                AgentController newAgent = allListeners.createNewAgent (name, "ConferenceTask.ListenerAgent", array);
                newAgent.start ();
                agents.add (newAgent);
            }

//            organise (allListeners);
        }
        catch (IOException e)
        {
            System.out.println ("Incorrect name of file");
        }
        catch (StaleProxyException e)
        {
            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (ControllerException e)
        {
            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /*private void organise (jade.wrapper.AgentContainer ac) throws IOException, ControllerException
    {
        Schedule schedule;
        schedule = createFirstSchedule();

        Random random = new Random ();
        int idAgent = random.nextInt (agents.size ());
        System.out.println ("BOSS IS " + idAgent);
        AgentController  boss = ac.getAgent (ListenerAgent.prefixName + idAgent);

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent (ListenerAgent.YOU_ARE_BOSS);
        msg.setContentObject (schedule);
        msg.addReceiver(new AID (boss.getName (), AID.ISGUID));

        send (msg);
        send (msg);
        *//*
        msg = new ACLMessage (ACLMessage.INFORM);
        msg.setContentObject (idAgent);
        msg.setContent (ListenerAgent.BOSS_NAME);

        for (int i = 0; i < agents.size (); i++)
        {
            if (i == idAgent) continue;
            msg.addReceiver (new AID (agentName + i, AID.ISLOCALNAME));
        }
        send (msg);*//*
    }*/
}