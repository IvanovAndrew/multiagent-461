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
            var shedule = new Shedule();
            var random = new Random();
            for (int reportId = 0; reportId < Generator.Reports; reportId++)
            {
                var report = new Report
                    {
                        Id = reportId,
                        Name = Generator.GenerateName(random),
                        SectionNumber = reportId/10,
                        NumberInSection = reportId%10
                    };
                shedule.Reports.Add(report);
            }
            return shedule;
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
            while (!(msg.Type == MessageType.Type.REJECTAGENT ||
                    msg.Type == MessageType.Type.ACCEPTAGENT))
            {
                _coalition.ReceiveMessage(msg);
                msg = _coalition.GetAnswer();
                agent.ReceiveMessage(msg);
                msg = agent.GetAnswer();
            }

            if (msg.Type == MessageType.Type.ACCEPTAGENT)
            {
                _coalition.Members.Add(agent);
            }
            else
            {
                _opposition.Members.Add(agent);
            }
        }

    }
}
