using System;
using System.Collections.Generic;

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
        
        private List<Agent> _coalition = new List<Agent>();
        private List<Agent> _opposition = new List<Agent>();

        public Shedule _shedule = new Shedule();

        private const float Quorum = (float) 0.5;

        public Dispatcher(int[,] matrix)
        {
            _matrix = matrix;

            InitialFillShedule();
            CreatesAgents();
        }

        private int InitialFillShedule()
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
                agent.AnaliseShedule(_shedule);

                if (_coalition.Count == 0)
                {
                    if (agent.IsGoodShedule())
                        continue;
                    _shedule = agent.GetShedule(_shedule);
                    _coalition.Add(agent);
                }

                else
                {
                    if (agent.IsGoodShedule())
                    {
                        _coalition.Add(agent);
                    }
                    else
                    {
                        var newShedule = agent.GetShedule(_shedule);
                        if (Voting(newShedule))
                        {
                            _shedule = newShedule;
                            _coalition.Add(agent);
                        }
                        else
                        {
                            _opposition.Add(agent);
                        }
                    }
                }
            }
        }

        /// <summary>
        /// voting among the agents from population.
        /// if not less than half of the approved agents, it returns true
        /// </summary>
        /// <param name="newShedule"></param>
        /// <returns></returns>
        private bool Voting(Shedule newShedule)
        {
            int count = 0;
            foreach (Agent agent in _coalition)
            {
                agent.AnaliseShedule(newShedule);
                if (agent.IsGoodShedule())
                {
                    count++;
                }
            }

            return count >= _coalition.Count*Quorum;
        }
    }
}
