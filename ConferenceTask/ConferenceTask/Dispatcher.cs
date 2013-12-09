using System;
using System.Collections.Generic;
using System.Windows.Forms;
using ConferenceTask.MAS;
using Message = ConferenceTask.MAS.Message;

namespace ConferenceTask
{
    public class Dispatcher
    {
        /// <summary>
        /// row - reports
        /// column - listeners
        /// </summary>
        private readonly int[,] _matrix;

        private readonly Dictionary<int, Agent> _agents = new Dictionary<int, Agent>();
        
        private Coalition _coalition = new Coalition();
        private Coalition _opposition = new Coalition();

        public Dispatcher(int[,] matrix)
        {
            _matrix = matrix;
            CreatesAgents();
        }

        private Shedule InitialFillShedule()
        {
            //todo Alex, you must create transformation from matrix to List<Report>. #RightNow!
        }

        /// <summary>
        /// Creates agents for every listener
        /// </summary>
        private void CreatesAgents()
        {
            for (int i = 0; i < Generator.Listeners; i++)
            {
                var ratings = new int[Shedule.ReportsCount];
                for (int j = 0; j < Shedule.ReportsCount; j++)
                {
                    ratings[j] = _matrix[j, i];
                }
                var agent = new Agent(i, ratings);
                _agents.Add(i, agent);
            }
        }

        /// <summary>
        /// Creates finally shedule
        /// </summary>
        public void CreateShedule()
        {
            foreach (KeyValuePair<int, Agent> pair in _agents)
            {
                var agent = pair.Value;

                if (_coalition.Members.Count == 0)
                {
                    var shedule = InitialFillShedule();
                    agent.AnaliseShedule(shedule);
                    
                    if (agent.IsGoodShedule())
                        continue;
                    shedule = agent.GetShedule(shedule);
                    _coalition = new Coalition(shedule);
                    _coalition.Members.Add(agent);
                    continue;
                }


                Negotiation(agent);
            }
        }

        private void Negotiation(Agent agent)
        {
            var msg = new Message();
            
            msg.Type = MessageType.Type.NEWAGENT;
            while (!(msg.Type == MessageType.Type.NO ||
                    msg.Type == MessageType.Type.YES))
            {
                _coalition.ReceiveMessage(msg);
                msg = _coalition.GetAnswer();
                agent.ReceiveMessage(msg);
                msg = agent.GetAnswer();
            }
        }

    }
}
