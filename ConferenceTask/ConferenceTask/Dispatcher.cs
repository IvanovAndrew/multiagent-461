using System;
using System.Collections.Generic;
using System.Linq;
using ConferenceTask.MAS;

namespace ConferenceTask
{
    public class Dispatcher
    {
        private readonly Dictionary<int, Agent> _agents = new Dictionary<int, Agent>();

        /// <summary>
        ///     row - reports
        ///     column - listeners
        /// </summary>
        private readonly int[,] _matrix;

        public Schedule BestSchedule;
        private int _bestRating;
        private Coalition _coalition = new Coalition();
        private List<Agent> _opposition = new List<Agent>();

        public Dispatcher(int[,] matrix)
        {
            _matrix = matrix;
            CreatesAgents();
            BestSchedule = InitialFillShedule();
        }

        private Schedule InitialFillShedule()
        {
            var shedule = new Schedule();
            var random = new Random();
            for (int reportId = 0; reportId < Generator.Reports; reportId++)
            {
                var report = new Report
                {
                    Id = reportId,
                    Name = Generator.GenerateName(random),
                    SectionNumber = reportId / Schedule.ReportsCountInSection,
                    PositionInSection = reportId % Schedule.ReportsCountInSection
                };
                shedule.Reports.Add(report);
            }
            return shedule;
        }

        /// <summary>
        ///     Creates agents for every listener
        /// </summary>
        private void CreatesAgents()
        {
            for (int i = 0; i < Generator.Listeners; i++)
            {
                var ratings = new int[Schedule.ReportsCount];
                for (int j = 0; j < Schedule.ReportsCount; j++)
                {
                    ratings[j] = _matrix[j, i];
                }
                var agent = new Agent(i, ratings);
                _agents.Add(i, agent);
            }
        }

        /// <summary>
        ///     Creates finally Schedule
        /// </summary>
        public Schedule CreateSchedule()
        {
            int count = 0;
            var oldOpposition = new List<Agent>();
            while (count < 5)
            {
                oldOpposition = StartNewIteration(oldOpposition);
                var schedule = new Schedule();
                schedule = InitialFillShedule();
                var random = new Random();
                int firstId = (oldOpposition.Count != 0) 
                        ? random.Next(oldOpposition.Count - 1)
                        : random.Next(_agents.Count - 1);

                FirstAgentInCoalition(_agents[firstId], schedule);

                foreach (var agent in _agents.Values)
                {
                    if (agent.Id == firstId) continue;

                    Negotiation(agent);
                }

                //all agents are in coalition or opposition
                if (NewIsBetter(_coalition.Schedule))
                {
                    count = 0;
                }
                else
                {
                    count++;
                }
                
            }

            return BestSchedule;
        }

        private List<Agent> StartNewIteration(List<Agent> oldOpposition)
        {
            _coalition = new Coalition();
            oldOpposition = new List<Agent>(_opposition);
            _opposition = new List<Agent>();
            return oldOpposition;
        }

        private void FirstAgentInCoalition(Agent agent, Schedule schedule)
        {
            agent.AnaliseShedule(schedule);

            if (agent.IsGoodShedule())
            {
                _coalition.Schedule = schedule;
            }
            else
            {
                _coalition.Schedule = agent.GetMyBestShedule(schedule);
            }

            _coalition.Members.Add(agent);
        }


        private bool NewIsBetter(Schedule schedule)
        {
            var newRating = _agents.Sum(agent => agent.Value.GetRating(schedule));

            if (newRating > _bestRating)
            {
                _bestRating = newRating;
                BestSchedule = schedule;
                return true;
            }
            return false;
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
                _opposition.Add(agent);
            }
        }
    }
}